package net.thevalorsmp.combat.cooldown;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * Runtime state (ARCHITECTURE.md section 2.4) tracking per-player, per-ability cooldown expiry for
 * custom item abilities (the mace attack). A {@link Clock} is injected so cooldown
 * logic is unit-testable without real time.
 */
public final class AbilityCooldownManager {

    private final Map<UUID, Map<String, Long>> expiryMillis = new ConcurrentHashMap<>();
    private final Clock clock;

    /**
     * Creates the manager.
     *
     * @param clock time source used to evaluate cooldown expiry
     */
    public AbilityCooldownManager(@NotNull Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Whether the given ability is currently on cooldown for the player.
     *
     * @param playerId player UUID
     * @param ability  ability key, e.g. {@code "mace"}
     * @return {@code true} if the cooldown has not yet elapsed
     */
    public boolean isOnCooldown(@NotNull UUID playerId, @NotNull String ability) {
        Long expiry = abilities(playerId).get(Objects.requireNonNull(ability, "ability"));
        return expiry != null && clock.millis() < expiry;
    }

    /**
     * Whole seconds remaining on a cooldown, rounded up; 0 if not on cooldown.
     *
     * @param playerId player UUID
     * @param ability  ability key
     * @return seconds remaining, never negative
     */
    public long remainingSeconds(@NotNull UUID playerId, @NotNull String ability) {
        Long expiry = abilities(playerId).get(Objects.requireNonNull(ability, "ability"));
        if (expiry == null) {
            return 0L;
        }
        long remaining = expiry - clock.millis();
        return remaining <= 0 ? 0L : (remaining + 999L) / 1000L;
    }

    /**
     * Starts (or restarts) a cooldown for an ability.
     *
     * @param playerId player UUID
     * @param ability  ability key
     * @param duration how long the cooldown lasts
     */
    public void start(@NotNull UUID playerId, @NotNull String ability, @NotNull Duration duration) {
        Objects.requireNonNull(ability, "ability");
        Objects.requireNonNull(duration, "duration");
        abilities(playerId).put(ability, clock.millis() + duration.toMillis());
    }

    /**
     * Clears all cooldowns for a player (e.g. on quit).
     *
     * @param playerId player UUID
     */
    public void clear(@NotNull UUID playerId) {
        expiryMillis.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    private Map<String, Long> abilities(UUID playerId) {
        return expiryMillis.computeIfAbsent(
                Objects.requireNonNull(playerId, "playerId"), key -> new ConcurrentHashMap<>());
    }
}
