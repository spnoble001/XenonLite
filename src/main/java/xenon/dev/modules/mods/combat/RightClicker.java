package xenon.dev.modules.mods.combat;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;
import xenon.dev.events.XenonEvent;
import xenon.dev.modules.Mod;
import xenon.dev.modules.Module;
import xenon.dev.modules.Settings;
import xenon.dev.ui.ClickGuiObj;
import xenon.dev.utils.ClickerUtils;

@Mod(keybind = 0)
public final class RightClicker extends Module {
    private long lastClick;
    private long lastDown;
    private boolean released = true;
    private double delay = -1.0D;
    private final ClickerUtils utils;

    public RightClicker() {
        this.sl.add(new Settings("Speed".toCharArray(), this, 10.0F, 1.0F, 40.0F, false));
        this.sl.add(new Settings("Blocks".toCharArray(), this, true));
        registerSettings();
        this.name = "Right Clicker";
        this.utils = new ClickerUtils();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        releaseUseKey();
        this.delay = -1.0D;
        this.released = true;
    }

    @XenonEvent
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent event) {
        if (!(event instanceof RenderGameOverlayEvent.Post)
                || event.type != RenderGameOverlayEvent.ElementType.ALL
                || !this.enabled
                || this.minecraft.thePlayer == null
                || this.minecraft.theWorld == null
                || !this.minecraft.inGameHasFocus
                || this.minecraft.currentScreen instanceof ClickGuiObj) {
            return;
        }

        if (!heldItemAllowed()) {
            releaseIfNecessary();
            return;
        }

        long now = System.currentTimeMillis();
        if (!this.released && now - this.lastDown > ThreadLocalRandom.current().nextInt(5)) {
            releaseUseKey();
            this.released = true;
        }

        if (!Mouse.isButtonDown(1)) {
            releaseIfNecessary();
            this.delay = -1.0D;
            return;
        }

        if (this.minecraft.thePlayer.isUsingItem()) {
            return;
        }

        if (this.delay < 0.0D || now - this.lastClick >= this.delay) {
            click(now);
        }
    }

    private boolean heldItemAllowed() {
        if (!setting(1).getValBoolean()) {
            return true;
        }
        return this.minecraft.thePlayer.getHeldItem() != null
                && this.minecraft.thePlayer.getHeldItem().getItem() instanceof ItemBlock;
    }

    private void click(long now) {
        int useKey = this.minecraft.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(useKey, true);
        KeyBinding.onTick(useKey);
        this.utils.sendFakeRight();
        this.lastClick = now;
        this.lastDown = now;
        this.released = false;
        this.delay = this.utils.randomization(setting(0).getValfloat());
    }

    private void releaseIfNecessary() {
        if (!this.released) {
            releaseUseKey();
            this.released = true;
        }
    }

    private void releaseUseKey() {
        KeyBinding.setKeyBindState(this.minecraft.gameSettings.keyBindUseItem.getKeyCode(), false);
    }

    private Settings setting(int index) {
        return this.sl.get(index);
    }

    @Override
    public char getSort() {
        return 'r';
    }

    public long getLastClick() {
        return this.lastClick;
    }

    public long getLastDown() {
        return this.lastDown;
    }
}
