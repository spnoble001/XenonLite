package xenon.dev.modules.mods.combat;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;
import xenon.dev.Xenon;
import xenon.dev.events.XenonEvent;
import xenon.dev.modules.Mod;
import xenon.dev.modules.Module;
import xenon.dev.modules.Settings;
import xenon.dev.utils.AimUtils;
import xenon.dev.utils.MagicUtils;
import xenon.dev.utils.PerlinNoise;
import xenon.dev.utils.XObject;

@Mod(keybind = 0)
public final class AimAssist extends Module {
    private UUID lastEntity;
    private final PerlinNoise yawNoise = new PerlinNoise(UUID.randomUUID().getMostSignificantBits());
    private final PerlinNoise pitchNoise = new PerlinNoise(UUID.randomUUID().getLeastSignificantBits());
    private double previousYawNoise;
    private double previousPitchNoise;
    private boolean noiseInitialized;

    public AimAssist() {
        this.sl.add(new Settings("FOV".toCharArray(), this, MagicUtils.xor(70), MagicUtils.xor(10), 180.0F, true));
        this.sl.add(new Settings("Speed".toCharArray(), this, 1.0F, MagicUtils.xor(0) + 1.0F, MagicUtils.xor(20), false));
        this.sl.add(new Settings("Min".toCharArray(), this, 1.8F, 0.1F, 3.0F, false));
        this.sl.add(new Settings("Max".toCharArray(), this, 4.5F, 3.0F, 6.0F, false));
        this.sl.add(new Settings("Click".toCharArray(), this, true));
        this.sl.add(new Settings("Sword".toCharArray(), this, false));
        this.sl.add(new Settings("Axe".toCharArray(), this, false));
        this.sl.add(new Settings("Lock".toCharArray(), this, true));
        this.sl.add(new Settings("Noise".toCharArray(), this, 0.35F, 0.0F, 2.0F, false));
        this.sl.add(new Settings("Noise Speed".toCharArray(), this, 0.08F, 0.01F, 0.30F, false));
        registerSettings();
        this.name = "Aim Assist";
    }

    @Override
    public char getSort() {
        return 'a';
    }

