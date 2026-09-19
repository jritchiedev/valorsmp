package net.thevalorsmp.progression;

import static org.assertj.core.api.Assertions.assertThat;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import java.util.List;
import java.util.Map;
import net.thevalorsmp.config.CombatConfig;
import net.thevalorsmp.config.ProgressionConfig;
import net.thevalorsmp.progression.listener.ValorPerkListener;
import net.thevalorsmp.progression.perk.SpeedPreferenceManager;
import net.thevalorsmp.progression.perk.TierPerkService;
import net.thevalorsmp.progression.service.ValorScoreService;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.slf4j.helpers.NOPLogger;

/**
 * Verifies perks are reapplied from the post-respawn event under MockBukkit, after the server has
 * wiped the respawned player's effects (TESTING.md section 3).
 */
class ValorPerkListenerIntegrationTest {

    private static final ProgressionConfig CONFIG =
            new ProgressionConfig(1, List.of(0, 5, 9, 13, 17), 20, true, Map.of());

    private ServerMock server;
    private Player player;
    private ValorPerkListener listener;
    private InMemoryValorScoreRepository repository;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        repository = new InMemoryValorScoreRepository();
        ValorScoreService valorScoreService =
                new ValorScoreService(repository, CONFIG, event -> { }, NOPLogger.NOP_LOGGER);
        TierPerkService perkService =
                new TierPerkService(new SpeedPreferenceManager(), new CombatConfig(60, 30, 2));
        listener = new ValorPerkListener(valorScoreService, perkService, new SpeedPreferenceManager());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onPlayerPostRespawn_tierV_reappliesSpeedAndStrength() {
        repository.saveScore(player.getUniqueId(), CONFIG.currentSeason(), 19); // Valor V

        listener.onPlayerPostRespawn(new PlayerPostRespawnEvent(
                player, player.getLocation(), false, false, false, PlayerRespawnEvent.RespawnReason.DEATH));

        assertThat(effect(PotionEffectType.SPEED)).isNotNull();
        assertThat(effect(PotionEffectType.SPEED).getAmplifier()).isEqualTo(1);
        assertThat(effect(PotionEffectType.STRENGTH)).isNotNull();
        assertThat(effect(PotionEffectType.STRENGTH).getAmplifier()).isEqualTo(1);
    }

    private PotionEffect effect(PotionEffectType type) {
        return player.getPotionEffect(type);
    }
}
