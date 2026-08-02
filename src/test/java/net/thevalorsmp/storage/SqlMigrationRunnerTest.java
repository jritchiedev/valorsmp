package net.thevalorsmp.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import net.thevalorsmp.storage.migration.SqlMigrationRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.helpers.NOPLogger;

/** Real-SQL test against a temp-file SQLite database (TESTING.md section 4). */
class SqlMigrationRunnerTest {

    @TempDir
    private Path tempDir;

    private Connection connection;
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws SQLException {
        // One shared connection keeps every migration and assertion on the same SQLite file.
        connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db").toAbsolutePath());
        dataSource = SingleConnectionDataSource.wrapping(connection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void migrate_freshDatabase_appliesEveryShippedMigration() {
        List<String> applied = newRunner().migrate(SqlMigrationRunner.DEFAULT_MIGRATIONS);

        assertThat(applied).isEqualTo(SqlMigrationRunner.DEFAULT_MIGRATIONS);
        assertThat(tableNames()).contains("player_profiles", "valor_scores", "schema_history");
    }

    @Test
    void migrate_alreadyMigratedDatabase_isNoOp() {
        newRunner().migrate(SqlMigrationRunner.DEFAULT_MIGRATIONS);

        assertThat(newRunner().migrate(SqlMigrationRunner.DEFAULT_MIGRATIONS)).isEmpty();
    }

    @Test
    void migrate_partiallyMigratedDatabase_appliesOnlyPending() {
        newRunner().migrate(List.of("V1__init_player_profiles.sql"));

        assertThat(newRunner().migrate(SqlMigrationRunner.DEFAULT_MIGRATIONS))
                .containsExactly("V2__init_valor_scores.sql");
    }

    @Test
    void migrate_appliedSchemaSupportsValorScoreRoundTrip() throws SQLException {
        newRunner().migrate(SqlMigrationRunner.DEFAULT_MIGRATIONS);

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO valor_scores (uuid, season, score, updated_at) "
                            + "VALUES ('00000000-0000-0000-0000-000000000001', 1, 12, CURRENT_TIMESTAMP)");
            try (ResultSet rows = statement.executeQuery(
                    "SELECT score FROM valor_scores "
                            + "WHERE uuid = '00000000-0000-0000-0000-000000000001' AND season = 1")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong(1)).isEqualTo(12L);
            }
        }
    }

    @Test
    void migrate_missingMigrationResource_failsFast() {
        SqlMigrationRunner runner = newRunner();

        assertThat(catchThrowable(() -> runner.migrate(List.of("V99__does_not_exist.sql"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("V99__does_not_exist.sql");
    }

    private static Throwable catchThrowable(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable thrown) {
            return thrown;
        }
        throw new AssertionError("Expected an exception to be thrown");
    }

    private SqlMigrationRunner newRunner() {
        return new SqlMigrationRunner(dataSource, NOPLogger.NOP_LOGGER);
    }

    private List<String> tableNames() {
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'")) {
            List<String> names = new java.util.ArrayList<>();
            while (rows.next()) {
                names.add(rows.getString("name"));
            }
            return names;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list tables", e);
        }
    }
}
