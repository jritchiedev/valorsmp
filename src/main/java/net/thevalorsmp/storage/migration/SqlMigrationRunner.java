package net.thevalorsmp.storage.migration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.sql.DataSource;
import net.thevalorsmp.storage.RepositoryException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * Applies pending {@code V&lt;sequence&gt;__&lt;description&gt;.sql} migrations from the plugin jar,
 * recording applied versions in {@code schema_history} so each runs exactly once
 * (DATABASE.md section 3). Runs during {@code onEnable}, before any repository is constructed.
 */
public final class SqlMigrationRunner {

    /** Migrations shipped with the plugin, in application order (DATABASE.md section 7). */
    public static final List<String> DEFAULT_MIGRATIONS = List.of(
            "V1__init_player_profiles.sql",
            "V2__init_wallets.sql",
            "V3__init_valor_scores.sql");

    private static final String MIGRATION_RESOURCE_PREFIX = "db/migration/";

    private final DataSource dataSource;
    private final Logger logger;

    /**
     * Creates a runner for the given pool.
     *
     * @param dataSource pooled data source to migrate
     * @param logger     plugin logger
     */
    public SqlMigrationRunner(@NotNull DataSource dataSource, @NotNull Logger logger) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Applies every migration not yet recorded in {@code schema_history}.
     *
     * @param migrationFileNames migration file names, relative to {@code db/migration/}, in order
     * @return the migrations applied by this call, in the order they ran
     */
    public @NotNull List<String> migrate(@NotNull List<String> migrationFileNames) {
        Objects.requireNonNull(migrationFileNames, "migrationFileNames");
        List<String> applied = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            SqlDialect dialect = SqlDialect.detect(connection);
            createHistoryTable(connection);
            Set<String> alreadyApplied = readAppliedMigrations(connection);
            for (String fileName : migrationFileNames) {
                if (alreadyApplied.contains(fileName)) {
                    continue;
                }
                applyMigration(connection, dialect, fileName);
                applied.add(fileName);
                logger.info("Applied migration {}.", fileName);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to apply database migrations", e);
        }
        return List.copyOf(applied);
    }

    private void createHistoryTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS schema_history (
                        migration VARCHAR(255) NOT NULL,
                        applied_at TIMESTAMP NOT NULL,
                        PRIMARY KEY (migration)
                    )""");
        }
    }

    private Set<String> readAppliedMigrations(Connection connection) throws SQLException {
        Set<String> applied = new HashSet<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT migration FROM schema_history")) {
            while (rows.next()) {
                applied.add(rows.getString("migration"));
            }
        }
        return applied;
    }

    private void applyMigration(Connection connection, SqlDialect dialect, String fileName) throws SQLException {
        String script = readMigrationScript(fileName);
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                for (String sql : splitStatements(script)) {
                    execute(statement, dialect, sql);
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO schema_history (migration, applied_at) VALUES (?, CURRENT_TIMESTAMP)")) {
                insert.setString(1, fileName);
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            if (!dialect.supportsTransactionalDdl()) {
                logger.error(
                        "{} applies DDL outside transactions, so migration {} may be partially applied; "
                                + "inspect the schema before restarting.",
                        dialect,
                        fileName);
            }
            throw new RepositoryException("Migration " + fileName + " failed and was rolled back", e);
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private void execute(Statement statement, SqlDialect dialect, String sql) throws SQLException {
        try {
            statement.execute(dialect.translate(sql));
        } catch (SQLException e) {
            if (!dialect.isDuplicateObjectError(e)) {
                throw e;
            }
            // Left behind by an earlier migration that failed after this statement on a backend
            // without transactional DDL, so the object exists but was never recorded as applied.
            logger.warn("Skipping already-applied statement: {}", e.getMessage());
        }
    }

    private String readMigrationScript(String fileName) {
        String resourceName = MIGRATION_RESOURCE_PREFIX + fileName;
        try (InputStream in = SqlMigrationRunner.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalStateException("Migration resource is missing: " + resourceName);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read migration " + resourceName, e);
        }
    }

    static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        // Comments are stripped before splitting so a semicolon inside a comment can't split a statement.
        for (String raw : stripComments(script).split(";")) {
            String sql = raw.trim();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
        }
        return statements;
    }

    private static String stripComments(String sql) {
        StringBuilder builder = new StringBuilder();
        for (String line : sql.split("\n")) {
            if (!line.trim().startsWith("--")) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }
}
