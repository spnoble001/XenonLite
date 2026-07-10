package xenon.dev;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Xenon {
    private static final Xenon INSTANCE = new Xenon();

    public final Set<UUID> friendsList = Collections.newSetFromMap(
            new ConcurrentHashMap<UUID, Boolean>()
    );

    private Xenon() {
    }

    public static Xenon getInstance() {
        return INSTANCE;
    }
}
