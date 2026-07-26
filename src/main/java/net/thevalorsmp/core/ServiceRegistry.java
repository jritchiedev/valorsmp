package net.thevalorsmp.core;

/**
 * Holds the long-lived services created by {@link CompositionRoot} so the plugin lifecycle can shut
 * them down deterministically in reverse order of creation (ARCHITECTURE.md section 6).
 */
public final class ServiceRegistry {

    /**
     * Flushes and stops every registered service. Called from {@code onDisable} before the data
     * source is closed, so implementations may still perform synchronous writes.
     */
    public void shutdown() {
        // Services with shutdown semantics are stopped here as they are added.
    }
}
