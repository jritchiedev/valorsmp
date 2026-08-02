package net.thevalorsmp.profile.repository;

import java.util.Optional;
import java.util.UUID;
import net.thevalorsmp.profile.model.PlayerProfile;
import org.jetbrains.annotations.NotNull;

/**
 * Persistence for {@link PlayerProfile} (REPOSITORIES.md). Owns no business rules.
 */
public interface PlayerProfileRepository {

    /**
     * Finds a player's profile.
     *
     * @param uuid player UUID
     * @return the profile, or empty if the player has never joined
     */
    @NotNull Optional<PlayerProfile> findByUuid(@NotNull UUID uuid);

    /**
     * Records a player's presence: creates the profile on first join, otherwise refreshes the
     * last-seen timestamp and cached display name.
     *
     * @param uuid        player UUID
     * @param displayName current display name to cache
     */
    void touch(@NotNull UUID uuid, @NotNull String displayName);
}
