package net.thevalorsmp.core.event;

import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Publishes domain events (EVENTS.md) on behalf of services. Abstracted behind an interface so
 * services can be unit-tested without a running Bukkit server (a test double collects events instead
 * of dispatching them through {@code PluginManager}).
 */
@FunctionalInterface
public interface DomainEventPublisher {

    /**
     * Dispatches a domain event to all registered listeners.
     *
     * @param event the event to publish; must be fired on the main server thread unless
     *              {@link Event#isAsynchronous()} is {@code true}
     */
    void publish(@NotNull Event event);
}
