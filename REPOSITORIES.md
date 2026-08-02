# REPOSITORIES.md — The Valor SMP

Conventions and catalogue for the data-access (Repository pattern) layer. See `ARCHITECTURE.md` §2.3 for architectural role. For the *Git* repository, see `REPOSITORY.md` — same word, different meaning, don't confuse the two documents.

---

## 1. What Belongs in a Repository

A repository is responsible for exactly one thing: **translating between domain models and persisted storage.** That includes:

- CRUD-style queries for its aggregate.
- Mapping `ResultSet` rows to domain model objects and back.
- Managing prepared statements / query construction.
- Owning the SQL schema for its table(s) (in coordination with `DATABASE.md`'s migration files).

A repository does **not** decide whether an operation is *allowed* — that's the service's job. A repository method like `addScore(uuid, delta)` writes whatever delta it's given; it doesn't decide whether the score should be floored at 0 (that's `ValorScoreService`).

## 2. Interface Convention

- Every aggregate gets an interface: `<Noun>Repository`.
- Production implementation: `Sql<Noun>Repository` (backend-agnostic SQL where possible; see `DATABASE.md` for backend-specific handling).
- Test implementation: `InMemory<Noun>Repository`, living in `src/test/java`, used by service unit tests so they never require a real database connection.

```java
public interface ValorScoreRepository {

    Optional<Integer> findScore(UUID playerId, int season);

    /**
     * Atomically applies {@code delta} to the player's current-season score and
     * returns the new value. Flooring at 0 is the service's responsibility.
     */
    int addScore(UUID playerId, int season, int delta);

    /**
     * @return the top {@code n} players by score for the given season,
     *         highest first. Used for leaderboard display.
     */
    List<ScoreEntry> topN(int season, int n);
}
```

## 3. Async Considerations

- Repository methods performing real I/O are safe to call from off the main server thread — and services calling them should do so from off-thread wherever the calling context allows (see `PERFORMANCE.md` for main-thread budget guidance).
- Repository methods must **never** themselves silently jump back onto the main thread or schedule Bukkit tasks — that's the caller's responsibility, since only the caller (typically a listener, via a service) knows whether the result needs to touch Bukkit API objects that require main-thread access.
- Repositories expose synchronous methods (return the value directly, or throw); they do not return `CompletableFuture` themselves as a rule, to keep the abstraction simple — callers wrap repository calls in async execution using the plugin's shared async executor, documented in `PERFORMANCE.md`. (Exception: if a specific repository's backend has a natively async driver and the sync wrapper would be meaningfully wasteful, this can be revisited via a `DECISIONS.md` entry.)

## 4. Caching

- Any read-heavy repository may implement an internal cache (e.g., a `CachingValorScoreRepository` decorator wrapping `SqlValorScoreRepository`), but the cache is an implementation detail behind the same interface — services are never aware a cache exists.
- Cache invalidation happens on every write path through the same repository instance; there is no separate "invalidate cache" method exposed to services.
- Any cache must have a bounded size or TTL. Unbounded caches are a `SECURITY.md`/`PERFORMANCE.md` concern (memory exhaustion over a long-running server uptime).

## 5. Repository Catalogue

| Repository | Backing Table(s) | Key Queries | Notes |
|---|---|---|---|
| `PlayerProfileRepository` | `player_profiles` | `findByUuid`, `save` | One row per player, created on first join |
| `ValorScoreRepository` | `valor_scores` | `findScore`, `addScore` (atomic), `topN` (leaderboard) | Season-scoped; see `DATABASE.md` for season partitioning |
| `RankRepository` | `player_ranks` | `findByUuid`, `setRank` | Staff rank *definitions*/permission mappings live in config |
| `SeasonRepository` | `seasons`, `season_archives` | `findCurrent`, `archiveSeason` | Archival, never destructive deletion, per `AGENTS.md` §12 |

Full schema (columns, types, indexes, migrations) lives in `DATABASE.md` — this table is the responsibility index, not the schema itself.

## 6. Testing Expectations

- Every production `Sql<Noun>Repository` has an integration test running against a real (in-memory or temp-file) SQLite instance, verifying actual SQL correctness — not just mocked behavior.
- Every repository interface has an `InMemory<Noun>Repository` test double kept behaviorally consistent with the real implementation (same query semantics), used across all service unit tests for that aggregate.
- Repository-level tests do not test business rules — only that data goes in and comes back out correctly, including edge cases like concurrent score adjustment (`addScore` must be atomic — test for race conditions where feasible, e.g., via a transaction-level `UPDATE ... SET score = score + ?` rather than read-modify-write).
