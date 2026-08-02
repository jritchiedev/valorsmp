package net.thevalorsmp.progression.command;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.thevalorsmp.progression.model.ValorTier;
import net.thevalorsmp.progression.perk.SpeedPreferenceManager;
import net.thevalorsmp.progression.perk.TierPerkService;
import net.thevalorsmp.progression.service.ValorScoreService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /speed set <1|2>} — lets Valor IV and V players switch their permanent Speed between
 * Speed I and Speed II (docs/progression.md).
 */
public final class SpeedCommand implements CommandExecutor {

    private final ValorScoreService valorScoreService;
    private final TierPerkService perkService;
    private final SpeedPreferenceManager speedPreferences;

    /**
     * Creates the command.
     *
     * @param valorScoreService source of the player's current tier
     * @param perkService       reapplies perks after a change
     * @param speedPreferences  stores the chosen Speed level
     */
    public SpeedCommand(
            @NotNull ValorScoreService valorScoreService,
            @NotNull TierPerkService perkService,
            @NotNull SpeedPreferenceManager speedPreferences) {
        this.valorScoreService = Objects.requireNonNull(valorScoreService, "valorScoreService");
        this.perkService = Objects.requireNonNull(perkService, "perkService");
        this.speedPreferences = Objects.requireNonNull(speedPreferences, "speedPreferences");
    }

    /**
     * Handles the {@code /speed} command.
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use /speed.", NamedTextColor.RED));
            return true;
        }
        if (args.length != 2 || !args[0].equalsIgnoreCase("set")) {
            player.sendMessage(Component.text("Usage: /speed set <1|2>", NamedTextColor.YELLOW));
            return true;
        }
        int level = parseLevel(args[1]);
        if (level == 0) {
            player.sendMessage(Component.text("Speed level must be 1 or 2.", NamedTextColor.YELLOW));
            return true;
        }

        ValorTier tier = valorScoreService.currentTier(player.getUniqueId());
        if (!tier.isSpeedSwitchable()) {
            player.sendMessage(Component.text(
                    "Switching speed requires Valor IV or higher.", NamedTextColor.RED));
            return true;
        }

        speedPreferences.setAmplifier(player.getUniqueId(), level - 1);
        perkService.applyPerks(player, tier);
        player.sendMessage(Component.text("Your Valor speed is now Speed " + level + ".", NamedTextColor.GREEN));
        return true;
    }

    private static int parseLevel(String raw) {
        if ("1".equals(raw)) {
            return 1;
        }
        if ("2".equals(raw)) {
            return 2;
        }
        return 0;
    }
}
