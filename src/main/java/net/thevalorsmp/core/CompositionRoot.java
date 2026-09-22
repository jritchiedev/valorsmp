package net.thevalorsmp.core;

import java.time.Clock;
import java.util.Objects;
import javax.sql.DataSource;
import net.thevalorsmp.combat.cooldown.AbilityCooldownManager;
import net.thevalorsmp.combat.dragonegg.DragonEggController;
import net.thevalorsmp.combat.listener.CombatListener;
import net.thevalorsmp.combat.listener.MaceListener;
import net.thevalorsmp.combat.listener.SpearListener;
import net.thevalorsmp.combat.service.CombatService;
import net.thevalorsmp.config.CombatConfig;
import net.thevalorsmp.config.ConfigService;
import net.thevalorsmp.core.command.AdminCommand;
import net.thevalorsmp.core.event.BukkitDomainEventPublisher;
import net.thevalorsmp.core.event.DomainEventPublisher;
import net.thevalorsmp.profile.listener.PlayerProfileListener;
import net.thevalorsmp.profile.repository.SqlPlayerProfileRepository;
import net.thevalorsmp.profile.service.PlayerProfileService;
import net.thevalorsmp.progression.command.SpeedCommand;
import net.thevalorsmp.progression.command.ValorCommand;
import net.thevalorsmp.progression.listener.ExtendedPotionListener;
import net.thevalorsmp.progression.listener.ValorPerkListener;
import net.thevalorsmp.progression.perk.SpeedPreferenceManager;
import net.thevalorsmp.progression.perk.TierPerkService;
import net.thevalorsmp.progression.repository.SqlValorScoreRepository;
import net.thevalorsmp.progression.repository.ValorScoreRepository;
import net.thevalorsmp.progression.service.ValorScoreService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;

/**
 * The single place where production object graphs are constructed (ARCHITECTURE.md section 3).
 * Repositories, services, listeners, and commands are wired here by constructor injection; no other
 * class instantiates a concrete repository or service implementation.
 */
public final class CompositionRoot {

    private static final long DRAGON_EGG_TICK_PERIOD = 20L;

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
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        DomainEventPublisher eventPublisher = new BukkitDomainEventPublisher(plugin.getServer());
        CombatConfig combatConfig = configService.combat();

        PlayerProfileService profileService =
                new PlayerProfileService(new SqlPlayerProfileRepository(dataSource));

        ValorScoreRepository valorScoreRepository = new SqlValorScoreRepository(dataSource);
        ValorScoreService valorScoreService = new ValorScoreService(
                valorScoreRepository, configService.progression(), eventPublisher, plugin.getSLF4JLogger());

        SpeedPreferenceManager speedPreferences = new SpeedPreferenceManager();
        TierPerkService perkService = new TierPerkService(speedPreferences, combatConfig);
        CombatService combatService = new CombatService(valorScoreService, eventPublisher);

        AbilityCooldownManager cooldowns = new AbilityCooldownManager(Clock.systemUTC());
        DragonEggController dragonEgg = new DragonEggController(valorScoreService, perkService);

        pluginManager.registerEvents(new PlayerProfileListener(profileService), plugin);
        pluginManager.registerEvents(new ValorPerkListener(valorScoreService, perkService, speedPreferences), plugin);
        pluginManager.registerEvents(
                new ExtendedPotionListener(valorScoreService, configService.progression()), plugin);
        pluginManager.registerEvents(new CombatListener(combatService), plugin);
        pluginManager.registerEvents(new MaceListener(cooldowns, combatConfig), plugin);
        pluginManager.registerEvents(new SpearListener(plugin, combatConfig), plugin);
        pluginManager.registerEvents(dragonEgg, plugin);
        plugin.getServer().getScheduler()
                .runTaskTimer(plugin, dragonEgg, DRAGON_EGG_TICK_PERIOD, DRAGON_EGG_TICK_PERIOD);

        registerCommand("valor", new ValorCommand(valorScoreService));
        registerCommand("speed", new SpeedCommand(valorScoreService, perkService, speedPreferences));
        registerCommand("valorsmp", new AdminCommand(plugin, configService, valorScoreService));

        plugin.getSLF4JLogger().debug(
                "Composition root built with storage backend {}.", configService.database().backend());
        return new ServiceRegistry();
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command '" + name + "' is not declared in plugin.yml");
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }
}
