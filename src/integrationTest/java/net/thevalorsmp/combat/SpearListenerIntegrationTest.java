package net.thevalorsmp.combat;

import static org.assertj.core.api.Assertions.assertThat;

import net.thevalorsmp.combat.listener.SpearListener;
import net.thevalorsmp.config.CombatConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

/**
 * Verifies the spear Lunge cooldown under MockBukkit: the listener is constructed directly and its
 * handler invoked with a constructed {@link EntityExhaustionEvent} (TESTING.md section 3).
 */
class SpearListenerIntegrationTest {

    private static final int LUNGE_COOLDOWN_SECONDS = 30;

    private ServerMock server;
    private PluginMock plugin;
    private Player player;
    private SpearListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        player = server.addPlayer();
        listener = new SpearListener(plugin, new CombatConfig(60, LUNGE_COOLDOWN_SECONDS, 2));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onLunge_lungeEnchantedSpear_startsItemCooldown() {
        player.getInventory().setItemInMainHand(lungeSpear());

        listener.onLunge(new EntityExhaustionEvent(player, ExhaustionReason.ENCHANTMENT_EFFECT, 4f));

        assertThat(player.hasCooldown(Material.IRON_SPEAR)).isTrue();
        assertThat(player.getCooldown(Material.IRON_SPEAR)).isEqualTo(LUNGE_COOLDOWN_SECONDS * 20);
    }

    @Test
    void onLunge_nonEnchantmentReason_noCooldown() {
        player.getInventory().setItemInMainHand(lungeSpear());

        listener.onLunge(new EntityExhaustionEvent(player, ExhaustionReason.SPRINT, 4f));

        assertThat(player.hasCooldown(Material.IRON_SPEAR)).isFalse();
    }

    @Test
    void onLunge_spearWithoutLunge_noCooldown() {
        player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SPEAR));

        listener.onLunge(new EntityExhaustionEvent(player, ExhaustionReason.ENCHANTMENT_EFFECT, 4f));

        assertThat(player.hasCooldown(Material.IRON_SPEAR)).isFalse();
    }

    @Test
    void onLunge_nonSpearWithEnchantment_noCooldown() {
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 1);
        player.getInventory().setItemInMainHand(sword);

        listener.onLunge(new EntityExhaustionEvent(player, ExhaustionReason.ENCHANTMENT_EFFECT, 4f));

        assertThat(player.hasCooldown(Material.IRON_SWORD)).isFalse();
    }

    @Test
    void onLunge_alreadyOnCooldown_cancelsEventAndUndoesDashNextTick() {
        player.getInventory().setItemInMainHand(lungeSpear());
        player.setCooldown(Material.IRON_SPEAR, 100);
        Location origin = player.getLocation().clone();
        EntityExhaustionEvent event =
                new EntityExhaustionEvent(player, ExhaustionReason.ENCHANTMENT_EFFECT, 4f);

        listener.onLunge(event);

        assertThat(event.isCancelled()).isTrue();
        assertThat(player.getCooldown(Material.IRON_SPEAR)).isEqualTo(100);

        // The vanilla impulse lands after the event; simulate the dash then tick the scheduler.
        player.teleport(origin.clone().add(3, 0, 0));
        server.getScheduler().performOneTick();

        assertThat(player.getLocation().distance(origin)).isLessThan(0.01);
    }

    @Test
    void onLunge_notOnCooldown_schedulesNoUndoTask() {
        player.getInventory().setItemInMainHand(lungeSpear());

        listener.onLunge(new EntityExhaustionEvent(player, ExhaustionReason.ENCHANTMENT_EFFECT, 4f));

        assertThat(server.getScheduler().getPendingTasks()).isEmpty();
    }

    private static ItemStack lungeSpear() {
        ItemStack spear = new ItemStack(Material.IRON_SPEAR);
        spear.addEnchantment(Enchantment.LUNGE, 1);
        return spear;
    }
}
