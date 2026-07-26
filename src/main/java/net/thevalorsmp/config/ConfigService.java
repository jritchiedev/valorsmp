package net.thevalorsmp.config;

import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * Aggregate of every typed config model, injected into services by {@code CompositionRoot} so no
 * service ever reads raw YAML itself (CODING_STANDARDS.md section 11).
 */
public final class ConfigService {

    /** Packaged default config resources materialised on first run (CONFIGURATION.md section 2). */
    public static final List<String> CONFIG_FILES = List.of("config/database.yml");

    private final DatabaseConfig database;

    private ConfigService(DatabaseConfig database) {
        this.database = database;
    }

    /**
     * Loads every config file from the operator-facing data folder.
     *
     * @param bootstrap config bootstrap already pointed at the plugin data folder
     * @param logger    logger used to report substituted defaults
     * @return a fully loaded, validated config aggregate
     */
    public static @NotNull ConfigService load(@NotNull ConfigBootstrap bootstrap, @NotNull Logger logger) {
        Objects.requireNonNull(bootstrap, "bootstrap");
        Objects.requireNonNull(logger, "logger");
        YamlConfiguration databaseYaml = bootstrap.load("config/database.yml");
        ConfigurationSection databaseSection = databaseYaml.getConfigurationSection("database");
        return new ConfigService(DatabaseConfig.load(
                databaseSection == null ? databaseYaml.createSection("database") : databaseSection, logger));
    }

    /**
     * Returns the storage configuration.
     *
     * @return the database config
     */
    public @NotNull DatabaseConfig database() {
        return database;
    }
}
