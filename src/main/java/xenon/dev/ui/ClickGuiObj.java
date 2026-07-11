package xenon.dev.ui;

import dev.xenonlite.XenonLite;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import xenon.dev.modules.Module;
import xenon.dev.modules.Settings;
import xenon.dev.modules.mods.combat.LeftClicker;
import xenon.dev.modules.mods.combat.ThrowPot;

public final class ClickGuiObj extends GuiScreen {
    private static final int HEADER_HEIGHT = 24;
    private static final int ROW_HEIGHT = 28;
    private static final int MODULE_ROW_HEIGHT = 24;
    private static final int ACCENT = new Color(0x94, 0x31, 0xED).getRGB();
    private static final int ACCENT_SECONDARY = new Color(0xB7, 0x29, 0x41).getRGB();
    private static final int PANEL = new Color(24, 26, 29).getRGB();
    private static final int PANEL_LIGHT = new Color(32, 35, 39).getRGB();

    private final List<Module> modules = new ArrayList<Module>();
    private Module selectedModule;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int dragOffsetX;
    private int dragOffsetY;
    private int moduleScroll;
    private int settingsScroll;
    private boolean initialized;
    private boolean dragging;
    private boolean resizing;
    private boolean awaitingKeybind;
    private Settings draggedSlider;

    public ClickGuiObj() {
        if (XenonLite.instance != null) {
            this.modules.add(XenonLite.instance.getAimAssist());
            this.modules.add(XenonLite.instance.getLeftClicker());
            this.modules.add(XenonLite.instance.getRightClicker());
            this.modules.add(XenonLite.instance.getRefill());
            this.modules.add(XenonLite.instance.getThrowPot());
            this.modules.add(XenonLite.instance.getReach());
            this.modules.add(XenonLite.instance.getVelocity());
        }
        if (!this.modules.isEmpty()) {
            this.selectedModule = this.modules.get(0);
        }
    }

