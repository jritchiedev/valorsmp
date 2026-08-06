package net.thevalorsmp.combat.dragonegg;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.thevalorsmp.progression.perk.TierPerkService;
import net.thevalorsmp.progression.service.ValorScoreService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dragon-egg rules (docs/combat.md): grants permanent Strength while the egg is carried (applied via
 * {@link TierPerkService}, refreshed by this task when a player's carry state changes) and prevents
 * the egg from being placed inside an ender chest.
 */
public final class DragonEggController implements Listener, Runnable {

    private final ValorScoreService valorScoreService;
    private final TierPerkService perkService;
    private final Map<UUID, Boolean> carrying = new ConcurrentHashMap<>();

    /**
     * Creates the controller.
     *
     * @param valorScoreService source of a player's current tier
     * @param perkService       applies the resulting effects
     */
    public DragonEggController(@NotNull ValorScoreService valorScoreService, @NotNull TierPerkService perkService) {
        this.valorScoreService = Objects.requireNonNull(valorScoreService, "valorScoreService");
        this.perkService = Objects.requireNonNull(perkService, "perkService");
    }

    /**
     * Scheduled tick: reapplies perks for any player whose dragon-egg carry state changed. Join and
     * respawn already apply perks, so the first observation only establishes a baseline.
     */
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            boolean now = player.getInventory().contains(Material.DRAGON_EGG);
            Boolean previous = carrying.put(id, now);
            if (previous != null && previous != now) {
                perkService.applyPerks(player, valorScoreService.currentTier(id));
            }
        }
    }

    /**
     * Forgets a player's carry state on quit.
     *
     * @param event the quit event
     */
    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        carrying.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Blocks moving a dragon egg into an ender chest via clicks (place, shift, hotbar swap).
     *
     * @param event the click event
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getView().getTopInventory().getType() != InventoryType.ENDER_CHEST) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        int rawSlot = event.getRawSlot();

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (rawSlot >= topSize && isDragonEgg(event.getCurrentItem())) {
                event.setCancelled(true);
            }
            return;
        }
        if (rawSlot >= topSize) {
            return;
        }
        if (event.getClick() == ClickType.NUMBER_KEY) {
            if (isDragonEgg(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()))) {
                event.setCancelled(true);
            }
            return;
        }
        if (isDragonEgg(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    /**
     * Blocks dragging a dragon egg into ender chest slots.
     *
     * @param event the drag event
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        if (event.getView().getTopInventory().getType() != InventoryType.ENDER_CHEST
                || !isDragonEgg(event.getOldCursor())) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private static boolean isDragonEgg(@Nullable ItemStack item) {
        return item != null && item.getType() == Material.DRAGON_EGG;
    }
}
