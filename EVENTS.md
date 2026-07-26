# EVENTS.md — The Valor SMP

Conventions and catalogue for **domain events** — custom Bukkit events representing feature-level occurrences, as distinct from raw Bukkit events (`BlockBreakEvent`, `PlayerJoinEvent`, etc.) which are Minecraft-mechanical, not domain-level.

---

## 1. Purpose

Domain events are the primary decoupling mechanism between features (`ARCHITECTURE.md` §10). A service fires an event describing *what happened*, in the past tense, as an immutable fact. It does not know or care who's listening.

## 2. Conventions

- Package: `net.thevalorsmp.<feature>.events`.
- Naming: `<Noun><PastTenseVerb>Event` — e.g., `LandClaimCreatedEvent`, `PlayerKilledEvent`, `ValorRankChangedEvent`. Never name an event with a present-tense or imperative verb (`CreateClaimEvent` is wrong — that sounds like a request, not a fact).
- All fields are `final`, set in the constructor, exposed via getters only. Domain events are immutable records of fact — no setters.
- Extend `org.bukkit.event.Event`. Use `Event.isAsynchronous()` truthfully — if the firing service call happens off the main thread, the event must be constructed and fired accordingly, and listeners must be written knowing this (no Bukkit API calls requiring main-thread access without explicitly scheduling back onto it).
- Domain events are **not** cancellable by default (they represent something that already happened). If a genuine "should this be allowed" check is needed, that's a *pre-check* concern belonging in the service's validation logic, or a separately named `<Noun><Verb>RequestEvent`/`...PreEvent` if a legitimate use case requires other plugins to veto an in-progress action — document this distinction explicitly if introduced, since it's an exception to the "events are facts" rule.
- Every event class has a short Javadoc block: what happened, when it fires (relative to the service call), and what data it carries.

```java
/**
 * Fired after a land claim has been successfully created and persisted.
 * Fired synchronously, on the thread that called LandClaimService#createClaim.
 */
public final class LandClaimCreatedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LandClaim claim;

    public LandClaimCreatedEvent(LandClaim claim) {
        this.claim = claim;
    }

    public LandClaim getClaim() {
        return claim;
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
| `ValorRankChangedEvent` | `ValorScoreService` | Player's computed rank changes tier | `UUID playerId`, `Rank oldRank`, `Rank newRank` | `CosmeticsService`, `ChatFormatService` |
| `PlayerCombatTaggedEvent` | `CombatService` | Player enters PvP-tagged state | `UUID playerId`, `Instant expiresAt` | UI/scoreboard listeners |
| `PlayerKilledEvent` | `CombatService` | A player is killed by another player under tracked combat rules | `UUID victim`, `UUID killer`, `Location location` | `EconomyService`, `ValorScoreService`, `LeaderboardService` |
| `BalanceChangedEvent` | `EconomyService` | Wallet balance changes for any reason | `UUID playerId`, `long oldBalance`, `long newBalance`, `String reason` | `CosmeticsService` (unlock re-check) |
| `LandClaimCreatedEvent` | `LandClaimService` | New claim persisted | `LandClaim claim` | — |
| `LandClaimTransferredEvent` | `LandClaimService` | Claim ownership changes | `LandClaim claim`, `UUID previousOwner` | `TeamService` (if claim tied to team membership) |
| `TeamCreatedEvent` | `TeamService` | New team persisted | `Team team` | — |
| `TeamMemberJoinedEvent` | `TeamService` | Player added to team | `UUID teamId`, `UUID playerId` | `LandClaimService` (shared-claim access) |
| `QuestCompletedEvent` | `QuestService` | Quest marked complete for a player | `UUID playerId`, `String questId` | `AchievementService`, `EconomyService`, `ValorScoreService` |
| `AchievementUnlockedEvent` | `AchievementService` | Achievement unlocked | `UUID playerId`, `String achievementId` | `LeaderboardService` |
| `ServerEventStartedEvent` | `EventSchedulerService` | Timed server event begins | `String eventId`, `Instant startedAt` | Chat broadcast listener |
| `ServerEventEndedEvent` | `EventSchedulerService` | Timed server event ends | `String eventId`, `Instant endedAt`, results payload | `LeaderboardService` |
| `CosmeticEquippedEvent` | `CosmeticsService` | Player equips a cosmetic | `UUID playerId`, `String cosmeticId` | — |
| `CrateOpenedEvent` | `CrateService` | Crate opened, reward granted | `UUID playerId`, `String crateType`, reward payload | `LeaderboardService` (drop stats) |
| `PlayerRankChangedEvent` | `RankService` | Permission-tier rank changes (distinct from Valor rank) | `UUID playerId`, `String oldRank`, `String newRank` | `ChatFormatService` |
| `SeasonStartedEvent` | `SeasonService` | New season begins | `int seasonNumber` | Multiple (reset ephemeral leaderboard state) |
| `SeasonEndedEvent` | `SeasonService` | Season ends, archival triggered | `int seasonNumber` | `LeaderboardService`, `AchievementService` (season-scoped achievements) |

This table must be kept current — adding a new domain event without a corresponding row here is a documentation bug.

## 5. Listener Registration for Domain Events

Listeners consuming domain events live in the *consuming* feature's package, not the firing feature's — e.g., `CosmeticsService`'s reaction to `ValorRankChangedEvent` is registered as a listener inside the `cosmetics` package, not inside `progression`. This keeps the firing feature genuinely unaware of its consumers, matching the decoupling goal.
