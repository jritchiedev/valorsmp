package net.thevalorsmp.storage.migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.thevalorsmp.storage.RepositoryException;
import org.jetbrains.annotations.NotNull;

/**
 * Isolates backend-specific SQL syntax so migration scripts stay written once, in portable form
 * (DATABASE.md section 1). Migrations are authored in SQLite syntax; each dialect rewrites the
 * constructs its backend rejects rather than shipping one script per backend.
 */
public enum SqlDialect {

    /** SQLite: the authoring dialect, so migration scripts run verbatim. */
    SQLITE {
        @Override
        String translate(String statement) {
            return statement;
        }

        @Override
        boolean supportsTransactionalDdl() {
            return true;
        }

        @Override
        boolean isDuplicateObjectError(SQLException e) {
            return false;
        }
    },

    /** MySQL and MariaDB, which share a wire protocol and the same DDL limitations here. */
    MYSQL {
        @Override
        String translate(String statement) {
            // MySQL has no IF NOT EXISTS on CREATE INDEX (it does on CREATE TABLE); re-runs are
            // instead made safe by treating a duplicate index name as already-applied.
            Matcher matcher = CREATE_INDEX_IF_NOT_EXISTS.matcher(statement);
            return matcher.find() ? matcher.replaceFirst(matcher.group(1) + " ") : statement;
        }

        @Override
        boolean supportsTransactionalDdl() {
            return false;
        }

        @Override
        boolean isDuplicateObjectError(SQLException e) {
            return e.getErrorCode() == MYSQL_ERROR_DUPLICATE_KEY_NAME;
        }
    };

    private static final Pattern CREATE_INDEX_IF_NOT_EXISTS = Pattern.compile(
            "^(CREATE\\s+(?:UNIQUE\\s+)?INDEX)\\s+IF\\s+NOT\\s+EXISTS\\s+",
            Pattern.CASE_INSENSITIVE);

    /** MySQL {@code ER_DUP_KEYNAME}: the index already exists. */
    private static final int MYSQL_ERROR_DUPLICATE_KEY_NAME = 1061;

    /**
     * Determines the dialect of an open connection from its JDBC metadata.
     *
     * @param connection open connection
     * @return the matching dialect
     * @throws SQLException if the metadata cannot be read
     */
    static @NotNull SqlDialect detect(@NotNull Connection connection) throws SQLException {
        return forProductName(connection.getMetaData().getDatabaseProductName());
    }

    /**
     * Maps a JDBC database product name onto a supported dialect.
     *
     * @param productName value of {@code DatabaseMetaData#getDatabaseProductName}
     * @return the matching dialect
     */
    static @NotNull SqlDialect forProductName(@NotNull String productName) {
        String normalized = productName.toLowerCase(Locale.ROOT);
        if (normalized.contains("sqlite")) {
            return SQLITE;
        }
        if (normalized.contains("mysql") || normalized.contains("mariadb")) {
            return MYSQL;
        }
        throw new RepositoryException("No migration dialect for database product '" + productName + "'");
    }

    /**
     * Rewrites a single statement into syntax this backend accepts.
     *
     * @param statement statement as authored, in SQLite syntax
     * @return the statement to execute
     */
    abstract String translate(String statement);

    /** @return whether a failed migration's DDL is rolled back, i.e. whether DDL is transactional. */
    abstract boolean supportsTransactionalDdl();

    /**
     * Reports whether a failure means the object being created already exists, which happens when a
     * non-transactional backend re-runs a migration that previously failed part-way through.
     *
     * @param e failure raised while executing a migration statement
     * @return whether the statement can be treated as already applied
     */
    abstract boolean isDuplicateObjectError(SQLException e);
}
