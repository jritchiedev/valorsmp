package net.thevalorsmp.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * Typed view of {@code config/progression.yml} (CONFIGURATION.md section 3, docs/progression.md).
 * Hot-reloadable: only tunable constants, no live resources.
 *
 * @param currentSeason             season Valor scores are currently keyed by
 * @param tierThresholds            minimum Valor Points for tiers I..V, ascending and non-negative
 * @param maxValor                  maximum Valor score; mutations clamp at this value and it must be
 *                                  at least the top tier threshold so Valor V stays reachable
 * @param extendedPotionEffects     whether Valor II+ extends potion effect durations
 * @param potionDurationExtensions  mapping of source potion duration (ticks) to extended duration
 *                                  (ticks), applied for Valor II+ players
 */
public record ProgressionConfig(
        int currentSeason,
        @NotNull List<Integer> tierThresholds,
        int maxValor,
        boolean extendedPotionEffects,
        @NotNull Map<Integer, Integer> potionDurationExtensions) {

    /** Number of Valor tiers (I..V). */
    public static final int TIER_COUNT = 5;

    private static final List<Integer> DEFAULT_THRESHOLDS = List.of(0, 5, 9, 13, 17);
    private static final int DEFAULT_MAX_VALOR = 20;
    // 8:00 (9600t) -> 10:00 (12000t) and 1:30 (1800t) -> 3:00 (3600t), per docs/progression.md.
    private static final Map<Integer, Integer> DEFAULT_POTION_EXTENSIONS = Map.of(9600, 12000, 1800, 3600);
    private static final int DEFAULT_SEASON = 1;

    /** Validates record invariants and defensively copies collections. */
    public ProgressionConfig {
        Objects.requireNonNull(tierThresholds, "tierThresholds");
        Objects.requireNonNull(potionDurationExtensions, "potionDurationExtensions");
        tierThresholds = List.copyOf(tierThresholds);
        potionDurationExtensions = Map.copyOf(potionDurationExtensions);
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
        if (maxValor < tierThresholds.get(TIER_COUNT - 1)) {
            throw new IllegalArgumentException("maxValor must be at least the top tier threshold");
        }
        for (Map.Entry<Integer, Integer> entry : potionDurationExtensions.entrySet()) {
            if (entry.getKey() <= 0 || entry.getValue() <= 0) {
                throw new IllegalArgumentException("potionDurationExtensions durations must be positive");
            }
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

        int maxValor = section.getInt("max-valor", DEFAULT_MAX_VALOR);
        int topThreshold = thresholds.get(TIER_COUNT - 1);
        if (maxValor < topThreshold) {
            int fallback = Math.max(DEFAULT_MAX_VALOR, topThreshold);
            logger.warn(
                    "progression.max-valor must be at least the top tier threshold {} (was {}); using {}.",
                    topThreshold, maxValor, fallback);
            maxValor = fallback;
        }

        boolean extended = section.getBoolean("extended-potion-effects", true);
        Map<Integer, Integer> extensions =
                loadExtensions(section.getConfigurationSection("potion-duration-extensions"), logger);
        return new ProgressionConfig(season, thresholds, maxValor, extended, extensions);
    }

    private static Map<Integer, Integer> loadExtensions(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return DEFAULT_POTION_EXTENSIONS;
        }
        Map<Integer, Integer> result = new HashMap<>();
        for (String key : section.getKeys(false)) {
            int source;
            try {
                source = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                logger.warn("progression.potion-duration-extensions key '{}' is not an integer; ignoring.", key);
                continue;
            }
            int target = section.getInt(key);
            if (source > 0 && target > 0) {
                result.put(source, target);
            } else {
                logger.warn("progression.potion-duration-extensions.{} must map positive->positive; ignoring.", key);
            }
        }
        return result.isEmpty() ? DEFAULT_POTION_EXTENSIONS : Map.copyOf(result);
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
