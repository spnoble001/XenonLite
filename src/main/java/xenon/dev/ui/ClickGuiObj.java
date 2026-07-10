package xenon.dev.ui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;
import xenon.dev.modules.Module;
import xenon.dev.modules.Settings;
import xenon.dev.modules.mods.combat.ThrowPot;
import dev.xenonlite.XenonLite;

public final class ClickGuiObj extends GuiScreen {
    private static final int MIN_WIDTH = 480;
    private static final int MIN_HEIGHT = 300;
    private static final int HEADER_HEIGHT = 24;
    private static final int SIDEBAR_WIDTH = 145;
    private static final int ACCENT = new Color(53, 145, 255).getRGB();
    private static final int PANEL = new Color(24, 26, 29).getRGB();
    private static final int PANEL_LIGHT = new Color(32, 35, 39).getRGB();

    private final List<Module> combatModules = new ArrayList<Module>();
    private Module selectedModule;
    private int panelX = 100;
    private int panelY = 100;
    private int panelWidth = 560;
    private int panelHeight = 380;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean dragging;
    private boolean resizing;
    private boolean awaitingKeybind;
    private Settings draggedSlider;

    public ClickGuiObj() {
        if (XenonLite.instance != null) {
            this.combatModules.add(XenonLite.instance.getAimAssist());
            this.combatModules.add(XenonLite.instance.getLeftClicker());
            this.combatModules.add(XenonLite.instance.getRightClicker());
            this.combatModules.add(XenonLite.instance.getRefill());
            this.combatModules.add(XenonLite.instance.getThrowPot());
        }
        if (!this.combatModules.isEmpty()) {
            this.selectedModule = this.combatModules.get(0);
        }
    }

