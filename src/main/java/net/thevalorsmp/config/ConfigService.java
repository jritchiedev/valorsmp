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
    public static final List<String> CONFIG_FILES =
            List.of("config/database.yml", "config/combat.yml", "config/progression.yml");

    private final DatabaseConfig database;
    private final CombatConfig combat;
    private final ProgressionConfig progression;

    private ConfigService(DatabaseConfig database, CombatConfig combat, ProgressionConfig progression) {
        this.database = database;
        this.combat = combat;
        this.progression = progression;
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
        DatabaseConfig database =
                DatabaseConfig.load(section(bootstrap, "config/database.yml", "database"), logger);
        CombatConfig combat =
                CombatConfig.load(section(bootstrap, "config/combat.yml", "combat"), logger);
        ProgressionConfig progression =
                ProgressionConfig.load(section(bootstrap, "config/progression.yml", "progression"), logger);
        return new ConfigService(database, combat, progression);
    }

    /**
     * Returns the storage configuration.
     *
     * @return the database config
     */
    public @NotNull DatabaseConfig database() {
        return database;
    }

    /**
     * Returns the combat and custom-item configuration.
     *
     * @return the combat config
     */
    public @NotNull CombatConfig combat() {
        return combat;
    }

    /**
     * Returns the Valor progression configuration.
     *
     * @return the progression config
     */
    public @NotNull ProgressionConfig progression() {
        return progression;
    }

    private static ConfigurationSection section(ConfigBootstrap bootstrap, String resourceName, String rootKey) {
        YamlConfiguration yaml = bootstrap.load(resourceName);
        ConfigurationSection section = yaml.getConfigurationSection(rootKey);
        return section == null ? yaml.createSection(rootKey) : section;
    }
}
