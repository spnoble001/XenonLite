package xenon.dev.modules.mods.combat;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
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
public final class PatchClicker extends Module {
    private final ClickerUtils rightUtils = new ClickerUtils();
    private final ClickerUtils leftUtils = new ClickerUtils();
    private long lastRightClick;
    private long lastRightDown;
    private long lastLeftClick;
    private long lastLeftDown;
    private double rightDelay = -1.0D;
    private double leftDelay = -1.0D;
    private boolean rightReleased = true;
    private boolean leftReleased = true;
    private boolean placement;

    public PatchClicker() {
        this.sl.add(new Settings("Right Speed".toCharArray(), this, 10.0F, 1.0F, 40.0F, false));
        this.sl.add(new Settings("Left Min".toCharArray(), this, 9.0F, 1.0F, 20.0F, false));
        this.sl.add(new Settings("Left Max".toCharArray(), this, 12.0F, 1.0F, 20.0F, false));
        this.sl.add(new Settings("Stop On Place".toCharArray(), this, true));
        registerSettings();
        this.name = "Patch Clicker";
    }

    @Override
    public void onDisable() {
        super.onDisable();
        releaseUseKey();
        releaseAttackKey();
        this.rightDelay = -1.0D;
        this.leftDelay = -1.0D;
        this.rightReleased = true;
        this.leftReleased = true;
        this.placement = false;
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
            releaseBothIfNecessary();
            return;
        }

        if (!isPatchBlockHeld() || !Mouse.isButtonDown(1)) {
            releaseBothIfNecessary();
            this.rightDelay = -1.0D;
            this.leftDelay = -1.0D;
            this.placement = false;
            return;
        }

        this.placement = isPlacingBlock();
        long now = System.currentTimeMillis();
        updateRightClicker(now);

        if (setting(3).getValBoolean() && this.placement) {
            releaseLeftIfNecessary();
            this.leftDelay = -1.0D;
        } else {
            updateLeftClicker(now);
        }
    }

    private void updateRightClicker(long now) {
        if (!this.rightReleased && now - this.lastRightDown > ThreadLocalRandom.current().nextInt(5)) {
            releaseUseKey();
            this.rightReleased = true;
        }
        if (this.minecraft.thePlayer.isUsingItem()) {
            return;
        }
        if (this.rightDelay < 0.0D || now - this.lastRightClick >= this.rightDelay) {
            int useKey = this.minecraft.gameSettings.keyBindUseItem.getKeyCode();
            KeyBinding.setKeyBindState(useKey, true);
            KeyBinding.onTick(useKey);
            this.rightUtils.sendFakeRight();
            this.lastRightClick = now;
            this.lastRightDown = now;
            this.rightReleased = false;
            this.rightDelay = this.rightUtils.randomization(setting(0).getValfloat());
        }
    }

    private void updateLeftClicker(long now) {
        if (!this.leftReleased && now - this.lastLeftDown > Math.max(1.0D, this.leftDelay / 2.0D)) {
            releaseAttackKey();
            this.leftReleased = true;
        }
        if (this.leftDelay < 0.0D || now - this.lastLeftClick >= this.leftDelay) {
            int attackKey = this.minecraft.gameSettings.keyBindAttack.getKeyCode();
            KeyBinding.setKeyBindState(attackKey, true);
            KeyBinding.onTick(attackKey);
            this.leftUtils.sendFakeLeft();
            this.lastLeftClick = now;
            this.lastLeftDown = now;
            this.leftReleased = false;
            this.leftDelay = this.leftUtils.randomization(randomLeftCps());
        }
    }

    private float randomLeftCps() {
        float minimum = setting(1).getValfloat();
        float maximum = setting(2).getValfloat();
        if (minimum >= maximum) {
            return minimum;
        }
        return (float) ThreadLocalRandom.current().nextDouble(minimum, maximum);
    }

    private boolean isPatchBlockHeld() {
        ItemStack held = this.minecraft.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) {
            return false;
        }
        net.minecraft.block.Block block = ((ItemBlock) held.getItem()).getBlock();
        return block == Blocks.obsidian || block == Blocks.bedrock;
    }

    private boolean isPlacingBlock() {
        return this.minecraft.objectMouseOver != null
                && this.minecraft.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    private void releaseBothIfNecessary() {
        releaseRightIfNecessary();
        releaseLeftIfNecessary();
    }

    private void releaseRightIfNecessary() {
        if (!this.rightReleased) {
            releaseUseKey();
            this.rightReleased = true;
        }
    }

    private void releaseLeftIfNecessary() {
        if (!this.leftReleased) {
            releaseAttackKey();
            this.leftReleased = true;
        }
    }

    private void releaseUseKey() {
        KeyBinding.setKeyBindState(this.minecraft.gameSettings.keyBindUseItem.getKeyCode(), false);
    }

    private void releaseAttackKey() {
        KeyBinding.setKeyBindState(this.minecraft.gameSettings.keyBindAttack.getKeyCode(), false);
    }

    private Settings setting(int index) {
        return this.sl.get(index);
    }

    public boolean isPlacement() {
        return this.placement;
    }

    @Override
    public char getSort() {
        return 'p';
    }
}
