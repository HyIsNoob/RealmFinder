package com.hyisnoob.realmfinder.test;

import com.hyisnoob.realmfinder.core.engine.GameplayLimits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameplayLimitsTest {
    @Test
    void captureRejectsUnboundedOrNonFiniteParameters() {
        assertTrue(GameplayLimits.validCapture(40, 36, 0, 65, 0, 0, 0, 0, 65, 0));
        assertFalse(GameplayLimits.validCapture(Float.NaN, 36, 0, 65, 0, 0, 0, 0, 65, 0));
        assertFalse(GameplayLimits.validCapture(40, Float.POSITIVE_INFINITY, 0, 65, 0, 0, 0, 0, 65, 0));
        assertFalse(GameplayLimits.validCapture(40, 10000, 0, 65, 0, 0, 0, 0, 65, 0));
        assertFalse(GameplayLimits.validCapture(40, 36, 1000, 65, 0, 0, 0, 0, 65, 0));
    }

    @Test
    void stampRejectsRemoteAndInvalidScale() {
        assertTrue(GameplayLimits.validStamp(1, 0, 65, 0, 0, 0, 0, 65, 0));
        assertFalse(GameplayLimits.validStamp(1, 100, 65, 0, 0, 0, 0, 65, 0));
        assertFalse(GameplayLimits.validStamp(Float.NaN, 0, 65, 0, 0, 0, 0, 65, 0));
        assertFalse(GameplayLimits.validStamp(1.5f, 0, 65, 0, 0, 0, 0, 65, 0));
        assertFalse(GameplayLimits.validStamp(1, Double.NaN, 65, 0, 0, 0, 0, 65, 0));
    }
}
