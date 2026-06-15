package com.nexus.presence;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory record of who is currently connected, by username. A user may hold several
 * sessions at once (multiple tabs/devices); they count as online until the last one drops.
 * Single-instance only — a horizontally scaled deployment would back this with a shared
 * store (e.g. Redis) so presence is consistent across nodes. (Documented tradeoff.)
 */
@Component
public class PresenceRegistry {

    private final ConcurrentHashMap<String, Integer> sessionCounts = new ConcurrentHashMap<>();

    /** Records a new session. Returns true if this brought the user online (their first session). */
    public boolean connected(String username) {
        return sessionCounts.merge(username, 1, Integer::sum) == 1;
    }

    /** Drops a session. Returns true if this took the user offline (their last session). */
    public boolean disconnected(String username) {
        boolean[] wentOffline = {false};
        sessionCounts.compute(username, (key, count) -> {
            if (count == null) return null;
            if (count <= 1) {
                wentOffline[0] = true;
                return null;
            }
            return count - 1;
        });
        return wentOffline[0];
    }

    public boolean isOnline(String username) {
        return sessionCounts.containsKey(username);
    }

    public Set<String> onlineUsers() {
        return Set.copyOf(sessionCounts.keySet());
    }
}
