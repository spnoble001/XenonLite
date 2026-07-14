package xenon.dev.modules.mods.combat;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import xenon.dev.modules.Mod;
import xenon.dev.modules.Module;
import xenon.dev.modules.Settings;
import xenon.dev.utils.ClickerUtils;
import xenon.dev.utils.MagicUtils;
import xenon.dev.utils.PerlinNoise;
import xenon.dev.utils.PotionUtils;
import xenon.dev.utils.RightClickCoordinator;
import xenon.dev.utils.XObject;

@Mod(keybind = 0)
public final class ThrowPot extends Module implements RightClickCoordinator.Owner {
    private static final int INPUT_PRIORITY = 100;
    private static final Field RIGHT_CLICK_DELAY_TIMER = ReflectionHelper.findField(
            Minecraft.class, "rightClickDelayTimer", "field_71467_ac");
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "XenonLite-ThrowPot-Timer");
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private volatile boolean throwing;
    private final PerlinNoise transitionNoise = new PerlinNoise(UUID.randomUUID().getMostSignificantBits());
    private long sequence;
    private final PerlinNoise autoPotNoise = new PerlinNoise(UUID.randomUUID().getLeastSignificantBits());

    public ThrowPot() {
        this.name = "Throw Pot";
        setKeybinding(Keyboard.KEY_F);
        this.sl.add(new Settings(
                "Speed".toCharArray(), this, MagicUtils.xor(250), MagicUtils.xor(200),
                MagicUtils.xor(300), true));
        this.sl.add(new Settings("Detour".toCharArray(), this, true));
        registerSettings();
    }

    private void throwItem(final int potionSlot, final int initialSlot) {
        if (!RightClickCoordinator.acquire(this, INPUT_PRIORITY)) {
            return;
        }
        this.throwing = true;
        final long totalDelay = sampleLogNormalDelay(setting(0).getValfloat());
        int candidate = chooseDetourSlot(initialSlot, potionSlot);
        boolean useDetour = setting(1).getValBoolean() && candidate != -1 && shouldUseDetour();
        if (useDetour) {
            final int detourSlot = candidate;
            final long firstDelay = detourDelay(totalDelay);
            enqueueMainThread(new Runnable() {
                @Override
                public void run() {
                    selectSlotNow(detourSlot);
                    schedule(firstDelay, new Runnable() {
                        @Override
                        public void run() {
                            enqueuePotionThrow(potionSlot, initialSlot, totalDelay - firstDelay);
                        }
                    });
                }
            });
        } else {
            enqueuePotionThrow(potionSlot, initialSlot, totalDelay);
        }
    }

    private void enqueuePotionThrow(final int potionSlot, final int initialSlot, final long restoreDelay) {
        enqueueMainThread(new Runnable() {
            @Override
            public void run() {
                if (!canContinueThrow()) {
                    finishThrow(initialSlot);
                    return;
                }
                selectSlotNow(potionSlot);
                throwPotionNow();
                schedule(restoreDelay, new Runnable() {
                    @Override
                    public void run() {
                        enqueueMainThread(new Runnable() {
                            @Override
                            public void run() {
                                finishThrow(initialSlot);
                            }
                        });
                    }
                });
            }
        });
    }

    private void throwPotionNow() {
        setRightClickDelayTimer(0);
        int useKey = this.minecraft.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(useKey, true);
        KeyBinding.onTick(useKey);
        new ClickerUtils().sendFakeRight();
        KeyBinding.setKeyBindState(useKey, false);
    }

    public boolean triggerPrioritaryThrow() {
        if (this.throwing || this.minecraft.thePlayer == null || this.minecraft.theWorld == null) {
            return false;
        }
        int potionSlot = findPotionSlot();
        if (potionSlot < 0 || !RightClickCoordinator.acquire(this, INPUT_PRIORITY)) {
            return false;
        }

        this.throwing = true;
        int initialSlot = this.minecraft.thePlayer.inventory.currentItem;
        float originalPitch = this.minecraft.thePlayer.rotationPitch;
        float originalYaw = this.minecraft.thePlayer.rotationYaw;
        try {
            selectSlotNow(potionSlot);
            float silentPitch = 85.0F;
            float silentYaw = originalYaw
                    + (float) (this.autoPotNoise.sample(System.nanoTime() / 1.0E9D) * 1.5D);
            this.minecraft.thePlayer.rotationPitch = silentPitch;
            this.minecraft.thePlayer.rotationYaw = silentYaw;
            this.minecraft.thePlayer.sendQueue.addToSendQueue(
                    new C03PacketPlayer.C05PacketPlayerLook(
                            silentYaw, silentPitch, this.minecraft.thePlayer.onGround));
            setRightClickDelayTimer(0);
            new ClickerUtils().sendFakeRight();
            this.minecraft.playerController.sendUseItem(
                    this.minecraft.thePlayer, this.minecraft.theWorld,
                    this.minecraft.thePlayer.getHeldItem());
            return true;
        } finally {
            this.minecraft.thePlayer.rotationPitch = originalPitch;
            this.minecraft.thePlayer.rotationYaw = originalYaw;
            selectSlotNow(initialSlot);
            this.throwing = false;
            RightClickCoordinator.release(this);
        }
    }

    public boolean hasPotion() {
        return findPotionSlot() >= 0;
    }

    private int findPotionSlot() {
        if (this.minecraft.thePlayer == null) {
            return -1;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = this.minecraft.thePlayer.inventory.mainInventory[slot];
            if (stack != null && PotionUtils.isSplashPotion(new XObject<ItemStack>(stack))) {
                return slot;
            }
        }
        return -1;
    }

    private void finishThrow(int initialSlot) {
        if (this.minecraft.thePlayer != null) {
            selectSlotNow(initialSlot);
        }
        this.throwing = false;
        RightClickCoordinator.release(this);
    }

    private boolean canContinueThrow() {
        return this.throwing
                && RightClickCoordinator.isOwner(this)
                && this.minecraft.thePlayer != null
                && this.minecraft.theWorld != null;
    }

    private void selectSlotNow(int slot) {
        if (this.minecraft.thePlayer != null) {
            this.minecraft.thePlayer.inventory.currentItem = slot;
            this.minecraft.playerController.updateController();
        }
    }

    private void enqueueMainThread(final Runnable action) {
        this.minecraft.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                try {
                    action.run();
                } catch (RuntimeException exception) {
                    exception.printStackTrace();
                    throwing = false;
                    RightClickCoordinator.release(ThrowPot.this);
                }
            }
        });
    }

    private static void schedule(long delay, Runnable action) {
        TIMER.schedule(action, Math.max(0L, delay), TimeUnit.MILLISECONDS);
    }

    private void setRightClickDelayTimer(int value) {
        try {
            RIGHT_CLICK_DELAY_TIMER.setInt(this.minecraft, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not reset Minecraft right click delay", exception);
        }
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
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = this.minecraft.thePlayer.inventory.mainInventory[slot];
            if (stack != null && PotionUtils.isSplashPotion(new XObject<ItemStack>(stack))) {
                throwItem(slot, initialSlot);
                return;
            }
        }
    }

    @Override
    public void onRightClickControlLost() {
        this.throwing = false;
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
