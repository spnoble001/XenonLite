package xenon.dev.utils;

public final class RightClickCoordinator {
    public interface Owner {
        void onRightClickControlLost();
    }

    private static Owner owner;
    private static int priority;

    private RightClickCoordinator() {
    }

    public static synchronized boolean acquire(Owner requester, int requestedPriority) {
        if (owner == requester) {
            return true;
        }
        if (owner != null && requestedPriority <= priority) {
            return false;
        }
        if (owner != null) {
            owner.onRightClickControlLost();
        }
        owner = requester;
        priority = requestedPriority;
        return true;
    }

    public static synchronized void release(Owner requester) {
        if (owner == requester) {
            owner = null;
            priority = 0;
        }
    }

    public static synchronized boolean isOwner(Owner requester) {
        return owner == requester;
    }
}
