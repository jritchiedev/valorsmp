package net.thevalorsmp.core.event;

import java.util.Objects;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Production {@link DomainEventPublisher} that dispatches events through the server's
 * {@code PluginManager}.
 */
public final class BukkitDomainEventPublisher implements DomainEventPublisher {

    private final Server server;

    /**
     * Creates a publisher bound to the given server.
     *
     * @param server the Bukkit server whose plugin manager dispatches events
     */
    public BukkitDomainEventPublisher(@NotNull Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public void publish(@NotNull Event event) {
        server.getPluginManager().callEvent(Objects.requireNonNull(event, "event"));
    }
}
