package net.thevalorsmp.combat.service;

import java.util.Objects;
import java.util.UUID;
import net.thevalorsmp.combat.events.PlayerKilledEvent;
import net.thevalorsmp.core.event.DomainEventPublisher;
import net.thevalorsmp.progression.model.ValorAwardResult;
import net.thevalorsmp.progression.service.ValorScoreService;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Orchestrates PvP-death bookkeeping: publishes {@link PlayerKilledEvent} and drives the +1/-1 Valor
 * award through {@link ValorScoreService} (docs/combat.md). There is no combat tagging.
 */
public final class CombatService {

    private final ValorScoreService valorScoreService;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates the service.
     *
     * @param valorScoreService the Valor scoring service
     * @param eventPublisher    publisher for {@link PlayerKilledEvent}
     */
    public CombatService(
            @NotNull ValorScoreService valorScoreService,
            @NotNull DomainEventPublisher eventPublisher) {
        this.valorScoreService = Objects.requireNonNull(valorScoreService, "valorScoreService");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    /**
     * Handles a confirmed player-versus-player kill.
     *
     * @param killer   the killing player
     * @param victim   the killed player
     * @param location where the death occurred
     * @return the resulting Valor scores for both players
     */
    public @NotNull ValorAwardResult handlePlayerKill(
            @NotNull UUID killer, @NotNull UUID victim, @NotNull Location location) {
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(victim, "victim");
        Objects.requireNonNull(location, "location");
        eventPublisher.publish(new PlayerKilledEvent(victim, killer, location));
        return valorScoreService.awardKill(killer, victim);
    }
}
