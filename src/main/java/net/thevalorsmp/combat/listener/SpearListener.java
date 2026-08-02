package net.thevalorsmp.combat.listener;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.thevalorsmp.combat.cooldown.AbilityCooldownManager;
import net.thevalorsmp.combat.item.SpearItemService;
import net.thevalorsmp.config.CombatConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Implements the spear "lunge" ability (docs/combat.md): right-clicking a lunge spear dashes the
 * player forward, on a configurable cooldown (default 10s).
 */
public final class SpearListener implements Listener {

    /** Ability key used with {@link AbilityCooldownManager}. */
    public static final String LUNGE_ABILITY = "spear-lunge";

    private static final double LUNGE_POWER = 1.4;
    private static final double LUNGE_MIN_VERTICAL = 0.35;

    private final SpearItemService spearItems;
    private final AbilityCooldownManager cooldowns;
    private final int cooldownSeconds;

    /**
     * Creates the listener.
     *
     * @param spearItems   spear identification service
     * @param cooldowns    shared ability cooldown manager
     * @param combatConfig combat config supplying the lunge cooldown
     */
    public SpearListener(
            @NotNull SpearItemService spearItems,
            @NotNull AbilityCooldownManager cooldowns,
            @NotNull CombatConfig combatConfig) {
        this.spearItems = Objects.requireNonNull(spearItems, "spearItems");
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
        this.cooldownSeconds = Objects.requireNonNull(combatConfig, "combatConfig").spearLungeCooldownSeconds();
    }

    /**
     * Performs a lunge when a player right-clicks a lunge spear.
     *
     * @param event the interact event
     */
    @EventHandler(ignoreCancelled = true)
    public void onLunge(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!spearItems.isSpear(item) || !spearItems.hasLunge(item)) {
            return;
        }

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (cooldownSeconds > 0 && cooldowns.isOnCooldown(id, LUNGE_ABILITY)) {
            player.sendActionBar(Component.text(
                    "Lunge on cooldown: " + cooldowns.remainingSeconds(id, LUNGE_ABILITY) + "s", NamedTextColor.RED));
            return;
        }

        Vector direction = player.getEyeLocation().getDirection().normalize().multiply(LUNGE_POWER);
        direction.setY(Math.max(direction.getY(), LUNGE_MIN_VERTICAL));
        player.setVelocity(direction);
        if (cooldownSeconds > 0) {
            cooldowns.start(id, LUNGE_ABILITY, Duration.ofSeconds(cooldownSeconds));
        }
        player.sendActionBar(Component.text("Lunge!", NamedTextColor.AQUA));
    }
}
