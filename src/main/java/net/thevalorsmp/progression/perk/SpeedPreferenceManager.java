package net.thevalorsmp.progression.perk;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime state (ARCHITECTURE.md section 2.4) tracking each Valor IV/V player's chosen permanent
 * Speed level, set via {@code /speed}. Session-scoped for alpha: preferences reset to the tier
 * default on server restart.
 */
public final class SpeedPreferenceManager {

    private final Map<UUID, Integer> amplifiers = new ConcurrentHashMap<>();

    /**
     * Records a player's chosen Speed amplifier.
     *
     * @param playerId  player UUID
     * @param amplifier {@code 0} for Speed I, {@code 1} for Speed II
     */
    public void setAmplifier(UUID playerId, int amplifier) {
        amplifiers.put(Objects.requireNonNull(playerId, "playerId"), amplifier);
    }

    /**
     * Returns a player's chosen Speed amplifier, falling back to the tier default when unset.
     *
     * @param playerId       player UUID
     * @param defaultAmplifier the tier's default amplifier
     * @return the effective amplifier
     */
    public int getAmplifier(UUID playerId, int defaultAmplifier) {
        return amplifiers.getOrDefault(Objects.requireNonNull(playerId, "playerId"), defaultAmplifier);
    }

    /**
     * Clears a player's stored preference (e.g. on quit).
     *
     * @param playerId player UUID
     */
    public void clear(UUID playerId) {
        amplifiers.remove(Objects.requireNonNull(playerId, "playerId"));
    }
}
