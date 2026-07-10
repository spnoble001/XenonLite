package xenon.dev.utils;

import java.util.Random;

/** Ruido Perlin unidimensional, determinista y continuo. */
public final class PerlinNoise {
    private final int[] permutation = new int[512];

    public PerlinNoise(long seed) {
        int[] values = new int[256];
        for (int i = 0; i < values.length; i++) {
            values[i] = i;
        }
        Random random = new Random(seed);
        for (int i = values.length - 1; i > 0; i--) {
            int swap = random.nextInt(i + 1);
            int temporary = values[i];
            values[i] = values[swap];
            values[swap] = temporary;
        }
        for (int i = 0; i < this.permutation.length; i++) {
            this.permutation[i] = values[i & 255];
        }
    }

    public double sample(double x) {
        int cell = fastFloor(x);
        double local = x - cell;
        int leftHash = this.permutation[cell & 255];
        int rightHash = this.permutation[(cell + 1) & 255];
        double left = gradient(leftHash, local);
        double right = gradient(rightHash, local - 1.0D);
        return lerp(left, right, fade(local)) * 2.0D;
    }

    private static int fastFloor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static double fade(double value) {
        return value * value * value * (value * (value * 6.0D - 15.0D) + 10.0D);
    }

    private static double gradient(int hash, double distance) {
        return (hash & 1) == 0 ? distance : -distance;
    }

    private static double lerp(double first, double second, double amount) {
        return first + amount * (second - first);
    }
}
