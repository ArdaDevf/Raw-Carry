package dev.rawify.rawcarry.carry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarryManagerLogicTest {

    @Test
    void freshCarryOfSinglePlayerAllowed() {
        assertFalse(CarryManager.exceedsStackLimit(1, 1, 3));
    }

    @Test
    void headCarryingMaxStackPlayersAllowed() {
        assertFalse(CarryManager.exceedsStackLimit(3, 1, 3));
    }

    @Test
    void fourthCarriedPlayerRejected() {
        assertTrue(CarryManager.exceedsStackLimit(4, 1, 3));
    }

    @Test
    void mergingTwoChainsReachesLimit() {
        assertFalse(CarryManager.exceedsStackLimit(2, 2, 3));
    }

    @Test
    void mergingTwoChainsOverLimitRejected() {
        assertTrue(CarryManager.exceedsStackLimit(3, 2, 3));
    }

    @Test
    void maxStackOneAllowsSingleCarryOnly() {
        assertFalse(CarryManager.exceedsStackLimit(1, 1, 1));
        assertTrue(CarryManager.exceedsStackLimit(2, 1, 1));
    }

    @Test
    void cleanJoinerGetsCleanup() {
        assertTrue(CarryManager.shouldClearLeftoverSlowdown(false, false));
    }

    @Test
    void joinerCarryingSomeoneSkipsCleanup() {
        assertFalse(CarryManager.shouldClearLeftoverSlowdown(true, false));
    }

    @Test
    void joinerBeingCarriedSkipsCleanup() {
        assertFalse(CarryManager.shouldClearLeftoverSlowdown(false, true));
    }

    @Test
    void joinerInBothStatesSkipsCleanup() {
        assertFalse(CarryManager.shouldClearLeftoverSlowdown(true, true));
    }
}
