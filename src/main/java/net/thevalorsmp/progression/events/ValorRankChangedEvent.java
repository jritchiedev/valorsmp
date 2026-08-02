package net.thevalorsmp.progression.events;

import java.util.Objects;
import java.util.UUID;
import net.thevalorsmp.progression.model.ValorTier;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a player's computed Valor tier changes (EVENTS.md). Fired synchronously on the thread
 * that mutated the score. Consumed by perk and chat-formatting features.
 */
public final class ValorRankChangedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final ValorTier oldTier;
    private final ValorTier newTier;

    /**
     * Creates the event.
     *
     * @param playerId the affected player
     * @param oldTier  the tier before the change
     * @param newTier  the tier after the change
     */
    public ValorRankChangedEvent(@NotNull UUID playerId, @NotNull ValorTier oldTier, @NotNull ValorTier newTier) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.oldTier = Objects.requireNonNull(oldTier, "oldTier");
        this.newTier = Objects.requireNonNull(newTier, "newTier");
    }

    /**
     * The affected player.
     *
     * @return player UUID
     */
    public @NotNull UUID getPlayerId() {
        return playerId;
    }

    /**
     * The tier before the change.
     *
     * @return the previous tier
     */
    public @NotNull ValorTier getOldTier() {
        return oldTier;
    }

    /**
     * The tier after the change.
     *
     * @return the new tier
     */
    public @NotNull ValorTier getNewTier() {
        return newTier;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Required by Bukkit's event system.
     *
     * @return the shared handler list
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
