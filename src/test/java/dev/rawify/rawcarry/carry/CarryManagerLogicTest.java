package dev.rawify.rawcarry.carry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void singleCarryKeepsBaseSlowdown() {
        assertEquals(1, CarryManager.weightAmplifier(1, 1));
        assertEquals(0, CarryManager.weightAmplifier(1, 0));
    }

    @Test
    void distanceLimitAllowsBoundaryButRejectsBeyond() {
        assertFalse(CarryManager.exceedsMaxDistance(true, 3.0, 3.0));
        assertTrue(CarryManager.exceedsMaxDistance(true, 3.01, 3.0));
        assertTrue(CarryManager.exceedsMaxDistance(false, 0.0, 3.0));
    }

    @Test
    void slowdownGrowsWithPlayersBelowCarrier() {
        assertEquals(2, CarryManager.weightAmplifier(1, 2));
        assertEquals(3, CarryManager.weightAmplifier(1, 3));
        assertEquals(4, CarryManager.weightAmplifier(2, 3));
    }

    @Test
    void refreshClearsOnlyTheSlowdownThisPluginApplied() {
        assertTrue(CarryManager.shouldRemoveWeightSlowdown(1, 1, true));
    }

    @Test
    void refreshKeepsAPotionThePlayerDrankInstead() {
        // A drunk potion has a different amplifier or no carry signature, so it must survive.
        assertFalse(CarryManager.shouldRemoveWeightSlowdown(1, 2, true));
        assertFalse(CarryManager.shouldRemoveWeightSlowdown(1, 1, false));
        assertFalse(CarryManager.shouldRemoveWeightSlowdown(2, 0, false));
    }

    @Test
    void guardPassesWhenEveryPreconditionHolds() {
        assertEquals(CarryManager.CarryBlocker.NONE,
                CarryManager.canCarry(facts(true, true, false, true, true, true, true, true, true)));
    }

    @Test
    void guardReportsEachFailingPrecondition() {
        assertEquals(CarryManager.CarryBlocker.NO_PERMISSION,
                CarryManager.canCarry(facts(false, true, false, true, true, true, true, true, true)));
        assertEquals(CarryManager.CarryBlocker.TARGET_OFFLINE,
                CarryManager.canCarry(facts(true, false, false, true, true, true, true, true, true)));
        assertEquals(CarryManager.CarryBlocker.SELF_CARRY,
                CarryManager.canCarry(facts(true, true, true, true, true, true, true, true, true)));
        assertEquals(CarryManager.CarryBlocker.ALREADY_CARRYING,
                CarryManager.canCarry(facts(true, true, false, false, true, true, true, true, true)));
        assertEquals(CarryManager.CarryBlocker.ALREADY_CARRIED,
                CarryManager.canCarry(facts(true, true, false, true, false, true, true, true, true)));
        assertEquals(CarryManager.CarryBlocker.ON_COOLDOWN,
                CarryManager.canCarry(facts(true, true, false, true, true, false, true, true, true)));
        assertEquals(CarryManager.CarryBlocker.TOO_FAR_AWAY,
                CarryManager.canCarry(facts(true, true, false, true, true, true, false, true, true)));
        assertEquals(CarryManager.CarryBlocker.TOWER_TOO_TALL,
                CarryManager.canCarry(facts(true, true, false, true, true, true, true, false, true)));
        assertEquals(CarryManager.CarryBlocker.TARGET_OPTED_OUT,
                CarryManager.canCarry(facts(true, true, false, true, true, true, true, true, false)));
    }

    @Test
    void guardReportsTheFirstFailingCheckInPlayerFacingOrder() {
        // A carrier who is already busy sees that before a stale cooldown is mentioned.
        assertEquals(CarryManager.CarryBlocker.ALREADY_CARRYING,
                CarryManager.canCarry(facts(true, true, false, false, true, false, false, false, false)));
    }

    @Test
    void everyBlockerCarriesAPlayerFacingMessage() {
        for (CarryManager.CarryBlocker blocker : CarryManager.CarryBlocker.values()) {
            String message = blocker.message("Arda");
            if (blocker == CarryManager.CarryBlocker.NONE) {
                assertTrue(message.isEmpty());
                continue;
            }
            assertFalse(message.isEmpty());
            assertFalse(message.contains("null"));
        }
    }

    private static CarryManager.CarryFacts facts(boolean permitted, boolean targetOnline, boolean samePlayer,
                                                 boolean carrierFree, boolean targetFree, boolean cooledDown,
                                                 boolean inRange, boolean towerFits, boolean targetWilling) {
        return new CarryManager.CarryFacts(permitted, targetOnline, samePlayer, carrierFree, targetFree,
                cooledDown, inRange, towerFits, targetWilling);
    }
}
