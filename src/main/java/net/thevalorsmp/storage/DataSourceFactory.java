package net.thevalorsmp.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.sql.DataSource;
import net.thevalorsmp.config.DatabaseConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * Creates and owns the single pooled {@link DataSource} shared by every repository
 * (DATABASE.md section 2). Closed last during {@code onDisable}, after services have flushed.
 */
public final class DataSourceFactory implements AutoCloseable {

    private final Path dataFolder;
    private final Logger logger;
    private HikariDataSource dataSource;

    /**
     * Creates a factory rooted at the plugin data folder (where the SQLite file lives).
     *
     * @param dataFolder plugin data folder
     * @param logger     plugin logger
     */
    public DataSourceFactory(@NotNull Path dataFolder, @NotNull Logger logger) {
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Opens the connection pool for the configured backend.
     *
     * @param config validated database config
     * @return the pooled data source
     */
    public @NotNull DataSource create(@NotNull DatabaseConfig config) {
        Objects.requireNonNull(config, "config");
        if (dataSource != null) {
            throw new IllegalStateException("Data source has already been created");
        }
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("valorsmp-" + config.backend().name().toLowerCase(java.util.Locale.ROOT));
        hikariConfig.setMaximumPoolSize(config.maximumPoolSize());
        switch (config.backend()) {
            case SQLITE -> configureSqlite(hikariConfig, config);
            case MYSQL -> configureMysql(hikariConfig, config);
            default -> throw new IllegalStateException("Unhandled backend " + config.backend());
        }
        logger.info("Opening {} connection pool (max {} connections).", config.backend(), config.maximumPoolSize());
        dataSource = new HikariDataSource(hikariConfig);
        return dataSource;
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private void configureSqlite(HikariConfig hikariConfig, DatabaseConfig config) {
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create plugin data folder " + dataFolder, e);
        }
        Path databaseFile = dataFolder.resolve(config.sqliteFileName());
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath());
        // SQLite tolerates exactly one writer; a larger pool only produces SQLITE_BUSY contention.
        hikariConfig.setMaximumPoolSize(1);
    }

    private void configureMysql(HikariConfig hikariConfig, DatabaseConfig config) {
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.host() + ":" + config.port() + "/" + config.databaseName());
        hikariConfig.setUsername(config.username());
        config.resolvePassword().ifPresentOrElse(
                hikariConfig::setPassword,
                () -> logger.warn(
                        "Environment variable {} is unset; connecting to MySQL without a password.",
                        config.passwordEnvVar()));
    }
}
