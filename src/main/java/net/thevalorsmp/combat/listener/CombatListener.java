package net.thevalorsmp.combat.listener;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.thevalorsmp.combat.service.CombatService;
import net.thevalorsmp.progression.model.ValorAwardResult;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit adapter translating a PvP {@link PlayerDeathEvent} into a Valor award and messaging both
 * players with the exact wording from docs/valor-system.md. Enforces drop-everything on PvP death.
 */
public final class CombatListener implements Listener {

    private final CombatService combatService;

    /**
     * Creates the listener.
     *
     * @param combatService the combat service
     */
    public CombatListener(@NotNull CombatService combatService) {
        this.combatService = Objects.requireNonNull(combatService, "combatService");
    }

    /**
     * Awards Valor when a player is killed by another player.
     *
     * @param event the death event
     */
    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        // docs/combat.md: PvP death drops everything, regardless of server keep-inventory settings.
        event.setKeepInventory(false);
        event.setKeepLevel(false);

        ValorAwardResult result =
                combatService.handlePlayerKill(killer.getUniqueId(), victim.getUniqueId(), victim.getLocation());

        killer.sendMessage(Component.text(
                "You've gained 1 Valor Point from killing a player. You now have "
                        + result.killerScore() + " Valor Points.",
                NamedTextColor.GREEN));
        victim.sendMessage(Component.text(
                "You've lost 1 Valor Point due to dying. You now have "
                        + result.victimScore() + " Valor Points.",
                NamedTextColor.RED));
    }
}
