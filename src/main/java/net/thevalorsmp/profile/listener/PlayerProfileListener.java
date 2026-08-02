package net.thevalorsmp.profile.listener;

import java.util.Objects;
import net.thevalorsmp.profile.service.PlayerProfileService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit adapter that records player presence on join (ARCHITECTURE.md section 5).
 */
public final class PlayerProfileListener implements Listener {

    private final PlayerProfileService profileService;

    /**
     * Creates the listener.
     *
     * @param profileService profile service to notify on join
     */
    public PlayerProfileListener(@NotNull PlayerProfileService profileService) {
        this.profileService = Objects.requireNonNull(profileService, "profileService");
    }

    /**
     * Records the joining player's profile before other features read it.
     *
     * @param event the join event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        profileService.recordJoin(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }
}
