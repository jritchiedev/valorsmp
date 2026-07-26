package net.thevalorsmp.core;

import javax.sql.DataSource;
import net.thevalorsmp.config.ConfigBootstrap;
import net.thevalorsmp.config.ConfigService;
import net.thevalorsmp.storage.DataSourceFactory;
import net.thevalorsmp.storage.migration.SqlMigrationRunner;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit entry point for The Valor SMP. Owns only lifecycle concerns: config bootstrap, data source
 * creation, migrations, and handing control to {@link CompositionRoot} for wiring (ARCHITECTURE.md section 6).
 *
 * <p>Not {@code final} only because MockBukkit subclasses the plugin class to load it in integration
 * tests; it is not otherwise designed for extension.
 */
public class ValorPlugin extends JavaPlugin {

    private ConfigService configService;
    private DataSourceFactory dataSourceFactory;
    private ServiceRegistry serviceRegistry;

    @Override
    public void onEnable() {
        ConfigBootstrap configBootstrap = new ConfigBootstrap(getDataFolder().toPath(), getSLF4JLogger());
        configBootstrap.copyAndMergeDefaults(ConfigService.CONFIG_FILES, resourceLoader());
        configService = ConfigService.load(configBootstrap, getSLF4JLogger());

        dataSourceFactory = new DataSourceFactory(getDataFolder().toPath(), getSLF4JLogger());
        DataSource dataSource = dataSourceFactory.create(configService.database());

        new SqlMigrationRunner(dataSource, getSLF4JLogger()).migrate(SqlMigrationRunner.DEFAULT_MIGRATIONS);

        serviceRegistry = new CompositionRoot(this, configService, dataSource).build();
        getSLF4JLogger().info("The Valor SMP enabled (version {}).", getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        if (serviceRegistry != null) {
            serviceRegistry.shutdown();
            serviceRegistry = null;
        }
        if (dataSourceFactory != null) {
            dataSourceFactory.close();
            dataSourceFactory = null;
        }
    }

    /**
     * Returns the loaded, validated configuration for this plugin instance.
     *
     * @return the config service; never {@code null} once {@link #onEnable()} has completed
     */
    public @NotNull ConfigService getConfigService() {
        return configService;
    }

    private ConfigBootstrap.ResourceLoader resourceLoader() {
        return name -> getResource(name);
    }
}
