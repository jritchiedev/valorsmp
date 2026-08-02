package net.thevalorsmp.progression;

import java.util.ArrayList;
import java.util.List;
import net.thevalorsmp.core.event.DomainEventPublisher;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/** Collects published domain events instead of dispatching them, for service unit tests. */
final class RecordingDomainEventPublisher implements DomainEventPublisher {

    private final List<Event> published = new ArrayList<>();

    @Override
    public void publish(@NotNull Event event) {
        published.add(event);
    }

    List<Event> published() {
        return published;
    }
}
