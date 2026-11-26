package dev.rawify.rawcarry.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bookkeeping for the interaction listener's delayed tasks: a delayed drop after sneaking and
 * the short window that turns a left click into a yeet.
 *
 * <p>Kept free of Bukkit types on purpose, so the quit cleanup can be tested without a server.
 * Only the cancel action of a task is stored, because the listener never needs the task again.
 */
final class InteractionTimers {

    @FunctionalInterface
    interface Cancelable {
        void cancel();
    }

    private final Map<UUID, Cancelable> pendingDrops = new HashMap<>();
    private final Map<UUID, Cancelable> lateClicks = new HashMap<>();

    boolean hasPendingDrop(UUID playerId) {
        return pendingDrops.containsKey(playerId);
    }

    boolean hasLateClick(UUID playerId) {
        return lateClicks.containsKey(playerId);
    }

    void putPendingDrop(UUID playerId, Cancelable task) {
        pendingDrops.put(playerId, task);
    }

    Cancelable removePendingDrop(UUID playerId) {
        return pendingDrops.remove(playerId);
    }

    void putLateClick(UUID playerId, Cancelable task) {
        lateClicks.put(playerId, task);
    }

    Cancelable removeLateClick(UUID playerId) {
        return lateClicks.remove(playerId);
    }

    /**
     * Cancels and forgets every delayed task of one player. Called when they leave, so no task
     * keeps running for an offline player.
     */
    int cancelAll(UUID playerId) {
        int cancelled = 0;
        if (cancel(pendingDrops.remove(playerId))) {
            cancelled++;
        }
        if (cancel(lateClicks.remove(playerId))) {
            cancelled++;
        }
        return cancelled;
    }

    private static boolean cancel(Cancelable task) {
        if (task == null) {
            return false;
        }
        task.cancel();
        return true;
    }
}
