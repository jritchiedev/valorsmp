package net.thevalorsmp.combat.command;

import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.thevalorsmp.combat.item.SpearItemService;
import net.thevalorsmp.combat.item.SpearTier;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /valorspear <tier> [player]} — grants a custom spear (operator tool for alpha, since spears
 * are not yet obtainable in-world).
 */
public final class SpearCommand implements CommandExecutor {

    private final SpearItemService spearItems;

    /**
     * Creates the command.
     *
     * @param spearItems spear item factory
     */
    public SpearCommand(@NotNull SpearItemService spearItems) {
        this.spearItems = Objects.requireNonNull(spearItems, "spearItems");
    }

    /**
     * Handles the {@code /valorspear} command.
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
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /valorspear <tier> [player]", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text(
                    "Tiers: wood, copper, iron, gold, diamond, netherite", NamedTextColor.GRAY));
            return true;
        }
        Optional<SpearTier> tier = SpearTier.fromString(args[0]);
        if (tier.isEmpty()) {
            sender.sendMessage(Component.text("Unknown spear tier: " + args[0], NamedTextColor.RED));
            return true;
        }

        Player target = resolveTarget(sender, args);
        if (target == null) {
            return true;
        }
        target.getInventory().addItem(spearItems.create(tier.get()));
        sender.sendMessage(Component.text(
                "Gave " + tier.get().displayName() + " to " + target.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private static Player resolveTarget(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Player named = Bukkit.getPlayerExact(args[1]);
            if (named == null) {
                sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            }
            return named;
        }
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(Component.text("Specify a player when running from console.", NamedTextColor.RED));
        return null;
    }
}
