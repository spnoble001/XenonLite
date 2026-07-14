package xenon.dev.modules.mods.combat;

import dev.xenonlite.XenonLite;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import xenon.dev.events.XenonEvent;
import xenon.dev.modules.Mod;
import xenon.dev.modules.Module;
import xenon.dev.modules.Settings;
import xenon.dev.ui.ClickGuiObj;
import xenon.dev.utils.ClickerUtils;
import xenon.dev.utils.RightClickCoordinator;

@Mod(keybind = 0)
public final class PatchClickerV2 extends Module implements RightClickCoordinator.Owner {
    private static final int INPUT_PRIORITY = 10;
    private static final double EDGE_MARGIN = 0.05D;
    private static final Field RIGHT_CLICK_DELAY_TIMER = ReflectionHelper.findField(
            Minecraft.class, "rightClickDelayTimer", "field_71467_ac");
    private static final Field PRESS_TIME = ReflectionHelper.findField(
            KeyBinding.class, "pressTime", "field_151474_i");

    private final ClickerUtils clickerUtils = new ClickerUtils();
    private long lastClick;
    private long lastDown;
    private double delay = -1.0D;
    private boolean released = true;
    private boolean rightClickerActive;
    private long nextAutoPotAttempt;
    private final Random autoPotRandom = new Random();
    private float autoPotThreshold = getDynamicHealthThreshold();

    public PatchClickerV2() {
        this.sl.add(new Settings("Speed".toCharArray(), this, 10.0F, 1.0F, 40.0F, false));
        this.sl.add(new Settings("0 Delay".toCharArray(), this, false));
        this.sl.add(new Settings("Mode".toCharArray(), this, "Render", "Render", "Tick"));
        this.sl.add(new Settings("Hitbox N".toCharArray(), this, 1.0F, 1.0F, 3.0F, 1.0F));
        this.sl.add(new Settings("Modo Libre".toCharArray(), this, false));
        this.sl.add(new Settings("Auto Pot".toCharArray(), this, false));
        registerSettings();
        this.name = "Patch Clicker V2";
    }

    @Override
    public void onDisable() {
        super.onDisable();
        deactivateRightClicker();
    }

