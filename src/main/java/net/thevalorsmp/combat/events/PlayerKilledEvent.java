package net.thevalorsmp.combat.events;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a player is killed by another player (EVENTS.md). Fired synchronously on the main
 * thread from the death listener, before the Valor award is applied, so consumers observe the fact
 * of the kill independently of scoring.
 */
public final class PlayerKilledEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID victim;
    private final UUID killer;
    private final Location location;

    /**
     * Creates the event.
     *
     * @param victim   the killed player
     * @param killer   the killing player
     * @param location where the death occurred
     */
    public PlayerKilledEvent(@NotNull UUID victim, @NotNull UUID killer, @NotNull Location location) {
        this.victim = Objects.requireNonNull(victim, "victim");
        this.killer = Objects.requireNonNull(killer, "killer");
        this.location = Objects.requireNonNull(location, "location").clone();
    }

    /**
     * The killed player.
     *
     * @return victim UUID
     */
    public @NotNull UUID getVictim() {
        return victim;
    }

    /**
     * The killing player.
     *
     * @return killer UUID
     */
    public @NotNull UUID getKiller() {
        return killer;
    }

    /**
     * Where the death occurred.
     *
     * @return a copy of the death location
     */
    public @NotNull Location getLocation() {
        return location.clone();
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
