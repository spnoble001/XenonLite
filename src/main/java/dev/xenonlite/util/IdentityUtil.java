package dev.xenonlite.util;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class IdentityUtil {
    private IdentityUtil() {
    }

    public static UUID randomUuid() {
        return new UUID(
                ThreadLocalRandom.current().nextLong(),
                ThreadLocalRandom.current().nextLong()
        );
    }

    public static int randomInt(int minimumInclusive, int maximumExclusive) {
        if (minimumInclusive >= maximumExclusive) {
            throw new IllegalArgumentException("El mínimo debe ser menor que el máximo");
        }
        return ThreadLocalRandom.current().nextInt(minimumInclusive, maximumExclusive);
    }
}
