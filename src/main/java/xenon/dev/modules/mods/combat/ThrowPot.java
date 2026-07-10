package xenon.dev.modules.mods.combat;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import xenon.dev.modules.Mod;
import xenon.dev.modules.Module;
import xenon.dev.modules.Settings;
import xenon.dev.utils.ClickerUtils;
import xenon.dev.utils.MagicUtils;
import xenon.dev.utils.PerlinNoise;
import xenon.dev.utils.PotionUtils;
import xenon.dev.utils.XObject;

@Mod(keybind = 0)
public final class ThrowPot extends Module {
    private volatile boolean throwing;
    private final PerlinNoise transitionNoise = new PerlinNoise(UUID.randomUUID().getMostSignificantBits());
    private long sequence;

    public ThrowPot() {
        this.name = "Throw Pot";
        setKeybinding(Keyboard.KEY_F);
        this.sl.add(new Settings(
                "Speed".toCharArray(),
                this,
                MagicUtils.xor(250),
                MagicUtils.xor(200),
                MagicUtils.xor(300),
                true
        ));
        this.sl.add(new Settings("Detour".toCharArray(), this, true));
        registerSettings();
    }

    private void throwItem(final int slot, final int initialSlot) {
        this.throwing = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean restored = false;
                try {
                    long totalDelay = sampleLogNormalDelay(setting(0).getValfloat());
                    int detourSlot = chooseDetourSlot(initialSlot, slot);
                    if (setting(1).getValBoolean() && detourSlot != -1 && shouldUseDetour()) {
                        long detourDelay = detourDelay(totalDelay);
                        selectSlot(detourSlot);
                        Thread.sleep(detourDelay);
                        selectSlot(slot);
                        pressUseItem();
                        Thread.sleep(totalDelay - detourDelay);
                    } else {
                        selectSlot(slot);
                        pressUseItem();
                        Thread.sleep(totalDelay);
                    }
                    selectSlot(initialSlot);
                    restored = true;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    exception.printStackTrace();
                } finally {
                    if (!restored) {
                        scheduleSlotRestore(initialSlot);
                    }
                    throwing = false;
                }
            }
        }, "XenonLite-ThrowPot");
        thread.setDaemon(true);
        thread.start();
    }

    private void selectSlot(final int slot) throws InterruptedException, ExecutionException {
        this.minecraft.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (minecraft.thePlayer != null) {
                    minecraft.thePlayer.inventory.currentItem = slot;
                    minecraft.playerController.updateController();
                }
            }
        }).get();
    }

    private void pressUseItem() throws InterruptedException, ExecutionException {
        this.minecraft.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                int useKey = minecraft.gameSettings.keyBindUseItem.getKeyCode();
                KeyBinding.setKeyBindState(useKey, true);
                KeyBinding.onTick(useKey);
                new ClickerUtils().sendFakeRight();
                KeyBinding.setKeyBindState(useKey, false);
            }
        }).get();
    }

    private void scheduleSlotRestore(final int slot) {
        this.minecraft.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (minecraft.thePlayer != null) {
                    minecraft.thePlayer.inventory.currentItem = slot;
                    minecraft.playerController.updateController();
                }
            }
        });
    }

    private long sampleLogNormalDelay(float requestedCenter) {
        double center = Math.max(200.0D, Math.min(300.0D, requestedCenter));
        double sigma = 0.12D;
        double mu = Math.log(center) - sigma * sigma / 2.0D;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 16; attempt++) {
            long sample = Math.round(Math.exp(mu + sigma * random.nextGaussian()));
            if (sample >= 200L && sample <= 300L) {
                return sample;
            }
        }
        return Math.round(center);
    }

    private boolean shouldUseDetour() {
        double correlated = (this.transitionNoise.sample(this.sequence++ * 0.17D) + 1.0D) * 0.5D;
        return correlated > 0.52D;
    }

    private int chooseDetourSlot(int initialSlot, int potionSlot) {
        int start = (int) Math.floor(Math.abs(this.transitionNoise.sample(this.sequence * 0.23D)) * 9.0D) % 9;
        for (int offset = 0; offset < 9; offset++) {
            int candidate = (start + offset) % 9;
            if (candidate != initialSlot && candidate != potionSlot) {
                return candidate;
            }
        }
        return -1;
    }

    private long detourDelay(long totalDelay) {
        double noise = (this.transitionNoise.sample(this.sequence * 0.31D + 41.0D) + 1.0D) * 0.5D;
        double ratio = 0.12D + noise * 0.13D;
        return Math.max(20L, Math.min(totalDelay - 20L, Math.round(totalDelay * ratio)));
    }

    @Override
    public void toggle() {
        super.toggle();
        onDisable();
        if (this.minecraft.theWorld == null || this.minecraft.thePlayer == null || this.throwing) {
            return;
        }

        int initialSlot = this.minecraft.thePlayer.inventory.currentItem;
        AtomicInteger currentSlot = new AtomicInteger(0);
        while (currentSlot.get() < 9) {
            ItemStack stack = this.minecraft.thePlayer.inventory.mainInventory[currentSlot.get()];
            if (stack != null && PotionUtils.isSplashPotion(new XObject<ItemStack>(stack))) {
                throwItem(currentSlot.get(), initialSlot);
                break;
            }
            currentSlot.incrementAndGet();
        }
    }

    private Settings setting(int index) {
        return this.sl.get(index);
    }

    public boolean isThrowing() {
        return this.throwing;
    }

    @Override
    public char getSort() {
        return 't';
    }
}
