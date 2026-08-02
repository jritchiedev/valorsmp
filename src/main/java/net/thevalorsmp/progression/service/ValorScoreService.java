package net.thevalorsmp.progression.service;

import java.util.Objects;
import java.util.UUID;
import net.thevalorsmp.config.ProgressionConfig;
import net.thevalorsmp.core.event.DomainEventPublisher;
import net.thevalorsmp.progression.events.ValorRankChangedEvent;
import net.thevalorsmp.progression.model.ValorAwardResult;
import net.thevalorsmp.progression.model.ValorTier;
import net.thevalorsmp.progression.repository.ValorScoreRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * The single, auditable path for Valor score mutation (SECURITY.md section 9). Valor can only be
 * gained by killing players and only lost by dying to players; scores are integers floored at 0.
 * Fires {@link ValorRankChangedEvent} whenever a mutation changes a player's computed tier.
 */
public final class ValorScoreService {

    private final ValorScoreRepository repository;
    private final ProgressionConfig config;
    private final DomainEventPublisher eventPublisher;
    private final Logger logger;

    /**
     * Creates the service.
     *
     * @param repository     score persistence
     * @param config         progression config (season and tier thresholds)
     * @param eventPublisher publisher for {@link ValorRankChangedEvent}
     * @param logger         plugin logger for mutation auditing
     */
    public ValorScoreService(
            @NotNull ValorScoreRepository repository,
            @NotNull ProgressionConfig config,
            @NotNull DomainEventPublisher eventPublisher,
            @NotNull Logger logger) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.config = Objects.requireNonNull(config, "config");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Awards a PvP kill: +1 Valor to the killer, -1 to the victim (floored at 0). Fires a rank-change
     * event for either player whose tier changed.
     *
     * @param killer the killing player
     * @param victim the killed player (must differ from the killer)
     * @return the resulting scores for both players
     */
    public @NotNull ValorAwardResult awardKill(@NotNull UUID killer, @NotNull UUID victim) {
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(victim, "victim");
        if (killer.equals(victim)) {
            throw new IllegalArgumentException("A player cannot kill themselves for Valor");
        }
        int season = config.currentSeason();
        int killerScore = mutate(killer, season, +1, "kill");
        int victimScore = mutate(victim, season, -1, "death");
        return new ValorAwardResult(killerScore, victimScore);
    }

    /**
     * Returns a player's current-season Valor score.
     *
     * @param playerId player UUID
     * @return the score, never negative
     */
    public int currentScore(@NotNull UUID playerId) {
        return repository.findScore(Objects.requireNonNull(playerId, "playerId"), config.currentSeason());
    }

    /**
     * Returns a player's current Valor tier, computed from their current-season score.
     *
     * @param playerId player UUID
     * @return the computed tier
     */
    public @NotNull ValorTier currentTier(@NotNull UUID playerId) {
        return ValorTier.fromScore(currentScore(playerId), config.tierThresholds());
    }

    private int mutate(UUID playerId, int season, int delta, String reason) {
        int before = repository.findScore(playerId, season);
        int after = Math.max(0, before + delta);
        if (after == before) {
            return after;
        }
        repository.saveScore(playerId, season, after);
        logger.debug("valor-mutation player={} season={} reason={} before={} after={}",
                playerId, season, reason, before, after);
        ValorTier oldTier = ValorTier.fromScore(before, config.tierThresholds());
        ValorTier newTier = ValorTier.fromScore(after, config.tierThresholds());
        if (oldTier != newTier) {
            eventPublisher.publish(new ValorRankChangedEvent(playerId, oldTier, newTier));
        }
        return after;
    }
}
