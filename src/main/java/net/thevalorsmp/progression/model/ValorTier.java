package net.thevalorsmp.progression.model;

import java.util.List;
import java.util.Objects;

/**
 * The Valor tier ladder (docs/progression.md). Each tier carries its perk metadata; the point
 * thresholds that map a score to a tier are supplied from config, keeping them tunable.
 *
 * <p>Amplifier convention matches Minecraft: amplifier {@code 0} is level I, {@code 1} is level II.
 * A negative amplifier means the tier grants no permanent effect of that kind.
 */
public enum ValorTier {

    /** Valor I: no perks. */
    I(-1, -1, false),
    /** Valor II: extended potion effects only. */
    II(-1, -1, false),
    /** Valor III: permanent Speed I. */
    III(0, -1, false),
    /** Valor IV: permanent Strength I and Speed II (speed switchable). */
    IV(1, 0, true),
    /** Valor V: permanent Strength II and Speed II (speed switchable). */
    V(1, 1, true);

    private static final int NONE = -1;

    private final int defaultSpeedAmplifier;
    private final int strengthAmplifier;
    private final boolean speedSwitchable;

    ValorTier(int defaultSpeedAmplifier, int strengthAmplifier, boolean speedSwitchable) {
        this.defaultSpeedAmplifier = defaultSpeedAmplifier;
        this.strengthAmplifier = strengthAmplifier;
        this.speedSwitchable = speedSwitchable;
    }

    /**
     * Resolves the tier for a Valor score against the configured thresholds.
     *
     * @param score      the player's current Valor score (never negative)
     * @param thresholds minimum points for tiers I..V, ascending; must have one entry per tier
     * @return the highest tier whose threshold the score meets
     */
    public static ValorTier fromScore(int score, List<Integer> thresholds) {
        Objects.requireNonNull(thresholds, "thresholds");
        ValorTier[] tiers = values();
        ValorTier result = tiers[0];
        for (int i = 0; i < tiers.length && i < thresholds.size(); i++) {
            if (score >= thresholds.get(i)) {
                result = tiers[i];
            }
        }
        return result;
    }

    /**
     * The tier's display number, 1..5.
     *
     * @return the 1-based tier number
     */
    public int displayNumber() {
        return ordinal() + 1;
    }

    /**
     * The player-facing display name, e.g. {@code "Valor III"}.
     *
     * @return the display name
     */
    public String displayName() {
        return "Valor " + name();
    }

    /**
     * Whether this tier grants a permanent Speed effect.
     *
     * @return {@code true} if a permanent Speed effect applies
     */
    public boolean hasPermanentSpeed() {
        return defaultSpeedAmplifier != NONE;
    }

    /**
     * The default Speed amplifier for this tier ({@code 0} = Speed I, {@code 1} = Speed II).
     *
     * @return the default speed amplifier, or a negative value if none
     */
    public int defaultSpeedAmplifier() {
        return defaultSpeedAmplifier;
    }

    /**
     * Whether a player at this tier may switch their permanent Speed level via {@code /speed}.
     *
     * @return {@code true} for tiers IV and V
     */
    public boolean isSpeedSwitchable() {
        return speedSwitchable;
    }

    /**
     * Whether this tier grants a permanent Strength effect.
     *
     * @return {@code true} if a permanent Strength effect applies
     */
    public boolean hasPermanentStrength() {
        return strengthAmplifier != NONE;
    }

    /**
     * The Strength amplifier for this tier ({@code 0} = Strength I, {@code 1} = Strength II).
     *
     * @return the strength amplifier, or a negative value if none
     */
    public int strengthAmplifier() {
        return strengthAmplifier;
    }

    /**
     * Whether this tier extends potion effect durations (Valor II and above).
     *
     * @return {@code true} for every tier except Valor I
     */
    public boolean extendsPotionEffects() {
        return this != I;
    }
}
