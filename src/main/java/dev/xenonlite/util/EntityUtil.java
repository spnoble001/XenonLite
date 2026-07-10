package dev.xenonlite.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public final class EntityUtil {
    private EntityUtil() {
    }

    public static boolean isPlayer(Entity entity) {
        return entity instanceof EntityPlayer;
    }

    public static EntityPlayer asPlayer(Entity entity) {
        return isPlayer(entity) ? (EntityPlayer) entity : null;
    }

    public static double distanceSquared(Entity first, Entity second) {
        if (first == null || second == null) {
            return Double.POSITIVE_INFINITY;
        }
        return first.getDistanceSqToEntity(second);
    }
}
