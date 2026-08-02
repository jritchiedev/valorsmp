package net.thevalorsmp.config;

import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * Typed view of {@code config/progression.yml} (CONFIGURATION.md section 3, docs/progression.md).
 * Hot-reloadable: only tunable constants, no live resources.
 *
 * @param currentSeason         season Valor scores are currently keyed by
 * @param tierThresholds        minimum Valor Points for tiers I..V, ascending and non-negative
 * @param extendedPotionEffects whether Valor II+ extends potion effect durations
 */
public record ProgressionConfig(
        int currentSeason,
        @NotNull List<Integer> tierThresholds,
        boolean extendedPotionEffects) {

    /** Number of Valor tiers (I..V). */
    public static final int TIER_COUNT = 5;

    private static final List<Integer> DEFAULT_THRESHOLDS = List.of(0, 5, 9, 13, 17);
    private static final int DEFAULT_SEASON = 1;

    /** Validates record invariants and defensively copies the thresholds. */
    public ProgressionConfig {
        Objects.requireNonNull(tierThresholds, "tierThresholds");
        tierThresholds = List.copyOf(tierThresholds);
        if (currentSeason < 1) {
            throw new IllegalArgumentException("currentSeason must be at least 1");
        }
        if (tierThresholds.size() != TIER_COUNT) {
            throw new IllegalArgumentException("tierThresholds must contain exactly " + TIER_COUNT + " values");
        }
        int previous = Integer.MIN_VALUE;
        for (int threshold : tierThresholds) {
            if (threshold < 0) {
                throw new IllegalArgumentException("tierThresholds must be non-negative");
            }
            if (threshold <= previous) {
                throw new IllegalArgumentException("tierThresholds must be strictly ascending");
            }
            previous = threshold;
        }
    }

    /**
     * Reads and validates the progression section, substituting documented defaults for invalid values.
     *
     * @param section the {@code progression.yml} root section
     * @param logger  logger used to warn about substituted defaults
     * @return a validated config
     */
    public static @NotNull ProgressionConfig load(@NotNull ConfigurationSection section, @NotNull Logger logger) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(logger, "logger");

        int season = section.getInt("current-season", DEFAULT_SEASON);
        if (season < 1) {
            logger.warn("progression.current-season must be at least 1 (was {}); using {}.", season, DEFAULT_SEASON);
            season = DEFAULT_SEASON;
        }

        List<Integer> thresholds = section.getIntegerList("tier-thresholds");
        if (!isValidThresholds(thresholds)) {
            logger.warn(
                    "progression.tier-thresholds must be {} ascending non-negative values (was {}); using {}.",
                    TIER_COUNT, thresholds, DEFAULT_THRESHOLDS);
            thresholds = DEFAULT_THRESHOLDS;
        }

        boolean extended = section.getBoolean("extended-potion-effects", true);
        return new ProgressionConfig(season, thresholds, extended);
    }

    private static boolean isValidThresholds(List<Integer> thresholds) {
        if (thresholds == null || thresholds.size() != TIER_COUNT) {
            return false;
        }
        int previous = Integer.MIN_VALUE;
        for (Integer threshold : thresholds) {
            if (threshold == null || threshold < 0 || threshold <= previous) {
                return false;
            }
            previous = threshold;
        }
        return true;
    }
}
