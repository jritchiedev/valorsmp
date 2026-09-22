package net.thevalorsmp.core.command;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.thevalorsmp.config.ConfigService;
import net.thevalorsmp.progression.service.ValorScoreService;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /valorsmp <health|version|reload|valor>} — lightweight operator diagnostics
 * (OBSERVABILITY.md section 6) and admin Valor adjustment. Config is not hot-reloadable in alpha;
 * changing it requires a restart.
 */
public final class AdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("health", "version", "reload", "valor");
    private static final List<String> VALOR_ACTIONS = List.of("give", "take", "set");
    private static final String VALOR_USAGE =
            "Usage: /valorsmp valor <give|take|set> <player> <amount>";

    private final Plugin plugin;
    private final ConfigService configService;
    private final ValorScoreService valorScoreService;

    /**
     * Creates the command.
     *
     * @param plugin           the owning plugin (for version and server access)
     * @param configService    the loaded configuration
     * @param valorScoreService the Valor scoring service used by the {@code valor} subcommand
     */
    public AdminCommand(
            @NotNull Plugin plugin,
            @NotNull ConfigService configService,
            @NotNull ValorScoreService valorScoreService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configService = Objects.requireNonNull(configService, "configService");
        this.valorScoreService = Objects.requireNonNull(valorScoreService, "valorScoreService");
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
        String subcommand = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "health" -> sendHealth(sender);
            case "version" -> sender.sendMessage(Component.text(
                    "The Valor SMP " + plugin.getPluginMeta().getVersion(), NamedTextColor.AQUA));
            case "reload" -> sender.sendMessage(Component.text(
                    "Config is not hot-reloadable in alpha; restart the server to apply changes.",
                    NamedTextColor.YELLOW));
            case "valor" -> handleValor(sender, args);
            default -> sender.sendMessage(Component.text(
                    "Usage: /valorsmp <health|version|reload|valor>", NamedTextColor.YELLOW));
        }
        return true;
    }

    /**
     * Completes {@code /valorsmp} subcommand, valor action, and target player arguments.
     *
     * @param sender  the command sender
     * @param command the command
     * @param label   the alias used
     * @param args    the arguments typed so far
     * @return candidate completions filtered by the current argument's prefix
     */
    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return switch (args.length) {
            case 1 -> filter(SUBCOMMANDS, args[0]);
            case 2 -> isValor(args) ? filter(VALOR_ACTIONS, args[1]) : List.of();
            case 3 -> isValor(args) ? filter(onlinePlayerNames(), args[2]) : List.of();
            default -> List.of();
        };
    }

    private void handleValor(CommandSender sender, String[] args) {
        if (args.length < 4 || !VALOR_ACTIONS.contains(args[1].toLowerCase(Locale.ROOT))) {
            sender.sendMessage(Component.text(VALOR_USAGE, NamedTextColor.YELLOW));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);

        Player online = plugin.getServer().getPlayerExact(args[2]);
        OfflinePlayer target = online != null ? online : plugin.getServer().getOfflinePlayerIfCached(args[2]);
        if (target == null) {
            sender.sendMessage(Component.text("Unknown player: " + args[2], NamedTextColor.RED));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text(VALOR_USAGE, NamedTextColor.YELLOW));
            return;
        }
        if (amount < 0) {
            sender.sendMessage(Component.text(VALOR_USAGE, NamedTextColor.YELLOW));
            return;
        }

        UUID uuid = target.getUniqueId();
        int score = switch (action) {
            case "give" -> valorScoreService.adjustScore(uuid, amount);
            case "take" -> valorScoreService.adjustScore(uuid, -amount);
            default -> valorScoreService.setScore(uuid, amount);
        };
        sender.sendMessage(Component.text(
                args[2] + " now has " + score + " Valor (" + valorScoreService.currentTier(uuid).displayName() + ")",
                NamedTextColor.GREEN));
        if (online != null) {
            online.sendMessage(Component.text(
                    "Your Valor was set to " + score + " by " + sender.getName() + ".", NamedTextColor.YELLOW));
        }
    }

    private static boolean isValor(String[] args) {
        return "valor".equalsIgnoreCase(args[0]);
    }

    private List<String> onlinePlayerNames() {
        return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
    }

    private static List<String> filter(List<String> candidates, String prefix) {
        String lowered = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(lowered))
                .toList();
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
