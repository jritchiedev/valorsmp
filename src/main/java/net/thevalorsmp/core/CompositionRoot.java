package net.thevalorsmp.core;

import java.util.Objects;
import javax.sql.DataSource;
import net.thevalorsmp.config.ConfigService;

/**
 * The single place where production object graphs are constructed (ARCHITECTURE.md section 3).
 * Repositories, services, listeners, and commands are wired here by constructor injection; no other
 * class instantiates a concrete repository or service implementation.
 */
public final class CompositionRoot {

    private final ValorPlugin plugin;
    private final ConfigService configService;
    private final DataSource dataSource;

    /**
     * Creates a composition root for the given plugin instance.
     *
     * @param plugin        the owning plugin, used for listener/command registration
     * @param configService validated configuration
     * @param dataSource    pooled data source shared by all repositories
     */
    public CompositionRoot(ValorPlugin plugin, ConfigService configService, DataSource dataSource) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configService = Objects.requireNonNull(configService, "configService");
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    /**
     * Builds the full object graph and registers Bukkit-facing adapters.
     *
     * @return the registry of long-lived services owned by this plugin instance
     */
    public ServiceRegistry build() {
        // Feature repositories, services, listeners, and commands are wired here as features land.
        // Kept intentionally empty in the scaffold so the first feature PR has an obvious, single home.
        plugin.getSLF4JLogger().debug(
                "Composition root built with storage backend {}.", configService.database().backend());
        return new ServiceRegistry();
    }

    DataSource dataSource() {
        return dataSource;
    }
}
