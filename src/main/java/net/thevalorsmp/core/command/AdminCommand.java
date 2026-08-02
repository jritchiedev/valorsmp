package net.thevalorsmp.core.command;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.thevalorsmp.config.ConfigService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /valorsmp <health|version>} — lightweight operator diagnostics (OBSERVABILITY.md section 6).
 * Config is not hot-reloadable in alpha; changing it requires a restart.
 */
public final class AdminCommand implements CommandExecutor {

    private final Plugin plugin;
    private final ConfigService configService;

    /**
     * Creates the command.
     *
     * @param plugin        the owning plugin (for version and server access)
     * @param configService the loaded configuration
     */
    public AdminCommand(@NotNull Plugin plugin, @NotNull ConfigService configService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configService = Objects.requireNonNull(configService, "configService");
    }

    /**
     * Handles the {@code /valorsmp} command.
     *
     * @param sender  the command sender
     * @param command the command
     * @param label   the alias used
     * @param args    the arguments
     * @return {@code true}; usage errors are reported to the sender
     */
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String subcommand = args.length == 0 ? "" : args[0].toLowerCase(java.util.Locale.ROOT);
        switch (subcommand) {
            case "health" -> sendHealth(sender);
            case "version" -> sender.sendMessage(Component.text(
                    "The Valor SMP " + plugin.getPluginMeta().getVersion(), NamedTextColor.AQUA));
            case "reload" -> sender.sendMessage(Component.text(
                    "Config is not hot-reloadable in alpha; restart the server to apply changes.",
                    NamedTextColor.YELLOW));
            default -> sender.sendMessage(Component.text(
                    "Usage: /valorsmp <health|version|reload>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private void sendHealth(CommandSender sender) {
        sender.sendMessage(Component.text("The Valor SMP — healthy", NamedTextColor.GREEN));
        sender.sendMessage(Component.text(
                "Storage backend: " + configService.database().backend(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(
                "Online players: " + plugin.getServer().getOnlinePlayers().size(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(
                "Current season: " + configService.progression().currentSeason(), NamedTextColor.GRAY));
    }
}