    @XenonEvent
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (this.enabled && event.phase == TickEvent.Phase.END && isMode("Render")) {
            runClickerLogic();
        }
    }

    @XenonEvent
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (this.enabled && event.phase == TickEvent.Phase.END && isMode("Tick")) {
            runClickerLogic();
        }
    }

    private void runClickerLogic() {
        if (this.minecraft.thePlayer == null
                || this.minecraft.theWorld == null
                || !this.minecraft.inGameHasFocus
                || this.minecraft.currentScreen instanceof ClickGuiObj) {
            deactivateRightClicker();
            return;
        }

        if (tryAutoPot()) {
            return;
        }

        if (!isObsidianPlaceable()) {
            deactivateRightClicker();
            return;
        }

        MovingObjectPosition hit = this.minecraft.objectMouseOver;
        if (!isValidObsidianSideHit(hit) || wouldCollideWithPlayer(hit)) {
            deactivateRightClicker();
            return;
        }

        if (!RightClickCoordinator.acquire(this, INPUT_PRIORITY)) {
            resetLocalClickerState();
            return;
        }

        this.rightClickerActive = true;
        updateRightClicker(System.currentTimeMillis());
    }

    private boolean tryAutoPot() {
        if (!setting(5).getValBoolean() || XenonLite.instance == null
                || XenonLite.instance.getThrowPot() == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < this.nextAutoPotAttempt
                || this.minecraft.thePlayer.getHealth() > this.autoPotThreshold) {
            return false;
        }
        ThrowPot throwPot = XenonLite.instance.getThrowPot();
        if (!throwPot.hasPotion()) {
            this.nextAutoPotAttempt = now + 250L;
            return false;
        }
        deactivateRightClicker();
        boolean thrown = throwPot.triggerPrioritaryThrow();
        this.nextAutoPotAttempt = now + (thrown ? 750L : 100L);
        if (thrown) {
            this.autoPotThreshold = getDynamicHealthThreshold();
        }
        return thrown;
    }

    public float getDynamicHealthThreshold() {
        double mean = 5.0D;
        double standardDeviation = 1.0D;
        double rawValue = mean + this.autoPotRandom.nextGaussian() * standardDeviation;
        return (float) Math.max(3.0D, Math.min(7.0D, rawValue));
    }

    private boolean isObsidianPlaceable() {
        ItemStack held = this.minecraft.thePlayer.getHeldItem();
        return held != null
                && held.stackSize > 0
                && held.getItem() instanceof ItemBlock
                && ((ItemBlock) held.getItem()).getBlock() == Blocks.obsidian;
    }

    private boolean isValidObsidianSideHit(MovingObjectPosition hit) {
        if (hit == null
                || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || hit.getBlockPos() == null
                || hit.sideHit == null
                || hit.hitVec == null) {
            return false;
        }

        BlockPos blockPos = hit.getBlockPos();
        if (this.minecraft.theWorld.getBlockState(blockPos).getBlock() != Blocks.obsidian) {
            return false;
        }

        EnumFacing.Axis axis = hit.sideHit.getAxis();
        if (!axis.isHorizontal()) {
            return setting(4).getValBoolean();
        }

        Vec3 impact = hit.hitVec;
        double baseY = blockPos.getY();
        double distanceFromTop = baseY + 1.0D - impact.yCoord;
        double distanceFromBottom = impact.yCoord - baseY;
        return distanceFromTop > EDGE_MARGIN && distanceFromBottom > EDGE_MARGIN;
    }

    private boolean wouldCollideWithPlayer(MovingObjectPosition hit) {
        BlockPos placementPos = hit.getBlockPos().offset(hit.sideHit);
        AxisAlignedBB placementBox = new AxisAlignedBB(
                placementPos.getX(),
                placementPos.getY(),
                placementPos.getZ(),
                placementPos.getX() + 1.0D,
                placementPos.getY() + 1.0D,
                placementPos.getZ() + 1.0D);
        double horizontalExpansion = 0.45D * Math.round(setting(3).getValfloat());
        AxisAlignedBB predictivePlayerBox = this.minecraft.thePlayer.getEntityBoundingBox()
                .expand(horizontalExpansion, 0.0D, horizontalExpansion);
        return predictivePlayerBox.intersectsWith(placementBox);
    }

    private void updateRightClicker(long now) {
        if (!this.released && now - this.lastDown > ThreadLocalRandom.current().nextInt(5)) {
            releaseUseKey();
            this.released = true;
        }
        if (this.minecraft.thePlayer.isUsingItem()) {
            return;
        }
        if (this.delay < 0.0D || now - this.lastClick >= this.delay) {
            if (setting(1).getValBoolean()) {
                setRightClickDelayTimer(0);
            }
            int useKey = this.minecraft.gameSettings.keyBindUseItem.getKeyCode();
            KeyBinding.setKeyBindState(useKey, true);
            KeyBinding.onTick(useKey);
            this.clickerUtils.sendFakeRight();
            this.lastClick = now;
            this.lastDown = now;
            this.released = false;
            this.delay = this.clickerUtils.randomization(setting(0).getValfloat());
        }
    }

    private void deactivateRightClicker() {
        relinquishRightClickControl();
        RightClickCoordinator.release(this);
    }

    private void relinquishRightClickControl() {
        boolean pendingPatchClick = !this.released;
        boolean wasActive = this.rightClickerActive || pendingPatchClick;
        if (pendingPatchClick) {
            releaseUseKey();
        }
        this.released = true;
        this.rightClickerActive = false;
        this.delay = -1.0D;
        if (pendingPatchClick && this.minecraft.gameSettings != null) {
            setPressTime(this.minecraft.gameSettings.keyBindUseItem, 0);
        }
        if (wasActive) {
            setRightClickDelayTimer(4);
        }
    }

    private void resetLocalClickerState() {
        this.released = true;
        this.rightClickerActive = false;
        this.delay = -1.0D;
    }

    @Override
    public void onRightClickControlLost() {
        relinquishRightClickControl();
    }

    private void releaseUseKey() {
        if (this.minecraft.gameSettings != null) {
            KeyBinding.setKeyBindState(this.minecraft.gameSettings.keyBindUseItem.getKeyCode(), false);
        }
    }

    private Settings setting(int index) {
        return this.sl.get(index);
    }

    private boolean isMode(String mode) {
        return setting(2).getValString().equalsIgnoreCase(mode);
    }

    private void setRightClickDelayTimer(int value) {
        setInt(RIGHT_CLICK_DELAY_TIMER, this.minecraft, value);
    }

    private static void setPressTime(KeyBinding keyBinding, int value) {
        setInt(PRESS_TIME, keyBinding, value);
    }

    private static void setInt(Field field, Object owner, int value) {
        try {
            field.setInt(owner, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not update Minecraft click state", exception);
        }
    }

    public boolean isRightClickerActive() {
        return this.rightClickerActive;
    }

    @Override
    public char getSort() {
        return 'p';
    }
}
