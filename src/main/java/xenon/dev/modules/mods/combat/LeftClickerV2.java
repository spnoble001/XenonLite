package xenon.dev.modules.mods.combat;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;
import xenon.dev.events.XenonEvent;
import xenon.dev.modules.Mod;
import xenon.dev.modules.Module;
import xenon.dev.modules.Settings;
import xenon.dev.ui.ClickGuiObj;
import xenon.dev.utils.ClickerUtils;
import xenon.dev.utils.Variance;

@Mod(keybind = 0)
public final class LeftClickerV2 extends Module {
    private final ClickerUtils clickerUtils = new ClickerUtils();
    private long nextPressNanos;
    private long releaseNanos;
    private boolean pressed;
    private double currentTrend;

    public LeftClickerV2() {
        addNumber("CPS Mean", 12.0F, 1.0F, 25.0F);
        addNumber("CPS Std Dev", 1.0F, 0.0F, 5.0F);
        addNumber("CPS Min", 8.0F, 1.0F, 25.0F);
        addNumber("CPS Max", 16.0F, 1.0F, 25.0F);
        addNumber("CPS Fallout", 0.3F, 0.0F, 3.0F);
        addNumber("Spike Chance", 3.0F, 0.0F, 100.0F);
        addNumber("Spike Min", 1.0F, 0.0F, 8.0F);
        addNumber("Spike Max", 3.0F, 0.0F, 8.0F);
        addNumber("Stutter Chance", 4.0F, 0.0F, 100.0F);
        addNumber("Stutter Min", 1.0F, 0.0F, 8.0F);
        addNumber("Stutter Max", 3.0F, 0.0F, 8.0F);
        addNumber("Hold Mean", 35.0F, 1.0F, 150.0F);
        addNumber("Hold Std Dev", 8.0F, 0.0F, 75.0F);
        addNumber("Hold Min", 15.0F, 1.0F, 150.0F);
        addNumber("Hold Max", 75.0F, 1.0F, 150.0F);
        addNumber("Heavy Chance", 3.0F, 0.0F, 100.0F);
        addNumber("Heavy Min", 15.0F, 0.0F, 100.0F);
        addNumber("Heavy Max", 40.0F, 0.0F, 100.0F);
        addNumber("Rhythm Volatility", 0.20F, 0.0F, 2.0F);
        addNumber("Rhythm Tension", 0.15F, 0.0F, 1.0F);
        registerSettings();
        this.name = "Left Clicker V2";
    }

    @XenonEvent
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!canClick()) {
            stopClicking();
            return;
        }

        long now = System.nanoTime();
        if (this.pressed && now >= this.releaseNanos) {
            releaseAttack();
        }
        if (!this.pressed && (this.nextPressNanos == 0L || now >= this.nextPressNanos)) {
            pressAttack(now);
        }
    }

    private boolean canClick() {
        return this.enabled
                && this.minecraft.thePlayer != null
                && this.minecraft.theWorld != null
                && this.minecraft.inGameHasFocus
                && !(this.minecraft.currentScreen instanceof ClickGuiObj)
                && Mouse.isButtonDown(0);
    }

    private void pressAttack(long now) {
        long interval = calculateNextIntervalNanos();
        int attackKey = this.minecraft.gameSettings.keyBindAttack.getKeyCode();
        KeyBinding.setKeyBindState(attackKey, true);
        KeyBinding.onTick(attackKey);
        this.clickerUtils.sendFakeLeft();
        this.pressed = true;
        this.nextPressNanos = now + interval;
        this.releaseNanos = now + calculateHoldNanos(interval);
    }

    private void releaseAttack() {
        KeyBinding.setKeyBindState(this.minecraft.gameSettings.keyBindAttack.getKeyCode(), false);
        this.pressed = false;
    }

    private long calculateNextIntervalNanos() {
        this.currentTrend = Variance.trend(this.currentTrend, value(18), value(19));
        double cps = Variance.gaussian(value(0) + this.currentTrend, value(1));
        if (Variance.chance(value(5))) {
            cps += Variance.range(value(6), value(7));
        }
        if (Variance.chance(value(8))) {
            cps -= Variance.range(value(9), value(10));
        }
        if (cps > value(3)) {
            cps = value(3) + Variance.range(-value(4), value(4));
        }
        if (cps < value(2)) {
            cps = value(2) + Variance.range(-value(4), value(4));
        }
        cps = Math.max(0.5D, cps);
        return (long) (1_000_000_000.0D / cps);
    }

    private long calculateHoldNanos(long intervalNanos) {
        double cps = 1_000_000_000.0D / intervalNanos;
        double speedFactor = 1.0D - cps / 25.0D;
        double adjustedMean = value(11) * Math.max(0.8D, speedFactor);
        double hold = Variance.gaussian(adjustedMean, value(12));
        if (Variance.chance(value(15))) {
            hold += Variance.range(value(16), value(17));
        }
        hold = Math.max(value(13), Math.min(value(14), hold));
        long requested = (long) (hold * 1_000_000.0D);
        return Math.min(requested, (long) (intervalNanos * 0.85D));
    }

    private void stopClicking() {
        if (this.pressed) {
            releaseAttack();
        }
        this.nextPressNanos = 0L;
        this.releaseNanos = 0L;
        this.currentTrend = 0.0D;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stopClicking();
    }

    private void addNumber(String name, float value, float minimum, float maximum) {
        this.sl.add(new Settings(name.toCharArray(), this, value, minimum, maximum, false));
    }

    private double value(int index) {
        return this.sl.get(index).getValfloat();
    }

    @Override
    public char getSort() {
        return 'l';
    }
}
