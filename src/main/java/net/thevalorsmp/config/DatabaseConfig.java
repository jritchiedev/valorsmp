package net.thevalorsmp.config;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * Typed view of {@code config/database.yml} (CONFIGURATION.md section 3, DATABASE.md sections 1 and 2).
 * Not hot-reloadable: a live connection pool cannot be swapped safely.
 *
 * @param backend          storage backend to connect to
 * @param sqliteFileName   SQLite database file name, relative to the plugin data folder
 * @param host             MySQL host, ignored for SQLite
 * @param port             MySQL port, ignored for SQLite
 * @param databaseName     MySQL schema name, ignored for SQLite
 * @param username         MySQL user, ignored for SQLite
 * @param passwordEnvVar   environment variable holding the MySQL password; the password itself is
 *                         never read from YAML (CONFIGURATION.md section 8)
 * @param maximumPoolSize  HikariCP pool size
 */
public record DatabaseConfig(
        @NotNull Backend backend,
        @NotNull String sqliteFileName,
        @NotNull String host,
        int port,
        @NotNull String databaseName,
        @NotNull String username,
        @NotNull String passwordEnvVar,
        int maximumPoolSize) {

    /** Supported storage backends (DATABASE.md section 1). */
    public enum Backend {
        SQLITE,
        MYSQL
    }

    private static final String DEFAULT_SQLITE_FILE = "data.db";
    private static final int DEFAULT_POOL_SIZE = 10;
    private static final int MAX_POOL_SIZE = 50;

    /** Validates record invariants. */
    public DatabaseConfig {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(sqliteFileName, "sqliteFileName");
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(databaseName, "databaseName");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(passwordEnvVar, "passwordEnvVar");
        if (maximumPoolSize < 1 || maximumPoolSize > MAX_POOL_SIZE) {
            throw new IllegalArgumentException("maximumPoolSize must be between 1 and " + MAX_POOL_SIZE);
        }
    }

    /**
     * Reads and validates the database section, substituting documented defaults for invalid values.
     *
     * @param section the {@code database.yml} root section
     * @param logger  logger used to warn about substituted defaults
     * @return a validated config
     */
    public static @NotNull DatabaseConfig load(@NotNull ConfigurationSection section, @NotNull Logger logger) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(logger, "logger");

        Backend backend = parseBackend(section.getString("backend", "sqlite"), logger);
        String sqliteFileName = section.getString("sqlite.file", DEFAULT_SQLITE_FILE);
        if (sqliteFileName == null || sqliteFileName.isBlank()) {
            logger.warn("database.sqlite.file must not be blank; using default {}.", DEFAULT_SQLITE_FILE);
            sqliteFileName = DEFAULT_SQLITE_FILE;
        }

        int poolSize = section.getInt("pool.maximum-pool-size", DEFAULT_POOL_SIZE);
        if (poolSize < 1 || poolSize > MAX_POOL_SIZE) {
            logger.warn(
                    "database.pool.maximum-pool-size must be between 1 and {} (was {}); using default {}.",
                    MAX_POOL_SIZE, poolSize, DEFAULT_POOL_SIZE);
            poolSize = DEFAULT_POOL_SIZE;
        }

        return new DatabaseConfig(
                backend,
                sqliteFileName,
                Objects.requireNonNullElse(section.getString("mysql.host"), "localhost"),
                section.getInt("mysql.port", 3306),
                Objects.requireNonNullElse(section.getString("mysql.database"), "valorsmp"),
                Objects.requireNonNullElse(section.getString("mysql.username"), "valorsmp"),
                Objects.requireNonNullElse(section.getString("mysql.password-env-var"), "VALORSMP_DB_PASSWORD"),
                poolSize);
    }

    /**
     * Resolves the MySQL password from the environment, never from committed YAML.
     *
     * @return the password, or empty when the environment variable is unset or blank
     */
    public @NotNull Optional<String> resolvePassword() {
        String value = System.getenv(passwordEnvVar);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static Backend parseBackend(String raw, Logger logger) {
        String candidate = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        try {
            return Backend.valueOf(candidate);
        } catch (IllegalArgumentException e) {
            logger.warn("database.backend '{}' is not a supported backend; using default sqlite.", raw);
            return Backend.SQLITE;
        }
    }
}
