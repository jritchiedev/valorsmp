package net.thevalorsmp.progression.perk;

import java.util.Objects;
import net.thevalorsmp.progression.model.ValorTier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

/**
 * Applies a Valor tier's permanent perks to a player (docs/progression.md): infinite Speed and
 * Strength effects for the relevant tiers. Idempotent — safe to call on join, respawn, and tier
 * change. The extended-potion perk (Valor II+) is handled separately by a dedicated listener.
 */
public final class TierPerkService {

    private final SpeedPreferenceManager speedPreferences;

    /**
     * Creates the service.
     *
     * @param speedPreferences per-player Speed level preferences for Valor IV/V
     */
    public TierPerkService(@NotNull SpeedPreferenceManager speedPreferences) {
        this.speedPreferences = Objects.requireNonNull(speedPreferences, "speedPreferences");
    }

    /**
     * Reapplies the permanent perks for a player's current tier, clearing any previously managed
     * effects first so a demotion removes the effect a lower tier no longer grants.
     *
     * @param player the online player
     * @param tier   the player's current Valor tier
     */
    public void applyPerks(@NotNull Player player, @NotNull ValorTier tier) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(tier, "tier");
        clearManagedEffects(player);

        if (tier.hasPermanentSpeed()) {
            int amplifier = tier.isSpeedSwitchable()
                    ? speedPreferences.getAmplifier(player.getUniqueId(), tier.defaultSpeedAmplifier())
                    : tier.defaultSpeedAmplifier();
            player.addPotionEffect(infinite(PotionEffectType.SPEED, amplifier));
        }
        if (tier.hasPermanentStrength()) {
            player.addPotionEffect(infinite(PotionEffectType.STRENGTH, tier.strengthAmplifier()));
        }
    }

    /**
     * Removes the perk effects this service manages (Speed and Strength).
     *
     * @param player the player to clear
     */
    public void clearManagedEffects(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.STRENGTH);
    }

    private static PotionEffect infinite(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, PotionEffect.INFINITE_DURATION, amplifier, false, false, true);
    }
}
