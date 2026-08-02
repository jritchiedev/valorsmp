package net.thevalorsmp.progression.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import net.thevalorsmp.storage.RepositoryException;
import org.jetbrains.annotations.NotNull;

/**
 * JDBC-backed {@link ValorScoreRepository} over the {@code valor_scores} table. Portable across
 * SQLite and MariaDB by using an update-then-insert upsert rather than dialect-specific syntax.
 */
public final class SqlValorScoreRepository implements ValorScoreRepository {

    private final DataSource dataSource;

    /**
     * Creates a repository over the shared pool.
     *
     * @param dataSource pooled data source
     */
    public SqlValorScoreRepository(@NotNull DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public int findScore(@NotNull UUID playerId, int season) {
        Objects.requireNonNull(playerId, "playerId");
        String sql = "SELECT score FROM valor_scores WHERE uuid = ? AND season = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setInt(2, season);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getInt("score") : 0;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to load Valor score for " + playerId, e);
        }
    }

    @Override
    public void saveScore(@NotNull UUID playerId, int season, int score) {
        Objects.requireNonNull(playerId, "playerId");
        if (score < 0) {
            throw new IllegalArgumentException("score must be non-negative");
        }
        Timestamp now = Timestamp.from(Instant.now());
        try (Connection connection = dataSource.getConnection()) {
            if (updateExisting(connection, playerId, season, score, now) == 0) {
                insertNew(connection, playerId, season, score, now);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to save Valor score for " + playerId, e);
        }
    }

    private int updateExisting(Connection connection, UUID playerId, int season, int score, Timestamp now)
            throws SQLException {
        String sql = "UPDATE valor_scores SET score = ?, updated_at = ? WHERE uuid = ? AND season = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, score);
            statement.setTimestamp(2, now);
            statement.setString(3, playerId.toString());
            statement.setInt(4, season);
            return statement.executeUpdate();
        }
    }

    private void insertNew(Connection connection, UUID playerId, int season, int score, Timestamp now)
            throws SQLException {
        String sql = "INSERT INTO valor_scores (uuid, season, score, updated_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setInt(2, season);
            statement.setInt(3, score);
            statement.setTimestamp(4, now);
            statement.executeUpdate();
        }
    }
}
