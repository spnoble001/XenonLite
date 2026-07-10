package xenon.dev.utils;

import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;

public final class PotionUtils {
    public static long watermark = System.currentTimeMillis();

    private PotionUtils() {
    }

    public static boolean isSplashPotion(XObject<?> wrappedStack) {
        if (wrappedStack == null || !(wrappedStack.get() instanceof ItemStack)) {
            return false;
        }
        ItemStack stack = (ItemStack) wrappedStack.get();
        return stack.getItem() instanceof ItemPotion
                && stack.getItemUseAction() == EnumAction.DRINK
                && stack.getMetadata() == 16421;
    }
}
