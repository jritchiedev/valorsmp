package net.thevalorsmp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

class DatabaseConfigTest {

    @Test
    void load_fullyPopulatedSection_readsEveryValue() {
        ConfigurationSection section = sectionOf("""
                backend: mysql
                sqlite:
                  file: valor.db
                mysql:
                  host: db.internal
                  port: 3307
                  database: valor
                  username: valor_app
                  password-env-var: CUSTOM_DB_PASSWORD
                pool:
                  maximum-pool-size: 4
                """);

        DatabaseConfig config = DatabaseConfig.load(section, NOPLogger.NOP_LOGGER);

        assertThat(config.backend()).isEqualTo(DatabaseConfig.Backend.MYSQL);
        assertThat(config.sqliteFileName()).isEqualTo("valor.db");
        assertThat(config.host()).isEqualTo("db.internal");
        assertThat(config.port()).isEqualTo(3307);
        assertThat(config.databaseName()).isEqualTo("valor");
        assertThat(config.username()).isEqualTo("valor_app");
        assertThat(config.passwordEnvVar()).isEqualTo("CUSTOM_DB_PASSWORD");
        assertThat(config.maximumPoolSize()).isEqualTo(4);
    }

    @Test
    void load_emptySection_usesDocumentedDefaults() {
        DatabaseConfig config = DatabaseConfig.load(sectionOf(""), NOPLogger.NOP_LOGGER);

        assertThat(config.backend()).isEqualTo(DatabaseConfig.Backend.SQLITE);
        assertThat(config.sqliteFileName()).isEqualTo("data.db");
        assertThat(config.maximumPoolSize()).isEqualTo(10);
        assertThat(config.passwordEnvVar()).isEqualTo("VALORSMP_DB_PASSWORD");
    }

    @Test
    void load_unknownBackend_fallsBackToSqlite() {
        DatabaseConfig config = DatabaseConfig.load(sectionOf("backend: postgres"), NOPLogger.NOP_LOGGER);

        assertThat(config.backend()).isEqualTo(DatabaseConfig.Backend.SQLITE);
    }

    @Test
    void load_poolSizeOutOfRange_fallsBackToDefault() {
        assertThat(DatabaseConfig.load(sectionOf("pool:\n  maximum-pool-size: 0"), NOPLogger.NOP_LOGGER)
                .maximumPoolSize()).isEqualTo(10);
        assertThat(DatabaseConfig.load(sectionOf("pool:\n  maximum-pool-size: 999"), NOPLogger.NOP_LOGGER)
                .maximumPoolSize()).isEqualTo(10);
    }

    @Test
    void load_blankSqliteFileName_fallsBackToDefault() {
        DatabaseConfig config = DatabaseConfig.load(sectionOf("sqlite:\n  file: '  '"), NOPLogger.NOP_LOGGER);

        assertThat(config.sqliteFileName()).isEqualTo("data.db");
    }

    @Test
    void constructor_poolSizeOutOfRange_rejected() {
        assertThatThrownBy(() -> new DatabaseConfig(
                DatabaseConfig.Backend.SQLITE, "data.db", "localhost", 3306, "valorsmp", "valorsmp", "ENV", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolvePassword_environmentVariableUnset_returnsEmpty() {
        DatabaseConfig config = DatabaseConfig.load(
                sectionOf("mysql:\n  password-env-var: VALORSMP_DB_PASSWORD_DEFINITELY_UNSET"),
                NOPLogger.NOP_LOGGER);

        assertThat(config.resolvePassword()).isEmpty();
    }

    private static ConfigurationSection sectionOf(String yaml) {
        YamlConfiguration root = new YamlConfiguration();
        ConfigurationSection section = root.createSection("database");
        YamlConfiguration parsed = new YamlConfiguration();
        try {
            parsed.loadFromString(yaml);
        } catch (org.bukkit.configuration.InvalidConfigurationException e) {
            throw new IllegalArgumentException("test yaml is invalid", e);
        }
        for (String key : parsed.getKeys(true)) {
            section.set(key, parsed.get(key));
        }
        return section;
    }
}
