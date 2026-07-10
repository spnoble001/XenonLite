package xenon.dev.utils;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.MathHelper;

public class Pattern {
    public boolean init;
    public double changeX;
    public double changeY;
    public double changeZ;

    double clamp(double number, double minimum, double maximum) {
        return number < minimum ? minimum : Math.min(number, maximum);
    }

    double randomize(int type) {
        double add = ThreadLocalRandom.current().nextInt(1, 10) < 5 ? 1.0D : -1.0D;
        if (type == 0) {
            if (this.changeX < 0.0D) {
                if (ThreadLocalRandom.current().nextInt(1, 5) < 3) {
                    add = 1.0D;
                }
            } else if (ThreadLocalRandom.current().nextInt(1, 5) < 3) {
                add = -1.0D;
            }
        } else if (this.changeZ < 0.0D) {
            if (ThreadLocalRandom.current().nextInt(1, 5) < 3) {
                add = 1.0D;
            }
        } else if (ThreadLocalRandom.current().nextInt(1, 5) < 3) {
            add = -1.0D;
        }
        return add
                * ThreadLocalRandom.current().nextDouble(1.0D, 2.0D)
                * MathHelper.sin((float) (System.currentTimeMillis() % 360L));
    }
}
