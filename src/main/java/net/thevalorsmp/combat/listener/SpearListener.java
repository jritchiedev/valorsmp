package net.thevalorsmp.combat.listener;

import java.util.Objects;
import net.thevalorsmp.config.CombatConfig;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Enforces the spear Lunge cooldown (docs/combat.md, default 30s). A jab with a Lunge-enchanted
 * vanilla spear puts the spear on a server-set vanilla item cooldown, visible in the hotbar.
 * The vanilla jab path fires no Bukkit event, so the cooldown is keyed off the enchantment's
 * exhaustion effect: a lunge attempted while on cooldown costs no hunger and the dash is
 * undone on the next tick. Unenchanted spears and the charged attack are unaffected.
 */
public final class SpearListener implements Listener {

    private static final int TICKS_PER_SECOND = 20;

    private final Plugin plugin;
    private final int cooldownSeconds;

    /**
     * Creates the listener.
     *
     * @param plugin       the owning plugin, used to schedule the next-tick dash undo
     * @param combatConfig combat config supplying the lunge cooldown
     */
    public SpearListener(@NotNull Plugin plugin, @NotNull CombatConfig combatConfig) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.cooldownSeconds = Objects.requireNonNull(combatConfig, "combatConfig").spearLungeCooldownSeconds();
    }

    /**
     * Applies the configured item cooldown when a Lunge-enchanted spear lunge fires.
     *
     * @param event the exhaustion event raised by the lunge's enchantment effect
     */
    @EventHandler(ignoreCancelled = true)
    public void onLunge(@NotNull EntityExhaustionEvent event) {
        if (event.getExhaustionReason() != EntityExhaustionEvent.ExhaustionReason.ENCHANTMENT_EFFECT
                || !(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isLungeSpear(item) || cooldownSeconds <= 0) {
            return;
        }
        if (player.hasCooldown(item.getType())) {
            // The Lunge impulse lands after this event in the same tick, so the dash is undone on
            // the next tick by snapping the player back; the cooldown itself is left untouched.
            event.setCancelled(true);
            Location origin = player.getLocation();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    Location target = origin.clone();
                    target.setYaw(player.getLocation().getYaw());
                    target.setPitch(player.getLocation().getPitch());
                    player.teleport(target);
                    player.setVelocity(new Vector(0, player.getVelocity().getY(), 0));
                }
            });
            return;
        }
        player.setCooldown(item.getType(), cooldownSeconds * TICKS_PER_SECOND);
    }

    static boolean isLungeSpear(@NotNull ItemStack item) {
        return Tag.ITEMS_SPEARS.isTagged(item.getType()) && item.containsEnchantment(Enchantment.LUNGE);
    }
}
