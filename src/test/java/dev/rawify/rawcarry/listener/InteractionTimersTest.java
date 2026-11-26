package dev.rawify.rawcarry.listener;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionTimersTest {

    private final InteractionTimers timers = new InteractionTimers();
    private final List<String> cancelled = new ArrayList<>();

    @Test
    void quitCancelsBothDelayedTasksAndForgetsThem() {
        UUID player = UUID.randomUUID();
        timers.putPendingDrop(player, () -> cancelled.add("drop"));
        timers.putLateClick(player, () -> cancelled.add("click"));

        assertEquals(2, timers.cancelAll(player));
        assertEquals(List.of("drop", "click"), cancelled);
        assertFalse(timers.hasPendingDrop(player));
        assertFalse(timers.hasLateClick(player));
    }

    @Test
    void quitOnlyTouchesThePlayerWhoLeft() {
        UUID leaving = UUID.randomUUID();
        UUID staying = UUID.randomUUID();
        timers.putPendingDrop(leaving, () -> cancelled.add("leaving"));
        timers.putLateClick(staying, () -> cancelled.add("staying"));

        assertEquals(1, timers.cancelAll(leaving));
        assertEquals(List.of("leaving"), cancelled);
        assertFalse(timers.hasPendingDrop(leaving));
        assertTrue(timers.hasLateClick(staying));
    }

    @Test
    void quitWithoutTimersCancelsNothing() {
        assertEquals(0, timers.cancelAll(UUID.randomUUID()));
        assertTrue(cancelled.isEmpty());
    }
}
