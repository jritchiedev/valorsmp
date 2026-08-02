package net.thevalorsmp.profile.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import net.thevalorsmp.profile.model.PlayerProfile;
import net.thevalorsmp.storage.RepositoryException;
import org.jetbrains.annotations.NotNull;

/**
 * JDBC-backed {@link PlayerProfileRepository} over the {@code player_profiles} table. Portable
 * across SQLite and MariaDB by using an update-then-insert upsert rather than dialect-specific syntax.
 */
public final class SqlPlayerProfileRepository implements PlayerProfileRepository {

    private final DataSource dataSource;

    /**
     * Creates a repository over the shared pool.
     *
     * @param dataSource pooled data source
     */
    public SqlPlayerProfileRepository(@NotNull DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public @NotNull Optional<PlayerProfile> findByUuid(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        String sql = "SELECT uuid, first_joined_at, last_seen_at, display_name_cache "
                + "FROM player_profiles WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }
                return Optional.of(new PlayerProfile(
                        UUID.fromString(rows.getString("uuid")),
                        rows.getTimestamp("first_joined_at").toInstant(),
                        rows.getTimestamp("last_seen_at").toInstant(),
                        rows.getString("display_name_cache")));
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to load player profile " + uuid, e);
        }
    }

    @Override
    public void touch(@NotNull UUID uuid, @NotNull String displayName) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(displayName, "displayName");
        Timestamp now = Timestamp.from(Instant.now());
        try (Connection connection = dataSource.getConnection()) {
            if (updateExisting(connection, uuid, displayName, now) == 0) {
                insertNew(connection, uuid, displayName, now);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to record player profile " + uuid, e);
        }
    }

    private int updateExisting(Connection connection, UUID uuid, String displayName, Timestamp now)
            throws SQLException {
        String sql = "UPDATE player_profiles SET last_seen_at = ?, display_name_cache = ? WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, now);
            statement.setString(2, displayName);
            statement.setString(3, uuid.toString());
            return statement.executeUpdate();
        }
    }

    private void insertNew(Connection connection, UUID uuid, String displayName, Timestamp now)
            throws SQLException {
        String sql = "INSERT INTO player_profiles (uuid, first_joined_at, last_seen_at, display_name_cache) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setTimestamp(2, now);
            statement.setTimestamp(3, now);
            statement.setString(4, displayName);
            statement.executeUpdate();
        }
    }
}
