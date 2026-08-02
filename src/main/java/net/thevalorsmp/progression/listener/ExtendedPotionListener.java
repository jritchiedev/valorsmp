package net.thevalorsmp.progression.listener;

import java.util.Map;
import java.util.Objects;
import net.thevalorsmp.config.ProgressionConfig;
import net.thevalorsmp.progression.service.ValorScoreService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

/**
 * Extends potion effect durations for Valor II+ players (docs/progression.md): a configurable set of
 * source durations is rewritten to a longer duration when such an effect is applied.
 *
 * <p>The (cheap) duration lookup runs before the tier check, so high-frequency effect sources whose
 * durations never match a mapping (beacons, lingering clouds) short-circuit without a score lookup.
 */
public final class ExtendedPotionListener implements Listener {

    private final ValorScoreService valorScoreService;
    private final boolean enabled;
    private final Map<Integer, Integer> extensions;

    /**
     * Creates the listener.
     *
     * @param valorScoreService source of a player's current tier
     * @param config            progression config supplying the extension mapping
     */
    public ExtendedPotionListener(@NotNull ValorScoreService valorScoreService, @NotNull ProgressionConfig config) {
        this.valorScoreService = Objects.requireNonNull(valorScoreService, "valorScoreService");
        Objects.requireNonNull(config, "config");
        this.enabled = config.extendedPotionEffects();
        this.extensions = config.potionDurationExtensions();
    }

    /**
     * Rewrites the duration of a newly applied potion effect when it matches a configured mapping and
     * the affected player is Valor II or higher.
     *
     * @param event the potion effect event
     */
    @EventHandler(ignoreCancelled = true)
    public void onPotionEffect(@NotNull EntityPotionEffectEvent event) {
        if (!enabled || event.getCause() == EntityPotionEffectEvent.Cause.PLUGIN) {
            return;
        }
        EntityPotionEffectEvent.Action action = event.getAction();
        if (action != EntityPotionEffectEvent.Action.ADDED && action != EntityPotionEffectEvent.Action.CHANGED) {
            return;
        }
        PotionEffect applied = event.getNewEffect();
        if (applied == null || !(event.getEntity() instanceof Player player)) {
            return;
        }
        Integer extended = extensions.get(applied.getDuration());
        if (extended == null || extended == applied.getDuration()) {
            return;
        }
        if (!valorScoreService.currentTier(player.getUniqueId()).extendsPotionEffects()) {
            return;
        }
        event.setCancelled(true);
        player.addPotionEffect(new PotionEffect(
                applied.getType(), extended, applied.getAmplifier(),
                applied.isAmbient(), applied.hasParticles(), applied.hasIcon()));
    }
}
