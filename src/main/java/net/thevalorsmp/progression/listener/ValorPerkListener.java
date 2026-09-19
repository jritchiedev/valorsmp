package net.thevalorsmp.progression.listener;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import java.util.Objects;
import net.thevalorsmp.progression.events.ValorRankChangedEvent;
import net.thevalorsmp.progression.perk.SpeedPreferenceManager;
import net.thevalorsmp.progression.perk.TierPerkService;
import net.thevalorsmp.progression.service.ValorScoreService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps a player's permanent tier perks in sync: applied on join and respawn, refreshed on tier
 * change, and preferences cleaned up on quit (ARCHITECTURE.md section 5).
 */
public final class ValorPerkListener implements Listener {

    private final ValorScoreService valorScoreService;
    private final TierPerkService perkService;
    private final SpeedPreferenceManager speedPreferences;

    /**
     * Creates the listener.
     *
     * @param valorScoreService source of a player's current tier
     * @param perkService       applies the tier's perks
     * @param speedPreferences  per-player Speed preferences, cleared on quit
     */
    public ValorPerkListener(
            @NotNull ValorScoreService valorScoreService,
            @NotNull TierPerkService perkService,
            @NotNull SpeedPreferenceManager speedPreferences) {
        this.valorScoreService = Objects.requireNonNull(valorScoreService, "valorScoreService");
        this.perkService = Objects.requireNonNull(perkService, "perkService");
        this.speedPreferences = Objects.requireNonNull(speedPreferences, "speedPreferences");
    }

    /**
     * Applies perks for the joining player's current tier.
     *
     * @param event the join event
     */
    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        perkService.applyPerks(player, valorScoreService.currentTier(player.getUniqueId()));
    }

    /**
     * Reapplies perks after respawn, since death clears potion effects. Uses the post-respawn
     * event, which fires after the server has finished resetting the respawned player's effects.
     *
     * @param event the post-respawn event
     */
    @EventHandler
    public void onPlayerPostRespawn(@NotNull PlayerPostRespawnEvent event) {
        Player player = event.getPlayer();
        perkService.applyPerks(player, valorScoreService.currentTier(player.getUniqueId()));
    }

    /**
     * Refreshes perks when a player's tier changes.
     *
     * @param event the rank-change event
     */
    @EventHandler
    public void onValorRankChanged(@NotNull ValorRankChangedEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerId());
        if (player != null) {
            perkService.applyPerks(player, event.getNewTier());
        }
    }

    /**
     * Clears the quitting player's session Speed preference.
     *
     * @param event the quit event
     */
    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        speedPreferences.clear(event.getPlayer().getUniqueId());
    }
}
