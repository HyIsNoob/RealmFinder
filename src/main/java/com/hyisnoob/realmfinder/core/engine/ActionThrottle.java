package com.hyisnoob.realmfinder.core.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ActionThrottle {
    private final Map<UUID, Long> lastActionTick = new HashMap<>();

    public boolean allow(UUID playerId, long serverTick, int cooldownTicks) {
        Long last = lastActionTick.get(playerId);
        if (last != null && serverTick - last >= 0 && serverTick - last < cooldownTicks) return false;
        lastActionTick.put(playerId, serverTick);
        return true;
    }

    public void clear(UUID playerId) {
        lastActionTick.remove(playerId);
    }
}
