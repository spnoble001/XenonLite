package xenon.dev.utils;

import java.lang.reflect.Field;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;

public final class ClickerUtils {
    private boolean drops;
    private boolean spikes;
    private int dropsReset;
    private int spikesReset;
    private int dropCounter;
    private int spikeCounter;
    private int clicks;
    private int clicksReset;
    private float editedCps;
    private final Field mouseButton;
    private final Field buttonState;

    public ClickerUtils() {
        this.mouseButton = findField(MouseEvent.class, int.class, "button");
        this.buttonState = findField(MouseEvent.class, boolean.class, "buttonstate");
    }

    public float randomization(float cps) {
        float safeCps = Math.max(1.0F, cps);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (this.clicksReset == 0 || this.clicks >= this.clicksReset) {
            this.clicksReset = random.nextInt(5, 15);
            this.clicks = 0;
            this.editedCps = Math.max(1.0F, safeCps - 3.0F + random.nextInt(0, 6));
        }
        this.clicks++;
        if (!this.drops && !this.spikes && random.nextInt(100) <= 40) {
            this.drops = true;
            this.dropsReset = random.nextInt(15, 25);
        }
        if (!this.drops && !this.spikes && random.nextInt(100) <= 25) {
            this.spikes = true;
            this.spikesReset = random.nextInt(15, 25);
        }
        if (this.drops && this.dropCounter++ <= this.dropsReset) {
            return random.nextInt(1050, 1100) / this.editedCps;
        }
        if (this.drops) {
            this.dropCounter = 0;
            this.drops = false;
        }
        if (this.spikes && this.spikeCounter++ <= this.spikesReset) {
            return random.nextInt(900, 950) / this.editedCps;
        }
        if (this.spikes) {
            this.spikeCounter = 0;
            this.spikes = false;
        }
        return random.nextInt(random.nextInt(925, 975), random.nextInt(1025, 1075)) / this.editedCps;
    }

    public void sendFakeRight() {
        sendFakeMouseEvent(1);
    }

    public void sendFakeLeft() {
        sendFakeMouseEvent(0);
    }

    private void sendFakeMouseEvent(int button) {
        if (this.mouseButton == null || this.buttonState == null) {
            return;
        }
        try {
            MouseEvent event = new MouseEvent();
            this.mouseButton.setInt(event, button);
            this.buttonState.setBoolean(event, true);
            MinecraftForge.EVENT_BUS.post(event);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("No se pudo crear el evento de ratón", exception);
        }
    }

    private static Field findField(Class<?> type, Class<?> fieldType, String preferredName) {
        for (Field field : type.getDeclaredFields()) {
            if (field.getType() == fieldType && field.getName().equalsIgnoreCase(preferredName)) {
                field.setAccessible(true);
                return field;
            }
        }
        for (Field field : type.getDeclaredFields()) {
            if (field.getType() == fieldType) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }
}
