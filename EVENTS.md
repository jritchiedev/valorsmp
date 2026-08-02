# EVENTS.md — The Valor SMP

Conventions and catalogue for **domain events** — custom Bukkit events representing feature-level occurrences, as distinct from raw Bukkit events (`BlockBreakEvent`, `PlayerJoinEvent`, etc.) which are Minecraft-mechanical, not domain-level.

---

## 1. Purpose

Domain events are the primary decoupling mechanism between features (`ARCHITECTURE.md` §10). A service fires an event describing *what happened*, in the past tense, as an immutable fact. It does not know or care who's listening.

## 2. Conventions

- Package: `net.thevalorsmp.<feature>.events`.
- Naming: `<Noun><PastTenseVerb>Event` — e.g., `PlayerKilledEvent`, `ValorRankChangedEvent`, `SeasonEndedEvent`. Never name an event with a present-tense or imperative verb (`AwardValorEvent` is wrong — that sounds like a request, not a fact).
- All fields are `final`, set in the constructor, exposed via getters only. Domain events are immutable records of fact — no setters.
- Extend `org.bukkit.event.Event`. Use `Event.isAsynchronous()` truthfully — if the firing service call happens off the main thread, the event must be constructed and fired accordingly, and listeners must be written knowing this (no Bukkit API calls requiring main-thread access without explicitly scheduling back onto it).
- Domain events are **not** cancellable by default (they represent something that already happened). If a genuine "should this be allowed" check is needed, that's a *pre-check* concern belonging in the service's validation logic, or a separately named `<Noun><Verb>RequestEvent`/`...PreEvent` if a legitimate use case requires other plugins to veto an in-progress action — document this distinction explicitly if introduced, since it's an exception to the "events are facts" rule.
- Every event class has a short Javadoc block: what happened, when it fires (relative to the service call), and what data it carries.

```java
/**
 * Fired after a player's computed Valor tier has changed.
 * Fired synchronously, on the thread that called ValorScoreService#awardKill.
 */
public final class ValorRankChangedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final ValorTier oldTier;
    private final ValorTier newTier;

    public ValorRankChangedEvent(UUID playerId, ValorTier oldTier, ValorTier newTier) {
        this.playerId = playerId;
        this.oldTier = oldTier;
        this.newTier = newTier;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public ValorTier getOldTier() {
        return oldTier;
    }

    public ValorTier getNewTier() {
        return newTier;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
```

## 3. When to Fire an Event vs. Call Directly

See `SERVICES.md` §4. Default to an event for cross-feature relationships. Use a direct service-to-service call only for a tight, intentional, documented dependency within the same conceptual boundary.

## 4. Event Catalogue

| Event | Fired By | Fired When | Key Data | Known Listeners |
|---|---|---|---|---|
| `PlayerProfileCreatedEvent` | `PlayerProfileService` | First-ever join, profile row created | `UUID playerId` | — |
| `PlayerKilledEvent` | `CombatService` | A player is killed by another player | `UUID victim`, `UUID killer`, `Location location` | `ValorScoreService`, `LeaderboardService` |
| `ValorRankChangedEvent` | `ValorScoreService` | Player's computed Valor tier changes | `UUID playerId`, `ValorTier oldTier`, `ValorTier newTier` | `ChatFormatService` |
| `PlayerRankChangedEvent` | `RankService` | Staff permission-tier changes (distinct from Valor tier) | `UUID playerId`, `String oldRank`, `String newRank` | `ChatFormatService` |
| `SeasonStartedEvent` | `SeasonService` | New season begins | `int seasonNumber` | Multiple (reset ephemeral leaderboard state) |
| `SeasonEndedEvent` | `SeasonService` | Season ends, archival triggered | `int seasonNumber` | `LeaderboardService` |

This table must be kept current — adding a new domain event without a corresponding row here is a documentation bug.

## 5. Listener Registration for Domain Events

Listeners consuming domain events live in the *consuming* feature's package, not the firing feature's — e.g., `ChatFormatService`'s reaction to `ValorRankChangedEvent` is registered as a listener inside the `chat` package, not inside `progression`. This keeps the firing feature genuinely unaware of its consumers, matching the decoupling goal.
