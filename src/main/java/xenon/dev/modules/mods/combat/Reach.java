package xenon.dev.modules.mods.combat;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Timer;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import xenon.dev.events.XenonEvent;
import xenon.dev.modules.Mod;
import xenon.dev.modules.Module;
import xenon.dev.modules.Settings;
import xenon.dev.utils.MagicUtils;
import xenon.dev.utils.XObject;

@Mod(keybind = 0)
public final class Reach extends Module {
    public float reachVal;

    public Reach() {
        this.sl.add(new Settings("Min".toCharArray(), this, 3.0F,
                MagicUtils.xor(3), MagicUtils.xor(6), false));
        this.sl.add(new Settings("Max".toCharArray(), this, 3.5F,
                MagicUtils.xor(3), MagicUtils.xor(6), false));
        this.sl.add(new Settings("Chance".toCharArray(), this, MagicUtils.xor(80),
                MagicUtils.xor(1), MagicUtils.xor(100), true));
        this.sl.add(new Settings("Walls".toCharArray(), this, false));
        this.sl.add(new Settings("Sprint".toCharArray(), this, false));
        registerSettings();
        this.name = "Reach";
    }

    @Override
    public char getSort() {
        return 'r';
    }

    @XenonEvent
    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (this.minecraft.thePlayer == null
                || this.minecraft.theWorld == null
                || !this.enabled
                || (setting(4).getValBoolean() && !this.minecraft.thePlayer.isSprinting())) {
            return;
        }
        updateReach();
    }

    public void updateReach() {
        float minimum = setting(MagicUtils.xor(0)).getValfloat();
        float maximum = setting(MagicUtils.xor(1)).getValfloat();
        float chance = setting(MagicUtils.xor(2)).getValfloat();

        if (!setting(3).getValBoolean() && this.minecraft.objectMouseOver != null) {
            BlockPos blockPos = this.minecraft.objectMouseOver.getBlockPos();
            if (blockPos != null && this.minecraft.theWorld.getBlockState(blockPos).getBlock() != Blocks.air) {
                return;
            }
        }
        if (ThreadLocalRandom.current().nextInt(0, MagicUtils.xor(101)) > chance) {
            return;
        }
        this.reachVal = minimum < maximum
                ? BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(minimum, maximum)).floatValue()
                : maximum;
        runRaycast(new XObject<Float>(this.reachVal));
    }

    private void runRaycast(XObject<Float> range) {
        Minecraft minecraft = this.minecraft;
        Timer timer = ReflectionHelper.getPrivateValue(
                Minecraft.class, minecraft, "timer", "field_71428_T"
        );
        if (timer == null || minecraft.getRenderViewEntity() == null || minecraft.theWorld == null) {
            return;
        }

        float partialTicks = timer.renderPartialTicks;
        Entity viewEntity = minecraft.getRenderViewEntity();
        minecraft.pointedEntity = null;
        double blockReach = minecraft.playerController.getBlockReachDistance();
        minecraft.objectMouseOver = viewEntity.rayTrace(blockReach, partialTicks);
        double blockDistance = blockReach;
        Vec3 eyes = viewEntity.getPositionEyes(partialTicks);

        if (minecraft.playerController.extendedReach()) {
            blockReach = 6.0D;
            blockDistance = 6.0D;
        } else {
            if (blockReach > range.get()) {
                blockDistance = range.get();
            }
            blockReach = blockDistance;
        }
        if (minecraft.objectMouseOver != null) {
            blockDistance = minecraft.objectMouseOver.hitVec.distanceTo(eyes);
        }

        Vec3 look = viewEntity.getLook(partialTicks);
        Vec3 end = eyes.addVector(look.xCoord * blockReach, look.yCoord * blockReach, look.zCoord * blockReach);
        Entity pointed = null;
        Vec3 hitVector = null;
        float border = 1.0F;
        AxisAlignedBB searchBox = viewEntity.getEntityBoundingBox()
                .addCoord(look.xCoord * blockReach, look.yCoord * blockReach, look.zCoord * blockReach)
                .expand(border, border, border);
        List<Entity> entities = minecraft.theWorld.getEntitiesWithinAABBExcludingEntity(viewEntity, searchBox);
        double closestDistance = blockDistance;

        for (Entity entity : entities) {
            if (!entity.canBeCollidedWith()) {
                continue;
            }
            float collisionBorder = entity.getCollisionBorderSize();
            AxisAlignedBB box = entity.getEntityBoundingBox().expand(
                    collisionBorder, collisionBorder, collisionBorder
            );
            MovingObjectPosition intercept = box.calculateIntercept(eyes, end);
            if (box.isVecInside(eyes)) {
                if (closestDistance > 0.0D || closestDistance == 0.0D) {
                    pointed = entity;
                    hitVector = intercept == null ? eyes : intercept.hitVec;
                    closestDistance = 0.0D;
                }
                continue;
            }
            if (intercept == null) {
                continue;
            }
            double entityDistance = eyes.distanceTo(intercept.hitVec);
            if (entityDistance < closestDistance || closestDistance == 0.0D) {
                if (entity == viewEntity.ridingEntity && !entity.canRiderInteract()) {
                    if (closestDistance == 0.0D) {
                        pointed = entity;
                        hitVector = intercept.hitVec;
                    }
                    continue;
                }
                pointed = entity;
                hitVector = intercept.hitVec;
                closestDistance = entityDistance;
            }
        }

        if (pointed != null && (closestDistance < blockDistance || minecraft.objectMouseOver == null)) {
            minecraft.objectMouseOver = new MovingObjectPosition(pointed, hitVector);
            if (pointed instanceof EntityLivingBase || pointed instanceof EntityItemFrame) {
                minecraft.pointedEntity = pointed;
            }
        }
    }

    private Settings setting(int index) {
        return this.sl.get(index);
    }
}
