package xenon.dev.utils;

import java.util.concurrent.ThreadLocalRandom;

public final class Variance {
    private Variance() {
    }

    public static double gaussian(double mean, double standardDeviation) {
        return mean + ThreadLocalRandom.current().nextGaussian() * Math.max(0.0D, standardDeviation);
    }

    public static double range(double minimum, double maximum) {
        if (minimum >= maximum) {
            return minimum;
        }
        return ThreadLocalRandom.current().nextDouble(minimum, maximum);
    }

    public static boolean chance(double percentage) {
        return percentage > 0.0D
                && (percentage >= 100.0D || ThreadLocalRandom.current().nextDouble(100.0D) < percentage);
    }

    public static double trend(double current, double volatility, double tension) {
        double impulse = gaussian(0.0D, Math.max(0.0D, volatility));
        double restoringForce = current * clamp(tension, 0.0D, 1.0D);
        return clamp(current + impulse - restoringForce, -4.0D, 4.0D);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
