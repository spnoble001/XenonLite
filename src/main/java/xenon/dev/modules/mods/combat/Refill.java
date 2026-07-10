package xenon.dev.modules.mods.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import xenon.dev.events.XenonEvent;
import xenon.dev.modules.Mod;
import xenon.dev.modules.Module;
import xenon.dev.modules.Settings;
import xenon.dev.utils.ClickerUtils;
import xenon.dev.utils.PotionUtils;
import xenon.dev.utils.XObject;

@Mod(keybind = 0)
public final class Refill extends Module {
    private long lastClick;
    private final ClickerUtils utils;

    public Refill() {
        this.sl.add(new Settings("Speed".toCharArray(), this, 4.0F, 3.0F, 10.0F, false));
        this.sl.add(new Settings("Pot".toCharArray(), this, true));
        this.sl.add(new Settings("Obsidian".toCharArray(), this, false));
        this.sl.add(new Settings("Random".toCharArray(), this, false));
        registerSettings();
        this.name = "Refill";
        this.utils = new ClickerUtils();
    }

    @Override
    public char getSort() {
        return 'r';
    }

    @XenonEvent
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (this.minecraft.theWorld == null
                || !this.enabled
                || this.minecraft.thePlayer == null
                || !(this.minecraft.currentScreen instanceof GuiInventory)) {
            return;
        }

        if (setting(3).getValBoolean()) {
            refillRandomSlot();
        } else {
            refillFirstAvailableSlot();
        }
    }

    private void refillRandomSlot() {
        int slot = getAvailableSlot();
        if (slot != -1 && clickDelayElapsed()) {
            moveToHotbar(slot);
        }
    }

    private void refillFirstAvailableSlot() {
        ItemStack[] inventory = this.minecraft.thePlayer.inventory.mainInventory;
        for (int slot = 9; slot < inventory.length && !isHotbarFull(); slot++) {
            ItemStack stack = inventory[slot];
            if (stack != null && clickDelayElapsed() && canMove(stack)) {
                moveToHotbar(slot);
                break;
            }
        }
    }

    private int getAvailableSlot() {
        if (isHotbarFull()) {
            return -1;
        }
        List<Integer> slots = new ArrayList<Integer>();
        ItemStack[] inventory = this.minecraft.thePlayer.inventory.mainInventory;
        for (int slot = 9; slot < inventory.length; slot++) {
            ItemStack stack = inventory[slot];
            if (stack != null && canMove(stack)) {
                slots.add(slot);
            }
        }
        if (slots.isEmpty()) {
            return -1;
        }
        return slots.get(slots.size() == 1 ? 0 : ThreadLocalRandom.current().nextInt(slots.size()));
    }

    private boolean canMove(ItemStack stack) {
        boolean potions = setting(1).getValBoolean();
        boolean obsidian = setting(2).getValBoolean();
        boolean isPotion = PotionUtils.isSplashPotion(new XObject<ItemStack>(stack));
        boolean isObsidian = stack.getItem() instanceof ItemBlock
                && ((ItemBlock) stack.getItem()).getBlock() == Blocks.obsidian;

        if (potions && obsidian) {
            return isPotion || isObsidian;
        }
        if (potions) {
            return isPotion;
        }
        if (obsidian) {
            return isObsidian;
        }
        return true;
    }

    private boolean isHotbarFull() {
        for (int slot = 0; slot < 9; slot++) {
            if (this.minecraft.thePlayer.inventory.mainInventory[slot] == null) {
                return false;
            }
        }
        return true;
    }

    private boolean clickDelayElapsed() {
        return (float) (System.currentTimeMillis() - this.lastClick)
                >= this.utils.randomization(setting(0).getValfloat());
    }

    private void moveToHotbar(int slot) {
        this.minecraft.playerController.windowClick(
                0,
                slot,
                0,
                1,
                (EntityPlayer) this.minecraft.thePlayer
        );
        this.lastClick = System.currentTimeMillis();
    }

    private Settings setting(int index) {
        return this.sl.get(index);
    }
}
