package net.thevalorsmp.profile.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A player's persistent profile row (DATABASE.md {@code player_profiles}).
 *
 * @param uuid             player UUID
 * @param firstJoinedAt    when the player first joined the server
 * @param lastSeenAt       when the player was last seen
 * @param displayNameCache cached display name for offline lookups; not authoritative
 */
public record PlayerProfile(
        @NotNull UUID uuid,
        @NotNull Instant firstJoinedAt,
        @NotNull Instant lastSeenAt,
        @Nullable String displayNameCache) {

    /** Validates record invariants. */
    public PlayerProfile {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(firstJoinedAt, "firstJoinedAt");
        Objects.requireNonNull(lastSeenAt, "lastSeenAt");
    }
}
