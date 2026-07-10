package xenon.dev.modules.mods.combat;

import java.util.Arrays;
import java.util.Random;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.input.Mouse;
import xenon.dev.events.XenonEvent;
import xenon.dev.modules.Mod;
import xenon.dev.modules.Module;
import xenon.dev.modules.Settings;
import xenon.dev.ui.ClickGuiObj;
import xenon.dev.utils.ClickerUtils;
import xenon.dev.utils.MagicUtils;

@Mod(keybind = 0)
public final class LeftClicker extends Module {
    private long lastClick;
    private long lastDown;
    private boolean released = true;
    private int updateKey;
    private final ClickerUtils utils;
    private double delay = -1.0D;
    private double up = -1.0D;
    private double down = -1.0D;
    private int cpsSize;
    private float[] cpsTargets;
    private int cpsIndex;
    private int cpsCounter;

    public LeftClicker() {
        this.cpsSize = 4;
        this.cpsIndex = 0;
        this.cpsCounter = 10;
        this.sl.add(new Settings("Min".toCharArray(), this, MagicUtils.xor(9), 1, 20, false));
        this.sl.add(new Settings("Sword".toCharArray(), this, true));
        this.sl.add(new Settings("Axe".toCharArray(), this, true));
        this.sl.add(new Settings("Block".toCharArray(), this, true));
        this.sl.add(new Settings("Break".toCharArray(), this, true));
        this.sl.add(new Settings("Max".toCharArray(), this, MagicUtils.xor(12), 1, 20, false));
        registerSettings();
        this.name = "Left Clicker";
        this.utils = new ClickerUtils();
        update();
    }

    @Override
    public char getSort() {
        return 'l';
    }

    @Override
    public void onDisable() {
        super.onDisable();
        setAttackKey(false);
        this.delay = -1.0D;
        this.released = true;
    }

    @XenonEvent
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent event) {
        if (!(event instanceof RenderGameOverlayEvent.Post)
                || event.type != RenderGameOverlayEvent.ElementType.ALL
                || !this.enabled
                || this.minecraft.thePlayer == null
                || this.minecraft.theWorld == null
                || this.minecraft.currentScreen instanceof ClickGuiObj) {
            return;
        }

        if (shouldKeepBreakingBlock()) {
            setAttackKey(true);
            return;
        }
        if (!heldItemAllowed()) {
            releaseIfNecessary();
            return;
        }

        long now = System.currentTimeMillis();
        if (this.delay < 0.0D) {
            scheduleNextClick(now);
        }

        Mouse.poll();
        if (!Mouse.isButtonDown(0)) {
            releaseIfNecessary();
            this.delay = -1.0D;
            return;
        }

        if (now > this.down) {
            pressAttack(now);
        } else if (now > this.up) {
            releaseAttack();
        }
    }

    private boolean shouldKeepBreakingBlock() {
        return this.minecraft.objectMouseOver != null
                && Mouse.isButtonDown(0)
                && setting(4).getValBoolean()
                && this.minecraft.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    private boolean heldItemAllowed() {
        boolean sword = setting(1).getValBoolean();
        boolean axe = setting(MagicUtils.xor(2)).getValBoolean();
        if (!sword && !axe) {
            return true;
        }
        if (this.minecraft.thePlayer.getHeldItem() == null) {
            return false;
        }
        Item item = this.minecraft.thePlayer.getHeldItem().getItem();
        return (sword && item instanceof ItemSword) || (axe && item instanceof ItemAxe);
    }

    private void pressAttack(long now) {
        int attackKey = this.minecraft.gameSettings.keyBindAttack.getKeyCode();
        KeyBinding.setKeyBindState(attackKey, true);
        KeyBinding.onTick(attackKey);
        this.utils.sendFakeLeft();
        this.lastClick = now;
        this.lastDown = now;
        this.released = false;
        scheduleNextClick(now);
    }

    private void releaseAttack() {
        setAttackKey(false);
        if (!this.released && this.minecraft.pointedEntity != null && setting(3).getValBoolean()
                && RandomUtils.nextInt(0, 100) <= 10) {
            int useKey = this.minecraft.gameSettings.keyBindUseItem.getKeyCode();
            KeyBinding.setKeyBindState(useKey, true);
            KeyBinding.onTick(useKey);
            this.utils.sendFakeRight();
            KeyBinding.setKeyBindState(useKey, false);
        }
        this.released = true;
    }

    private void releaseIfNecessary() {
        if (!this.released) {
            setAttackKey(false);
            this.released = true;
        }
    }

    private void scheduleNextClick(long now) {
        this.delay = generateDelay();
        this.up = now + this.delay / 2.0D - RandomUtils.nextInt(0, 15) + RandomUtils.nextInt(0, 15);
        this.down = now + this.delay;
    }

    private void setAttackKey(boolean pressed) {
        KeyBinding.setKeyBindState(this.minecraft.gameSettings.keyBindAttack.getKeyCode(), pressed);
    }

    private Settings setting(int index) {
        return this.sl.get(index);
    }

    private double generateDelay() {
        if (this.cpsTargets == null || this.cpsTargets.length == 0) {
            update();
        }
        float cps = this.cpsTargets[this.cpsIndex];
        this.cpsCounter++;
        if (this.cpsCounter >= 4) {
            if (this.updateKey > 0 && this.cpsSize > 1) {
                int old = this.cpsIndex;
                do {
                    this.cpsIndex = new Random().nextInt(this.cpsSize);
                } while (this.cpsIndex == old);
            }
            this.cpsCounter = 0;
        }
        return this.utils.randomization(cps);
    }

    public void update() {
        this.cpsSize = 4;
        float minCps = setting(0).getValfloat();
        float maxCps = setting(5).getValfloat();
        this.cpsTargets = new float[this.cpsSize];
        if (minCps >= maxCps) {
            Arrays.fill(this.cpsTargets, minCps);
            this.updateKey = -1;
            this.cpsIndex = 0;
            return;
        }
        float increment = (maxCps - minCps) / this.cpsSize;
        this.updateKey = increment > 0.0F ? 1 : -1;
        for (int i = 0; i < this.cpsTargets.length; i++) {
            this.cpsTargets[i] = minCps + i * increment;
        }
        this.cpsIndex = new Random().nextInt(this.cpsSize);
    }

    public long getLastClick() {
        return this.lastClick;
    }

    public long getLastDown() {
        return this.lastDown;
    }
}
