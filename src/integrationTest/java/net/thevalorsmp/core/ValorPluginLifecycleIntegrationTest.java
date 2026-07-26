package net.thevalorsmp.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import net.thevalorsmp.config.DatabaseConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Proves the real plugin lifecycle wiring works end to end under MockBukkit: config bootstrap,
 * SQLite pool creation, migrations, and clean disable (TESTING.md section 3).
 */
class ValorPluginLifecycleIntegrationTest {

    private ServerMock server;
    private ValorPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(ValorPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onEnable_freshServer_enablesPluginCleanly() {
        assertThat(plugin.isEnabled()).isTrue();
        assertThat(server.getPluginManager().isPluginEnabled("TheValorSMP")).isTrue();
    }

    @Test
    void onEnable_freshServer_generatesOperatorConfigAndDefaultsToSqlite() {
        Path dataFolder = plugin.getDataFolder().toPath();

        assertThat(dataFolder.resolve("config/database.yml")).exists();
        assertThat(plugin.getConfigService().database().backend()).isEqualTo(DatabaseConfig.Backend.SQLITE);
    }

    @Test
    void onEnable_freshServer_createsMigratedSqliteDatabase() {
        Path databaseFile = plugin.getDataFolder().toPath()
                .resolve(plugin.getConfigService().database().sqliteFileName());

        assertThat(Files.exists(databaseFile)).isTrue();
    }

    @Test
    void onDisable_afterEnable_shutsDownWithoutError() {
        server.getPluginManager().disablePlugin(plugin);

        assertThat(plugin.isEnabled()).isFalse();
    }
}
