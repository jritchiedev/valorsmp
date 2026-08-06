package net.thevalorsmp.progression.perk;

import java.util.Objects;
import net.thevalorsmp.config.CombatConfig;
import net.thevalorsmp.progression.model.ValorTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

/**
 * Applies a player's active permanent effects (docs/progression.md, docs/combat.md): infinite Speed
 * and Strength from their Valor tier, plus the dragon-egg Strength buff (whichever Strength amplifier
 * is highest wins). Idempotent — safe to call on join, respawn, tier change, and dragon-egg changes.
 */
public final class TierPerkService {

    private static final int NONE = -1;

    private final SpeedPreferenceManager speedPreferences;
    private final int dragonEggStrengthAmplifier;

    /**
     * Creates the service.
     *
     * @param speedPreferences per-player Speed level preferences for Valor IV/V
     * @param combatConfig     combat config supplying the dragon-egg Strength amplifier
     */
    public TierPerkService(@NotNull SpeedPreferenceManager speedPreferences, @NotNull CombatConfig combatConfig) {
        this.speedPreferences = Objects.requireNonNull(speedPreferences, "speedPreferences");
        this.dragonEggStrengthAmplifier =
                Objects.requireNonNull(combatConfig, "combatConfig").dragonEggStrengthAmplifier();
    }

    /**
     * Reapplies the player's managed effects for their current tier and inventory, clearing any
     * previously managed effects first so a demotion or dropped dragon egg removes what no longer applies.
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

        int strengthAmplifier = tier.hasPermanentStrength() ? tier.strengthAmplifier() : NONE;
        if (carryingDragonEgg(player)) {
            strengthAmplifier = Math.max(strengthAmplifier, dragonEggStrengthAmplifier);
        }
        if (strengthAmplifier >= 0) {
            player.addPotionEffect(infinite(PotionEffectType.STRENGTH, strengthAmplifier));
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

    private static boolean carryingDragonEgg(Player player) {
        return player.getInventory().contains(Material.DRAGON_EGG);
    }

    private static PotionEffect infinite(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, PotionEffect.INFINITE_DURATION, amplifier, false, false, true);
    }
}
