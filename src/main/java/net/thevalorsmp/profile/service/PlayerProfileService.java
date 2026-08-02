package net.thevalorsmp.profile.service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.thevalorsmp.profile.model.PlayerProfile;
import net.thevalorsmp.profile.repository.PlayerProfileRepository;
import org.jetbrains.annotations.NotNull;

/**
 * Loads and maintains {@link PlayerProfile} records. Every other feature assumes a profile row exists
 * for an online player, so this service records presence on join.
 */
public final class PlayerProfileService {

    private final PlayerProfileRepository repository;

    /**
     * Creates the service.
     *
     * @param repository profile persistence
     */
    public PlayerProfileService(@NotNull PlayerProfileRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Records that a player has joined, creating their profile if it does not yet exist.
     *
     * @param uuid        player UUID
     * @param displayName current display name to cache
     */
    public void recordJoin(@NotNull UUID uuid, @NotNull String displayName) {
        repository.touch(Objects.requireNonNull(uuid, "uuid"), Objects.requireNonNull(displayName, "displayName"));
    }

    /**
     * Finds a player's profile.
     *
     * @param uuid player UUID
     * @return the profile, or empty if the player has never joined
     */
    public @NotNull Optional<PlayerProfile> find(@NotNull UUID uuid) {
        return repository.findByUuid(Objects.requireNonNull(uuid, "uuid"));
    }
}
