# SERVICES.md — The Valor SMP

Conventions and catalogue for the service layer. See `ARCHITECTURE.md` §2.2 for the layer's architectural role; this document is the practical how-to and the living catalogue of services as they're built.

---

## 1. What Belongs in a Service

A service owns **business rules and orchestration** for one feature/aggregate. If you're asking "is X allowed", "what should happen when Y", or "combine data from two repositories to answer Z" — that's a service.

A service does **not** own: SQL, YAML parsing, Bukkit event registration, or player-facing text formatting (that last one belongs in the listener/command, using data the service returns).

## 2. Service Interface Conventions

- Every service is defined as an interface plus exactly one production implementation, even if a second implementation is never expected — this keeps the DI wiring uniform and makes future test doubles trivial.
- Naming: `<Noun>Service` (e.g., `ValorScoreService`, `CombatService`, `SeasonService`).
- Methods are named as verbs describing the business operation, not the underlying mechanics: `awardKillValor(...)`, not `updateScoreRow(...)`.
- Return `Optional<T>` for "may not exist" results. Return a dedicated `Result` type (e.g., `ValorAwardResult` with a sealed set of outcomes: `Awarded`, `TierChanged`, `AlreadyAtFloor`, etc.) for operations with multiple meaningful failure modes that the caller needs to distinguish and message differently — do not overload exceptions for expected, common failure paths.
- Throw exceptions only for genuinely exceptional conditions (a repository I/O failure), not for expected business outcomes (a player attempting an action their Valor tier doesn't permit).

```java
public interface ValorScoreService {

    /**
     * Awards a kill: +1 Valor Point to {@code killer}, -1 to {@code victim}
     * (floored at 0), applying any resulting tier perk changes.
     *
     * @return a result describing the new scores and whether either player's
     *         tier changed. Never returns null.
     */
    ValorAwardResult awardKill(UUID killer, UUID victim);

    /**
     * @return the current Valor score for {@code player}, if a profile exists.
     */
    Optional<Integer> currentScore(UUID player);
}
```

## 3. Construction & Dependencies

- Constructor injection only. All dependencies (repositories, other services, config objects, a `Clock`/time-source abstraction for testability) are `final` fields set in the constructor.
- Never inject `ValorPlugin` itself into a service — that's a smell indicating the service is reaching for something that should be an explicit, narrower dependency.
- Inject a `Clock` (java.time) rather than calling `Instant.now()`/`System.currentTimeMillis()` directly, wherever time matters for business logic (cooldowns, expiry), so tests can control time deterministically.

## 4. Cross-Service Composition

Two legitimate patterns:

1. **Direct call** — Service A's constructor takes Service B's interface, and calls it as a deliberate, documented dependency (e.g., `CombatService` calling `ValorScoreService.awardKill(...)`). Use when the relationship is essential and synchronous by nature.
2. **Domain event** — Service A fires an event; Service B (via its own listener) reacts independently. Use when the relationship is "B cares about A's outcome, but A doesn't need to know or care that B exists." This is the default for cross-feature relationships (see `ARCHITECTURE.md` §10).

Never let two services depend on each other directly in both directions — that's a cycle and indicates a missing third abstraction or a case that should be event-based instead.

## 5. Service Catalogue

This table is updated as services are built. It is the authoritative index — if a service exists in code but isn't listed here, that's a documentation bug to fix in the same PR that's touching it.

| Service | Package | Responsibility | Depends On | Fires Events |
|---|---|---|---|---|
| `PlayerProfileService` | `core.profile` | Load/create/update the per-player profile record | `PlayerProfileRepository` | `PlayerProfileCreatedEvent` |
| `ValorScoreService` | `progression` | Award/deduct Valor score on kills/deaths, compute tier from score, apply/remove tier perks | `ValorScoreRepository`, `ValorTierConfig` | `ValorRankChangedEvent` |
| `CombatService` | `combat` | Resolve PvP kill/death bookkeeping (drives the +1/-1 Valor award) | `ValorScoreService`, `CombatConfig` | `PlayerKilledEvent` |
| `LeaderboardService` | `leaderboards` | Aggregate/rank player stats (Valor score, PvP kills) for display | Read replicas of relevant repositories | — (read-mostly, no events) |
| `ChatFormatService` | `chat` | Resolve a player's chat format based on Valor tier / staff rank | `RankService` | — |
| `RankService` | `ranks` | Staff permission-tier assignment, rank metadata | `RankRepository` | `PlayerRankChangedEvent` |
| `SeasonService` | `seasons` | Season lifecycle, triggers archival | Multiple repositories (read), `SeasonRepository` (write) | `SeasonEndedEvent`, `SeasonStartedEvent` |

Status of each service's implementation is tracked in `ROADMAP.md`, not here — this table describes intended responsibility regardless of build status.

## 6. Testing Expectations

Every service in the catalogue above must have a corresponding unit test class (`<ServiceName>Test`) once implemented, per `TESTING.md`. Services with nontrivial cross-service composition (`CombatService` → `ValorScoreService`) need explicit tests for the composition, not just each dependency in isolation.