    @Override
    public void initGui() {
        this.panelWidth = Math.max(Math.min(MIN_WIDTH, this.width - 20), Math.min(this.panelWidth, this.width - 20));
        this.panelHeight = Math.max(Math.min(MIN_HEIGHT, this.height - 20), Math.min(this.panelHeight, this.height - 20));
        this.panelX = Math.max(5, Math.min(this.panelX, this.width - this.panelWidth - 5));
        this.panelY = Math.max(5, Math.min(this.panelY, this.height - this.panelHeight - 5));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        if (this.dragging) {
            this.panelX = Math.max(0, Math.min(mouseX - this.dragOffsetX, this.width - this.panelWidth));
            this.panelY = Math.max(0, Math.min(mouseY - this.dragOffsetY, this.height - this.panelHeight));
        }
        if (this.resizing) {
            this.panelWidth = Math.max(Math.min(MIN_WIDTH, this.width - this.panelX),
                    Math.min(mouseX - this.panelX, this.width - this.panelX));
            this.panelHeight = Math.max(Math.min(MIN_HEIGHT, this.height - this.panelY),
                    Math.min(mouseY - this.panelY, this.height - this.panelY));
        }
        if (this.draggedSlider != null && Mouse.isButtonDown(0)) {
            updateSlider(this.draggedSlider, mouseX);
        }

        drawRect(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, PANEL);
        drawRect(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + HEADER_HEIGHT, PANEL_LIGHT);
        drawRect(this.panelX, this.panelY + HEADER_HEIGHT, this.panelX + SIDEBAR_WIDTH, this.panelY + this.panelHeight, PANEL_LIGHT);
        drawRect(this.panelX, this.panelY + HEADER_HEIGHT - 2, this.panelX + this.panelWidth, this.panelY + HEADER_HEIGHT, ACCENT);
        drawRect(this.panelX + this.panelWidth - 10, this.panelY + this.panelHeight - 3,
                this.panelX + this.panelWidth - 3, this.panelY + this.panelHeight - 1, ACCENT);
        drawRect(this.panelX + this.panelWidth - 3, this.panelY + this.panelHeight - 10,
                this.panelX + this.panelWidth - 1, this.panelY + this.panelHeight - 3, ACCENT);
        this.fontRendererObj.drawString("Xenon Lite - nobleesky", this.panelX + 10, this.panelY + 8, Color.WHITE.getRGB());
        this.fontRendererObj.drawString("Combate", this.panelX + 10, this.panelY + 34, new Color(150, 155, 163).getRGB());

        drawModules(mouseX, mouseY);
        drawSettings(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawModules(int mouseX, int mouseY) {
        int y = this.panelY + 52;
        for (Module module : this.combatModules) {
            boolean selected = module == this.selectedModule;
            boolean hovered = inside(mouseX, mouseY, this.panelX + 6, y - 4, SIDEBAR_WIDTH - 12, 20);
            if (selected || hovered) {
                drawRect(this.panelX + 6, y - 4, this.panelX + SIDEBAR_WIDTH - 6, y + 16,
                        selected ? new Color(42, 48, 56).getRGB() : new Color(37, 40, 45).getRGB());
            }
            drawRect(this.panelX + 14, y + 2, this.panelX + 20, y + 8,
                    module.isEnabled() ? ACCENT : new Color(80, 84, 90).getRGB());
            this.fontRendererObj.drawString(module.getName(), this.panelX + 27, y, Color.WHITE.getRGB());
            y += 24;
        }
    }

    private void drawSettings(int mouseX, int mouseY) {
        if (this.selectedModule == null) {
            return;
        }
        int x = this.panelX + SIDEBAR_WIDTH + 20;
        int y = this.panelY + 40;
        this.fontRendererObj.drawString(this.selectedModule.getName(), x, y, Color.WHITE.getRGB());
        y += 25;

        boolean action = this.selectedModule instanceof ThrowPot;
        drawToggle(x, y, action ? "Throw potion" : "Enabled", !action && this.selectedModule.isEnabled());
        y += 28;

        if (action) {
            String keyName = this.awaitingKeybind ? "Tecla..."
                    : Keyboard.getKeyName(this.selectedModule.getKeybinding());
            this.fontRendererObj.drawString("Keybind", x, y + 2, Color.WHITE.getRGB());
            drawRect(x + 105, y - 2, x + 220, y + 14, new Color(48, 52, 58).getRGB());
            this.fontRendererObj.drawString(keyName, x + 111, y + 2,
                    this.awaitingKeybind ? ACCENT : Color.WHITE.getRGB());
            y += 28;
        }

        for (Settings setting : this.selectedModule.getSettings()) {
            if (setting.isBooleanSetting()) {
                drawToggle(x, y, setting.getName(), setting.getValBoolean());
            } else {
                drawSlider(x, y, setting);
            }
            y += 28;
        }
    }

    private void drawToggle(int x, int y, String label, boolean checked) {
        drawRect(x, y, x + 12, y + 12, new Color(75, 79, 86).getRGB());
        if (checked) {
            drawRect(x + 2, y + 2, x + 10, y + 10, ACCENT);
        }
        this.fontRendererObj.drawString(label, x + 19, y + 2, Color.WHITE.getRGB());
    }

    private void drawSlider(int x, int y, Settings setting) {
        int sliderX = x + 105;
        int sliderWidth = 190;
        float range = setting.getMax() - setting.getMin();
        float ratio = range <= 0.0F ? 0.0F : (setting.getValfloat() - setting.getMin()) / range;
        this.fontRendererObj.drawString(setting.getName(), x, y + 2, Color.WHITE.getRGB());
        drawRect(sliderX, y + 4, sliderX + sliderWidth, y + 10, new Color(67, 71, 78).getRGB());
        drawRect(sliderX, y + 4, sliderX + Math.round(sliderWidth * ratio), y + 10, ACCENT);
        String value = String.format("%.1f", setting.getValfloat());
        this.fontRendererObj.drawString(value, sliderX + sliderWidth - this.fontRendererObj.getStringWidth(value), y - 7,
                new Color(190, 194, 201).getRGB());
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }
        if (inside(mouseX, mouseY, this.panelX + this.panelWidth - 14,
                this.panelY + this.panelHeight - 14, 14, 14)) {
            this.resizing = true;
            return;
        }
        if (inside(mouseX, mouseY, this.panelX, this.panelY, this.panelWidth, HEADER_HEIGHT)) {
            this.dragging = true;
            this.dragOffsetX = mouseX - this.panelX;
            this.dragOffsetY = mouseY - this.panelY;
            return;
        }

        int moduleY = this.panelY + 52;
        for (Module module : this.combatModules) {
            if (inside(mouseX, mouseY, this.panelX + 6, moduleY - 4, SIDEBAR_WIDTH - 12, 20)) {
                this.selectedModule = module;
                this.awaitingKeybind = false;
                return;
            }
            moduleY += 24;
        }
        clickSetting(mouseX, mouseY);
    }

    private void clickSetting(int mouseX, int mouseY) {
        if (this.selectedModule == null) {
            return;
        }
        int x = this.panelX + SIDEBAR_WIDTH + 20;
        int y = this.panelY + 65;
        if (inside(mouseX, mouseY, x, y, 150, 14)) {
            if (this.selectedModule instanceof ThrowPot) {
                this.selectedModule.toggle();
            } else {
                this.selectedModule.toggle();
            }
            return;
        }
        y += 28;
        if (this.selectedModule instanceof ThrowPot) {
            if (inside(mouseX, mouseY, x + 105, y - 3, 115, 18)) {
                this.awaitingKeybind = true;
                return;
            }
            y += 28;
        }
        for (Settings setting : this.selectedModule.getSettings()) {
            if (setting.isBooleanSetting() && inside(mouseX, mouseY, x, y, 220, 14)) {
                setting.setValBoolean(!setting.getValBoolean());
                return;
            }
            if (!setting.isBooleanSetting() && inside(mouseX, mouseY, x + 105, y, 190, 15)) {
                this.draggedSlider = setting;
                updateSlider(setting, mouseX);
                return;
            }
            y += 28;
        }
    }

    private void updateSlider(Settings setting, int mouseX) {
        int sliderX = this.panelX + SIDEBAR_WIDTH + 125;
        float ratio = Math.max(0.0F, Math.min(1.0F, (mouseX - sliderX) / 190.0F));
        setting.setValfloat(setting.getMin() + ratio * (setting.getMax() - setting.getMin()));
        if (this.selectedModule != null && "Left Clicker".equals(this.selectedModule.getName())) {
            ((xenon.dev.modules.mods.combat.LeftClicker) this.selectedModule).update();
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        this.dragging = false;
        this.resizing = false;
        this.draggedSlider = null;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.awaitingKeybind && this.selectedModule instanceof ThrowPot) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.awaitingKeybind = false;
                return;
            }
            if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK) {
                this.selectedModule.setKeybinding(Keyboard.KEY_NONE);
            } else if (keyCode != Keyboard.KEY_NONE) {
                this.selectedModule.setKeybinding(keyCode);
            }
            this.awaitingKeybind = false;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
