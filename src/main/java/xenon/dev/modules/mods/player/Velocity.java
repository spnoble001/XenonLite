package xenon.dev.modules.mods.player;

import java.util.Random;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xenon.dev.events.XenonEvent;
import xenon.dev.modules.Mod;
import xenon.dev.modules.Module;
import xenon.dev.modules.Settings;
import xenon.dev.utils.MagicUtils;

@Mod(keybind = 0)
public final class Velocity extends Module {
    private long lastTick;

    public Velocity() {
        this.sl.add(new Settings("Vert".toCharArray(), this, 1.0F,
                MagicUtils.xor(0), MagicUtils.xor(2), false));
        this.sl.add(new Settings("Hor".toCharArray(), this, 1.0F,
                MagicUtils.xor(0), MagicUtils.xor(2), false));
        this.sl.add(new Settings("Chance".toCharArray(), this, MagicUtils.xor(100),
                MagicUtils.xor(2), MagicUtils.xor(100), true));
        registerSettings();
        this.name = "Velocity";
    }

    @Override
    public char getSort() {
        return 'v';
    }

    @XenonEvent
    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!this.enabled) {
            return;
        }
        EntityPlayerSP player = this.minecraft.thePlayer;
        if (player == null || event.entityLiving != player) {
            return;
        }

        float vertical = setting(0).getValfloat();
        float horizontal = setting(1).getValfloat();
        long seed = (long) ((float) System.nanoTime()
                * (1.0F + (1.0F - vertical) * (1.0F - horizontal)));
        Random random = new Random(seed);
        if (random.nextInt(100) <= setting(2).getValfloat()) {
            if (this.lastTick == player.ticksExisted) {
                return;
            }
            if (player.hurtResistantTime == player.maxHurtResistantTime
                    && player.maxHurtResistantTime != 0) {
                player.motionX *= horizontal;
                player.motionZ *= horizontal;
                player.motionY *= vertical;
                this.lastTick = player.ticksExisted;
            }
        }
    }

    private Settings setting(int index) {
        return this.sl.get(index);
    }
}