    @XenonEvent
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (this.minecraft.theWorld == null
                || this.minecraft.thePlayer == null
                || !this.enabled
                || this.minecraft.currentScreen != null) {
            return;
        }
        updateAim(event.renderTickTime);
    }

    private void updateAim(float partialTick) {
        float fov = setting(0).getValfloat();
        float speed = setting(1).getValfloat();
        float minimumDistance = setting(2).getValfloat();
        float maximumDistance = setting(3).getValfloat();
        boolean click = setting(4).getValBoolean();
        boolean sword = setting(5).getValBoolean();
        boolean axe = setting(6).getValBoolean();
        boolean lock = setting(7).getValBoolean();
        float noiseStrength = setting(8).getValfloat();
        float noiseSpeed = setting(9).getValfloat();

        if (click && !Mouse.isButtonDown(0)) {
            this.noiseInitialized = false;
            return;
        }
        if (!heldItemAllowed(sword, axe)) {
            this.noiseInitialized = false;
            return;
        }

        EntityPlayer target = findTarget(fov, minimumDistance, maximumDistance, lock);
        if (target == null || target.equals(this.minecraft.pointedEntity)) {
            this.noiseInitialized = false;
            return;
        }

        boolean targetChanged = this.lastEntity == null || !this.lastEntity.equals(target.getUniqueID());
        this.lastEntity = target.getUniqueID();
        float[] requiredRotations = AimUtils.getRequiredRotations(
                new XObject<net.minecraft.util.AxisAlignedBB>(target.getEntityBoundingBox()), true
        ).get();
        float requiredYaw = requiredRotations[0];
        EntityPlayerSP player = this.minecraft.thePlayer;
        double[] result = randIt(
                new XObject<Float>(requiredYaw),
                new XObject<Float>(speed / 10.0F),
                new XObject<Float>(player.rotationYaw),
                new XObject<Float>(player.prevRotationYaw)
        ).get();
        player.prevRotationYaw = (float) result[0];
        player.rotationYaw = (float) result[1];
        applyContinuousNoise(player, partialTick, noiseStrength, noiseSpeed, targetChanged);
    }

    private void applyContinuousNoise(EntityPlayerSP player, float partialTick, float strength,
                                      float speed, boolean targetChanged) {
        double time = (player.ticksExisted + partialTick) * speed;
        double currentYawNoise = this.yawNoise.sample(time);
        double currentPitchNoise = this.pitchNoise.sample(time + 137.31D);

        if (!this.noiseInitialized || targetChanged) {
            this.previousYawNoise = currentYawNoise;
            this.previousPitchNoise = currentPitchNoise;
            this.noiseInitialized = true;
            return;
        }

        float yawDelta = (float) ((currentYawNoise - this.previousYawNoise) * strength);
        float pitchDelta = (float) ((currentPitchNoise - this.previousPitchNoise) * strength * 0.65F);
        player.rotationYaw += yawDelta;
        player.prevRotationYaw += yawDelta;
        player.rotationPitch = clampPitch(player.rotationPitch + pitchDelta);
        player.prevRotationPitch = clampPitch(player.prevRotationPitch + pitchDelta);
        this.previousYawNoise = currentYawNoise;
        this.previousPitchNoise = currentPitchNoise;
    }

    private static float clampPitch(float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.noiseInitialized = false;
        this.lastEntity = null;
    }

    private boolean heldItemAllowed(boolean sword, boolean axe) {
        if (!sword && !axe) {
            return true;
        }
        if (this.minecraft.thePlayer.getHeldItem() == null) {
            return false;
        }
        Item item = this.minecraft.thePlayer.getHeldItem().getItem();
        return (sword && item instanceof ItemSword) || (axe && item instanceof ItemAxe);
    }

    private EntityPlayer findTarget(float fov, float minimumDistance, float maximumDistance, boolean lock) {
        EntityPlayer target = null;
        float closestDistance = Float.MAX_VALUE;
        for (EntityPlayer entity : this.minecraft.theWorld.playerEntities) {
            float distance = this.minecraft.thePlayer.getDistanceToEntity(entity);
            if (Xenon.getInstance().friendsList.contains(entity.getUniqueID())
                    || AimUtils.getDifference(entity) > fov
                    || !this.minecraft.thePlayer.canEntityBeSeen(entity)
                    || distance < minimumDistance
                    || distance > maximumDistance) {
                continue;
            }
            if (lock && this.lastEntity != null && this.lastEntity.equals(entity.getUniqueID())) {
                return entity;
            }
            if (closestDistance < distance) {
                continue;
            }
            closestDistance = distance;
            target = entity;
        }
        return target;
    }

    private XObject<double[]> randIt(
            XObject<Float> difference,
            XObject<Float> speed,
            XObject<Float> yaw,
            XObject<Float> previousYaw
    ) {
        float sensitivityBase = this.minecraft.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float actualSensitivity = (float) (Math.pow(sensitivityBase, 3.0D) * 8.0D);
        if (Math.abs(difference.get()) < 7.0F) {
            difference.set(difference.get() * 2.0F);
        }
        difference.set(difference.get() * actualSensitivity
                * (speed.get() * 100.0F + (float) ThreadLocalRandom.current().nextDouble(1.0D, 2.0D))
                / (4 + ThreadLocalRandom.current().nextInt(1, 10)));
        float newYaw = yaw.get() + difference.get().intValue() * 0.0015F;
        float newPreviousYaw = previousYaw.get() + difference.get().intValue() * 0.0015F;
        return new XObject<double[]>(new double[] {newYaw, newPreviousYaw});
    }

    private Settings setting(int index) {
        return this.sl.get(index);
    }
}
