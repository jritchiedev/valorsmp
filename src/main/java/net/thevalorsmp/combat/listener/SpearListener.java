package net.thevalorsmp.combat.listener;

import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import java.util.Objects;
import net.thevalorsmp.config.CombatConfig;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Vanilla spears carrying the vanilla Lunge enchantment get a server-set vanilla item cooldown
 * (docs/combat.md, default 30s) after each use, visible in the hotbar and blocking re-use.
 * Unenchanted spears are untouched.
 */
public final class SpearListener implements Listener {

    private static final int TICKS_PER_SECOND = 20;

    private final int cooldownSeconds;

    /**
     * Creates the listener.
     *
     * @param combatConfig combat config supplying the lunge cooldown
     */
    public SpearListener(@NotNull CombatConfig combatConfig) {
        this.cooldownSeconds = Objects.requireNonNull(combatConfig, "combatConfig").spearLungeCooldownSeconds();
    }

    /**
     * Applies the configured item cooldown when a Lunge-enchanted spear finishes a use.
     *
     * @param event the stop-using-item event
     */
    @EventHandler
    public void onSpearUse(@NotNull PlayerStopUsingItemEvent event) {
        ItemStack item = event.getItem();
        if (!Tag.ITEMS_SPEARS.isTagged(item.getType())) {
            return;
        }
        if (!item.containsEnchantment(Enchantment.LUNGE)) {
            return;
        }
        if (cooldownSeconds <= 0) {
            return;
        }
        event.getPlayer().setCooldown(item.getType(), cooldownSeconds * TICKS_PER_SECOND);
    }
}