    @Override
    public void initGui() {
        int margin = responsiveMargin();
        int minimumWidth = minimumPanelWidth();
        int minimumHeight = minimumPanelHeight();
        if (!this.initialized) {
            this.panelWidth = clamp(Math.round(this.width * 0.72F), minimumWidth,
                    Math.min(720, this.width - margin * 2));
            this.panelHeight = clamp(Math.round(this.height * 0.76F), minimumHeight,
                    Math.min(460, this.height - margin * 2));
            this.panelX = (this.width - this.panelWidth) / 2;
            this.panelY = (this.height - this.panelHeight) / 2;
            this.initialized = true;
        } else {
            this.panelWidth = clamp(this.panelWidth, minimumWidth, this.width - margin * 2);
            this.panelHeight = clamp(this.panelHeight, minimumHeight, this.height - margin * 2);
            keepPanelOnScreen();
        }
        clampScrolls();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        updateDragOrResize(mouseX, mouseY);
        if (this.draggedSlider != null && Mouse.isButtonDown(0)) {
            updateSlider(this.draggedSlider, mouseX);
        }

        int sidebarWidth = sidebarWidth();
        drawRect(this.panelX, this.panelY, right(), bottom(), PANEL);
        drawRect(this.panelX, this.panelY, right(), this.panelY + HEADER_HEIGHT, PANEL_LIGHT);
        drawRect(this.panelX, this.panelY + HEADER_HEIGHT,
                this.panelX + sidebarWidth, bottom(), PANEL_LIGHT);
        int accentMiddle = this.panelX + this.panelWidth / 2;
        drawRect(this.panelX, this.panelY + HEADER_HEIGHT - 2,
                accentMiddle, this.panelY + HEADER_HEIGHT, ACCENT);
        drawRect(accentMiddle, this.panelY + HEADER_HEIGHT - 2,
                right(), this.panelY + HEADER_HEIGHT, ACCENT_SECONDARY);
        drawResizeHandle();

        String title = "Aura client - nobleesky x nahu";
        this.fontRendererObj.drawString(trimToWidth(title, this.panelWidth - 25),
                this.panelX + 10, this.panelY + 8, Color.WHITE.getRGB());
        this.fontRendererObj.drawString("Módulos", this.panelX + 10,
                this.panelY + 34, new Color(150, 155, 163).getRGB());

        drawModules(mouseX, mouseY);
        drawSettings(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void updateDragOrResize(int mouseX, int mouseY) {
        if (this.dragging) {
            this.panelX = clamp(mouseX - this.dragOffsetX, 0, this.width - this.panelWidth);
            this.panelY = clamp(mouseY - this.dragOffsetY, 0, this.height - this.panelHeight);
        }
        if (this.resizing) {
            int margin = responsiveMargin();
            this.panelWidth = clamp(mouseX - this.panelX, minimumPanelWidth(),
                    this.width - this.panelX - margin);
            this.panelHeight = clamp(mouseY - this.panelY, minimumPanelHeight(),
                    this.height - this.panelY - margin);
            clampScrolls();
        }
    }

    private void drawModules(int mouseX, int mouseY) {
        int sidebarWidth = sidebarWidth();
        int top = moduleViewportTop();
        int bottom = viewportBottom();
        beginClip(this.panelX, top, sidebarWidth, bottom - top);
        int y = top + 4 - this.moduleScroll;
        for (Module module : this.modules) {
            boolean visible = y + 20 >= top && y <= bottom;
            if (visible) {
                boolean selected = module == this.selectedModule;
                boolean hovered = inside(mouseX, mouseY, this.panelX + 6, y,
                        sidebarWidth - 12, 20) && inside(mouseX, mouseY, this.panelX, top,
                        sidebarWidth, bottom - top);
                if (selected || hovered) {
                    drawRect(this.panelX + 6, y, this.panelX + sidebarWidth - 6, y + 20,
                            selected ? new Color(54, 30, 70).getRGB() : new Color(49, 31, 43).getRGB());
                }
                drawRect(this.panelX + 14, y + 6, this.panelX + 20, y + 12,
                        module.isEnabled() ? ACCENT_SECONDARY : new Color(80, 84, 90).getRGB());
                this.fontRendererObj.drawString(trimToWidth(module.getName(), sidebarWidth - 38),
                        this.panelX + 27, y + 4, Color.WHITE.getRGB());
            }
            y += MODULE_ROW_HEIGHT;
        }
        endClip();
        drawScrollbar(this.panelX + sidebarWidth - 3, top, bottom - top,
                this.modules.size() * MODULE_ROW_HEIGHT, this.moduleScroll);
    }

    private void drawSettings(int mouseX, int mouseY) {
        if (this.selectedModule == null) {
            return;
        }
        int sidebarWidth = sidebarWidth();
        int x = this.panelX + sidebarWidth + contentPadding();
        int titleY = this.panelY + 38;
        this.fontRendererObj.drawString(trimToWidth(this.selectedModule.getName(), contentWidth()),
                x, titleY, Color.WHITE.getRGB());

        int top = settingsViewportTop();
        int bottom = viewportBottom();
        beginClip(this.panelX + sidebarWidth, top, this.panelWidth - sidebarWidth, bottom - top);
        int y = top + 5 - this.settingsScroll;
        boolean action = this.selectedModule instanceof ThrowPot;
        drawToggle(x, y, action ? "Throw potion" : "Enabled", !action && this.selectedModule.isEnabled());
        y += ROW_HEIGHT;

        if (action) {
            drawKeybind(x, y);
            y += ROW_HEIGHT;
        }
        for (Settings setting : this.selectedModule.getSettings()) {
            if (setting.isBooleanSetting()) {
                drawToggle(x, y, setting.getName(), setting.getValBoolean());
            } else {
                drawSlider(x, y, setting);
            }
            y += ROW_HEIGHT;
        }
        endClip();
        drawScrollbar(right() - 4, top, bottom - top, settingsContentHeight(), this.settingsScroll);
    }

    private void drawToggle(int x, int y, String label, boolean checked) {
        drawRect(x, y, x + 12, y + 12, new Color(75, 79, 86).getRGB());
        if (checked) {
            drawRect(x + 2, y + 2, x + 10, y + 10, ACCENT);
        }
        this.fontRendererObj.drawString(trimToWidth(label, Math.max(20, contentWidth() - 22)),
                x + 19, y + 2, Color.WHITE.getRGB());
    }

    private void drawKeybind(int x, int y) {
        String keyName = this.awaitingKeybind ? "Tecla..."
                : Keyboard.getKeyName(this.selectedModule.getKeybinding());
        int controlX = sliderX();
        int controlWidth = sliderWidth();
        this.fontRendererObj.drawString("Keybind", x, y + 2, Color.WHITE.getRGB());
        drawRect(controlX, y - 2, controlX + controlWidth, y + 14,
                new Color(48, 52, 58).getRGB());
        this.fontRendererObj.drawString(trimToWidth(keyName, controlWidth - 10),
                controlX + 5, y + 2, this.awaitingKeybind ? ACCENT_SECONDARY : Color.WHITE.getRGB());
    }

    private void drawSlider(int x, int y, Settings setting) {
        int sliderX = sliderX();
        int sliderWidth = sliderWidth();
        float range = setting.getMax() - setting.getMin();
        float ratio = range <= 0.0F ? 0.0F : (setting.getValfloat() - setting.getMin()) / range;
        this.fontRendererObj.drawString(trimToWidth(setting.getName(), Math.max(20, sliderX - x - 8)),
                x, y + 2, Color.WHITE.getRGB());
        drawRect(sliderX, y + 4, sliderX + sliderWidth, y + 10,
                new Color(67, 71, 78).getRGB());
        drawRect(sliderX, y + 4, sliderX + Math.round(sliderWidth * ratio), y + 10, ACCENT);
        String value = String.format("%.1f", setting.getValfloat());
        this.fontRendererObj.drawString(value,
                sliderX + sliderWidth - this.fontRendererObj.getStringWidth(value), y - 7,
                new Color(190, 194, 201).getRGB());
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int direction = wheel > 0 ? -MODULE_ROW_HEIGHT : MODULE_ROW_HEIGHT;
        int sidebarWidth = sidebarWidth();
        if (inside(mouseX, mouseY, this.panelX, moduleViewportTop(),
                sidebarWidth, viewportBottom() - moduleViewportTop())) {
            this.moduleScroll += direction;
        } else if (inside(mouseX, mouseY, this.panelX + sidebarWidth, settingsViewportTop(),
                this.panelWidth - sidebarWidth, viewportBottom() - settingsViewportTop())) {
            this.settingsScroll += direction;
        }
        clampScrolls();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }
        if (inside(mouseX, mouseY, right() - 16, bottom() - 16, 16, 16)) {
            this.resizing = true;
            return;
        }
        if (inside(mouseX, mouseY, this.panelX, this.panelY, this.panelWidth, HEADER_HEIGHT)) {
            this.dragging = true;
            this.dragOffsetX = mouseX - this.panelX;
            this.dragOffsetY = mouseY - this.panelY;
            return;
        }
        if (clickModule(mouseX, mouseY)) {
            return;
        }
        clickSetting(mouseX, mouseY);
    }

    private boolean clickModule(int mouseX, int mouseY) {
        int sidebarWidth = sidebarWidth();
        int top = moduleViewportTop();
        int bottom = viewportBottom();
        if (!inside(mouseX, mouseY, this.panelX, top, sidebarWidth, bottom - top)) {
            return false;
        }
        int y = top + 4 - this.moduleScroll;
        for (Module module : this.modules) {
            if (inside(mouseX, mouseY, this.panelX + 6, y, sidebarWidth - 12, 20)) {
                this.selectedModule = module;
                this.settingsScroll = 0;
                this.awaitingKeybind = false;
                this.draggedSlider = null;
                clampScrolls();
                return true;
            }
            y += MODULE_ROW_HEIGHT;
        }
        return false;
    }

    private void clickSetting(int mouseX, int mouseY) {
        if (this.selectedModule == null) {
            return;
        }
        int sidebarWidth = sidebarWidth();
        int top = settingsViewportTop();
        int bottom = viewportBottom();
        if (!inside(mouseX, mouseY, this.panelX + sidebarWidth, top,
                this.panelWidth - sidebarWidth, bottom - top)) {
            return;
        }
        int x = this.panelX + sidebarWidth + contentPadding();
        int y = top + 5 - this.settingsScroll;
        if (inside(mouseX, mouseY, x, y, contentWidth(), 14)) {
            this.selectedModule.toggle();
            return;
        }
        y += ROW_HEIGHT;
        if (this.selectedModule instanceof ThrowPot) {
            if (inside(mouseX, mouseY, sliderX(), y - 3, sliderWidth(), 18)) {
                this.awaitingKeybind = true;
                return;
            }
            y += ROW_HEIGHT;
        }
        for (Settings setting : this.selectedModule.getSettings()) {
            if (setting.isBooleanSetting() && inside(mouseX, mouseY, x, y, contentWidth(), 14)) {
                setting.setValBoolean(!setting.getValBoolean());
                return;
            }
            if (!setting.isBooleanSetting()
                    && inside(mouseX, mouseY, sliderX(), y, sliderWidth(), 15)) {
                this.draggedSlider = setting;
                updateSlider(setting, mouseX);
                return;
            }
            y += ROW_HEIGHT;
        }
    }

    private void updateSlider(Settings setting, int mouseX) {
        float ratio = Math.max(0.0F,
                Math.min(1.0F, (mouseX - sliderX()) / (float) sliderWidth()));
        setting.setValfloat(setting.getMin() + ratio * (setting.getMax() - setting.getMin()));
        if (this.selectedModule instanceof LeftClicker) {
            ((LeftClicker) this.selectedModule).update();
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

    private void drawResizeHandle() {
        drawRect(right() - 11, bottom() - 4, right() - 3, bottom() - 2, ACCENT_SECONDARY);
        drawRect(right() - 5, bottom() - 11, right() - 3, bottom() - 3, ACCENT_SECONDARY);
    }

    private void drawScrollbar(int x, int y, int viewport, int content, int scroll) {
        if (content <= viewport || viewport <= 0) {
            return;
        }
        int thumb = Math.max(14, Math.round(viewport * (viewport / (float) content)));
        int maxScroll = content - viewport;
        int offset = Math.round((viewport - thumb) * (scroll / (float) maxScroll));
        drawRect(x, y, x + 2, y + viewport, new Color(45, 48, 53).getRGB());
        drawRect(x, y + offset, x + 2, y + offset + thumb, ACCENT);
    }

    private void beginClip(int x, int y, int width, int height) {
        ScaledResolution resolution = new ScaledResolution(this.mc);
        int scale = resolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, this.mc.displayHeight - (y + height) * scale,
                Math.max(0, width * scale), Math.max(0, height * scale));
    }

    private static void endClip() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void clampScrolls() {
        this.moduleScroll = clamp(this.moduleScroll, 0,
                Math.max(0, this.modules.size() * MODULE_ROW_HEIGHT
                        - (viewportBottom() - moduleViewportTop())));
        this.settingsScroll = clamp(this.settingsScroll, 0,
                Math.max(0, settingsContentHeight()
                        - (viewportBottom() - settingsViewportTop())));
    }

    private int settingsContentHeight() {
        if (this.selectedModule == null) {
            return 0;
        }
        int rows = 1 + this.selectedModule.getSettings().size();
        if (this.selectedModule instanceof ThrowPot) {
            rows++;
        }
        return rows * ROW_HEIGHT + 10;
    }

    private void keepPanelOnScreen() {
        this.panelX = clamp(this.panelX, 0, Math.max(0, this.width - this.panelWidth));
        this.panelY = clamp(this.panelY, 0, Math.max(0, this.height - this.panelHeight));
    }

    private int responsiveMargin() { return Math.max(6, Math.min(18, Math.min(this.width, this.height) / 18)); }
    private int minimumPanelWidth() { return Math.max(100, Math.min(420, this.width - responsiveMargin() * 2)); }
    private int minimumPanelHeight() { return Math.max(100, Math.min(280, this.height - responsiveMargin() * 2)); }
    private int sidebarWidth() { return clamp(Math.round(this.panelWidth * 0.27F), 105, 155); }
    private int contentPadding() { return this.panelWidth < 390 ? 10 : 18; }
    private int contentWidth() { return Math.max(40, this.panelWidth - sidebarWidth() - contentPadding() * 2); }
    private int sliderX() { return this.panelX + sidebarWidth() + contentPadding() + Math.min(105, contentWidth() / 3); }
    private int sliderWidth() { return Math.max(55, right() - contentPadding() - sliderX()); }
    private int moduleViewportTop() { return this.panelY + 48; }
    private int settingsViewportTop() { return this.panelY + 58; }
    private int viewportBottom() { return bottom() - 16; }
    private int right() { return this.panelX + this.panelWidth; }
    private int bottom() { return this.panelY + this.panelHeight; }

    private String trimToWidth(String text, int maximumWidth) {
        return this.fontRendererObj.trimStringToWidth(text == null ? "" : text, Math.max(0, maximumWidth));
    }

    private static int clamp(int value, int minimum, int maximum) {
        if (maximum < minimum) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return width >= 0 && height >= 0 && mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }
}
