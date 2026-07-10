package xenon.dev.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public final class AimUtils {
    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();
    private static final Pattern PATTERN = new Pattern();

    private AimUtils() {
    }

    public static float getAngleDifference(float first, float second) {
        return ((first - second) % 360.0F + 540.0F) % 360.0F - 180.0F;
    }

    public static XObject<float[]> getRequiredRotations(XObject<AxisAlignedBB> wrappedBox, boolean update) {
        if (wrappedBox == null || wrappedBox.get() == null || MINECRAFT.thePlayer == null) {
            return new XObject<float[]>(new float[] {0.0F, 0.0F});
        }

        AxisAlignedBB box = wrappedBox.get();
        double limitX = box.maxX - box.minX;
        double limitZ = box.maxZ - box.minZ;
        if (!PATTERN.init) {
            PATTERN.changeX = 0.0D;
            PATTERN.changeZ = 0.0D;
            PATTERN.init = true;
        }

        double midX = box.minX + limitX / 2.0D;
        double midZ = box.minZ + limitZ / 2.0D;
        Vec3 position = new Vec3(midX + PATTERN.changeX, box.maxY, midZ + PATTERN.changeZ);
        float[] required = getRotations(new XObject<Vec3>(position)).get();
        float deltaYaw = getAngleDifference(required[0], MINECRAFT.thePlayer.rotationYaw);
        float deltaPitch = getAngleDifference(required[1], MINECRAFT.thePlayer.rotationPitch);

        if (update && Math.abs(deltaYaw) < 3.0F) {
            PATTERN.changeX = PATTERN.clamp(
                    PATTERN.changeX + PATTERN.randomize(0), -limitX / 2.0D, limitX / 2.0D
            );
            PATTERN.changeZ = PATTERN.clamp(
                    PATTERN.changeZ + PATTERN.randomize(2), -limitZ / 2.0D, limitZ / 2.0D
            );
        }
        return new XObject<float[]>(new float[] {deltaYaw, deltaPitch});
    }

    public static XObject<float[]> getRotations(XObject<Vec3> wrappedVector) {
        if (wrappedVector == null || wrappedVector.get() == null || MINECRAFT.thePlayer == null) {
            return new XObject<float[]>(new float[] {0.0F, 0.0F});
        }

        Vec3 target = wrappedVector.get();
        Vec3 eyes = new Vec3(
                MINECRAFT.thePlayer.posX,
                MINECRAFT.thePlayer.getEntityBoundingBox().minY + MINECRAFT.thePlayer.getEyeHeight(),
                MINECRAFT.thePlayer.posZ
        );
        double differenceX = target.xCoord - eyes.xCoord;
        double differenceY = target.yCoord - eyes.yCoord;
        double differenceZ = target.zCoord - eyes.zCoord;
        float yaw = MathHelper.wrapAngleTo180_float(
                (float) Math.toDegrees(Math.atan2(differenceZ, differenceX)) - 90.0F
        );
        float pitch = MathHelper.wrapAngleTo180_float(
                (float) -Math.toDegrees(Math.atan2(
                        differenceY,
                        Math.sqrt(differenceX * differenceX + differenceZ * differenceZ)
                ))
        );
        return new XObject<float[]>(new float[] {yaw, pitch});
    }

    public static float getDifference(EntityPlayer player) {
        return Math.abs(getRequiredRotations(
                new XObject<AxisAlignedBB>(player.getEntityBoundingBox()), false
        ).get()[0]);
    }
}
