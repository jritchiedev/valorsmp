package net.thevalorsmp.progression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.thevalorsmp.config.ProgressionConfig;
import net.thevalorsmp.progression.events.ValorRankChangedEvent;
import net.thevalorsmp.progression.model.ValorAwardResult;
import net.thevalorsmp.progression.model.ValorTier;
import net.thevalorsmp.progression.service.ValorScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

class ValorScoreServiceTest {

    private static final ProgressionConfig CONFIG =
            new ProgressionConfig(1, List.of(0, 5, 9, 13, 17), 20, true, Map.of());

    private InMemoryValorScoreRepository repository;
    private RecordingDomainEventPublisher events;
    private ValorScoreService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryValorScoreRepository();
        events = new RecordingDomainEventPublisher();
        service = new ValorScoreService(repository, CONFIG, events, NOPLogger.NOP_LOGGER);
    }

    @Test
    void awardKill_incrementsKillerAndFloorsVictimAtZero() {
        ValorAwardResult result = service.awardKill(UUID.randomUUID(), UUID.randomUUID());

        assertThat(result.killerScore()).isEqualTo(1);
        assertThat(result.victimScore()).isZero();
    }

    @Test
    void awardKill_victimWithScore_decrementsByOne() {
        UUID victim = UUID.randomUUID();
        repository.saveScore(victim, 1, 3);

        ValorAwardResult result = service.awardKill(UUID.randomUUID(), victim);

        assertThat(result.victimScore()).isEqualTo(2);
    }

    @Test
    void awardKill_selfKill_rejected() {
        UUID player = UUID.randomUUID();

        assertThatThrownBy(() -> service.awardKill(player, player))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void awardKill_crossingTierThreshold_firesRankChanged() {
        UUID killer = UUID.randomUUID();
        repository.saveScore(killer, 1, 4); // one kill short of Valor II (threshold 5)

        service.awardKill(killer, UUID.randomUUID());

        assertThat(events.published()).hasSize(1);
        assertThat(events.published().get(0)).isInstanceOf(ValorRankChangedEvent.class);
        ValorRankChangedEvent event = (ValorRankChangedEvent) events.published().get(0);
        assertThat(event.getPlayerId()).isEqualTo(killer);
        assertThat(event.getOldTier()).isEqualTo(ValorTier.I);
        assertThat(event.getNewTier()).isEqualTo(ValorTier.II);
    }

    @Test
    void awardKill_noTierChange_firesNoEvent() {
        service.awardKill(UUID.randomUUID(), UUID.randomUUID());

        assertThat(events.published()).isEmpty();
    }

    @Test
    void awardKill_killerAtCap_staysAtCapWithoutSaveOrEvent() {
        UUID killer = UUID.randomUUID();
        repository.saveScore(killer, 1, 20);

        ValorAwardResult result = service.awardKill(killer, UUID.randomUUID());

        assertThat(result.killerScore()).isEqualTo(20);
        assertThat(repository.saves()).isEqualTo(1); // the seeding save only
        assertThat(events.published()).isEmpty();
    }

    @Test
    void awardKill_killerBelowCap_clampsAtMaxValor() {
        UUID killer = UUID.randomUUID();
        repository.saveScore(killer, 1, 19);

        ValorAwardResult result = service.awardKill(killer, UUID.randomUUID());

        assertThat(result.killerScore()).isEqualTo(20);
    }

    @Test
    void currentTier_reflectsScore() {
        UUID player = UUID.randomUUID();
        repository.saveScore(player, 1, 13);

        assertThat(service.currentTier(player)).isEqualTo(ValorTier.IV);
    }
}
