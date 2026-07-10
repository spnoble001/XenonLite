package dev.xenonlite.util;

import org.lwjgl.input.Mouse;

public final class MouseUtil {
    private static final int LEFT_BUTTON = 0;
    private static final int RIGHT_BUTTON = 1;

    private MouseUtil() {
    }

    public static boolean isLeftButtonDown() {
        return Mouse.isCreated() && Mouse.isButtonDown(LEFT_BUTTON);
    }

    public static boolean isRightButtonDown() {
        return Mouse.isCreated() && Mouse.isButtonDown(RIGHT_BUTTON);
    }
}
