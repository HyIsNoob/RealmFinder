package com.hyisnoob.realmfinder.test;

import com.hyisnoob.realmfinder.core.engine.ActionThrottle;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActionThrottleTest {
    @Test
    void limitsRepeatedPacketsWithoutBlockingOtherPlayers() {
        ActionThrottle throttle = new ActionThrottle();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertTrue(throttle.allow(first, 100, 20));
        assertFalse(throttle.allow(first, 101, 20));
        assertTrue(throttle.allow(second, 101, 20));
        assertTrue(throttle.allow(first, 120, 20));
    }
}
