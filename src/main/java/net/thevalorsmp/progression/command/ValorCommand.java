package net.thevalorsmp.progression.command;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.thevalorsmp.progression.model.ValorTier;
import net.thevalorsmp.progression.service.ValorScoreService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /valor} — shows the sender their current Valor score and tier.
 */
public final class ValorCommand implements CommandExecutor {

    private final ValorScoreService valorScoreService;

    /**
     * Creates the command.
     *
     * @param valorScoreService the Valor scoring service
     */
    public ValorCommand(@NotNull ValorScoreService valorScoreService) {
        this.valorScoreService = Objects.requireNonNull(valorScoreService, "valorScoreService");
    }

    /**
     * Handles the {@code /valor} command.
     *
     * @param sender  the command sender
     * @param command the command
     * @param label   the alias used
     * @param args    the arguments
     * @return {@code true}
     */
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players have a Valor score.", NamedTextColor.RED));
            return true;
        }
        int score = valorScoreService.currentScore(player.getUniqueId());
        ValorTier tier = valorScoreService.currentTier(player.getUniqueId());
        player.sendMessage(Component.text(
                "Valor: " + score + " points — " + tier.displayName(), NamedTextColor.GOLD));
        return true;
    }
}
