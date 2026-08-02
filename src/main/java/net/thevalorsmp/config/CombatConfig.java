package net.thevalorsmp.config;

import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * Typed view of {@code config/combat.yml} (CONFIGURATION.md section 3, docs/combat.md).
 * Hot-reloadable: only tunable constants.
 *
 * @param maceCooldownSeconds       server-enforced mace cooldown, in seconds
 * @param spearLungeCooldownSeconds server-enforced spear "lunge" cooldown, in seconds
 * @param dragonEggStrengthAmplifier strength amplifier granted while a dragon egg is carried
 */
public record CombatConfig(
        int maceCooldownSeconds,
        int spearLungeCooldownSeconds,
        int dragonEggStrengthAmplifier) {

    private static final int DEFAULT_MACE_COOLDOWN = 30;
    private static final int DEFAULT_LUNGE_COOLDOWN = 10;
    private static final int DEFAULT_DRAGON_EGG_AMPLIFIER = 2;
    private static final int MAX_AMPLIFIER = 255;

    /** Validates record invariants. */
    public CombatConfig {
        if (maceCooldownSeconds < 0) {
            throw new IllegalArgumentException("maceCooldownSeconds must be non-negative");
        }
        if (spearLungeCooldownSeconds < 0) {
            throw new IllegalArgumentException("spearLungeCooldownSeconds must be non-negative");
        }
        if (dragonEggStrengthAmplifier < 0 || dragonEggStrengthAmplifier > MAX_AMPLIFIER) {
            throw new IllegalArgumentException("dragonEggStrengthAmplifier must be between 0 and " + MAX_AMPLIFIER);
        }
    }

    /**
     * Reads and validates the combat section, substituting documented defaults for invalid values.
     *
     * @param section the {@code combat.yml} root section
     * @param logger  logger used to warn about substituted defaults
     * @return a validated config
     */
    public static @NotNull CombatConfig load(@NotNull ConfigurationSection section, @NotNull Logger logger) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(logger, "logger");

        int mace = nonNegative(section, "mace-cooldown-seconds", DEFAULT_MACE_COOLDOWN, logger);
        int lunge = nonNegative(section, "spear-lunge-cooldown-seconds", DEFAULT_LUNGE_COOLDOWN, logger);

        int amplifier = section.getInt("dragon-egg-strength-amplifier", DEFAULT_DRAGON_EGG_AMPLIFIER);
        if (amplifier < 0 || amplifier > MAX_AMPLIFIER) {
            logger.warn(
                    "combat.dragon-egg-strength-amplifier must be 0..{} (was {}); using {}.",
                    MAX_AMPLIFIER, amplifier, DEFAULT_DRAGON_EGG_AMPLIFIER);
            amplifier = DEFAULT_DRAGON_EGG_AMPLIFIER;
        }
        return new CombatConfig(mace, lunge, amplifier);
    }

    private static int nonNegative(ConfigurationSection section, String key, int fallback, Logger logger) {
        int value = section.getInt(key, fallback);
        if (value < 0) {
            logger.warn("combat.{} must be non-negative (was {}); using {}.", key, value, fallback);
            return fallback;
        }
        return value;
    }
}
