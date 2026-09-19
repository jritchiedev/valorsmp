package net.thevalorsmp.combat.listener;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.thevalorsmp.combat.cooldown.AbilityCooldownManager;
import net.thevalorsmp.config.CombatConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Enforces the mace rules (docs/combat.md): a configurable attack cooldown (default 60s), shown in
 * the hotbar via the vanilla item cooldown overlay, and a ban on enchanting the mace via an
 * enchantment table or an anvil.
 */
public final class MaceListener implements Listener {

    /** Ability key used with {@link AbilityCooldownManager}. */
    public static final String MACE_ABILITY = "mace";

    private static final int TICKS_PER_SECOND = 20;

    private final AbilityCooldownManager cooldowns;
    private final int cooldownSeconds;

    /**
     * Creates the listener.
     *
     * @param cooldowns    shared ability cooldown manager
     * @param combatConfig combat config supplying the mace cooldown
     */
    public MaceListener(@NotNull AbilityCooldownManager cooldowns, @NotNull CombatConfig combatConfig) {
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
        this.cooldownSeconds = Objects.requireNonNull(combatConfig, "combatConfig").maceCooldownSeconds();
    }

    /**
     * Applies and enforces the mace attack cooldown.
     *
     * @param event the melee damage event
     */
    @EventHandler(ignoreCancelled = true)
    public void onMaceAttack(@NotNull EntityDamageByEntityEvent event) {
        if (cooldownSeconds <= 0 || !(event.getDamager() instanceof Player player)) {
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.MACE) {
            return;
        }
        UUID id = player.getUniqueId();
        if (cooldowns.isOnCooldown(id, MACE_ABILITY)) {
            event.setCancelled(true);
            player.sendActionBar(Component.text(
                    "Mace on cooldown: " + cooldowns.remainingSeconds(id, MACE_ABILITY) + "s", NamedTextColor.RED));
            return;
        }
        cooldowns.start(id, MACE_ABILITY, Duration.ofSeconds(cooldownSeconds));
        player.setCooldown(Material.MACE, cooldownSeconds * TICKS_PER_SECOND);
    }

    /**
     * Blocks enchanting a mace at an enchantment table (offer stage).
     *
     * @param event the prepare-enchant event
     */
    @EventHandler(ignoreCancelled = true)
    public void onPrepareEnchant(@NotNull PrepareItemEnchantEvent event) {
        if (event.getItem().getType() == Material.MACE) {
            event.setCancelled(true);
        }
    }

    /**
     * Blocks enchanting a mace at an enchantment table (apply stage).
     *
     * @param event the enchant event
     */
    @EventHandler(ignoreCancelled = true)
    public void onEnchant(@NotNull EnchantItemEvent event) {
        if (event.getItem().getType() == Material.MACE) {
            event.setCancelled(true);
        }
    }

    /**
     * Blocks adding or upgrading enchantments on a mace via an anvil, while still allowing rename and
     * repair (which do not introduce new enchantments).
     *
     * @param event the prepare-anvil event
     */
    @EventHandler
    public void onPrepareAnvil(@NotNull PrepareAnvilEvent event) {
        ItemStack base = event.getView().getTopInventory().getItem(0);
        ItemStack result = event.getResult();
        if (base == null || result == null || base.getType() != Material.MACE) {
            return;
        }
        if (introducesEnchantments(base, result)) {
            event.setResult(null);
        }
    }

    private static boolean introducesEnchantments(ItemStack base, ItemStack result) {
        for (var entry : result.getEnchantments().entrySet()) {
            if (base.getEnchantments().getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return true;
            }
        }
        return false;
    }
}
