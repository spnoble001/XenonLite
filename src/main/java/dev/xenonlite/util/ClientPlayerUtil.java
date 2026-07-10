package dev.xenonlite.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

public final class ClientPlayerUtil {
    private ClientPlayerUtil() {
    }

    public static EntityPlayerSP getPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

    public static boolean isAvailable() {
        return getPlayer() != null;
    }
}
