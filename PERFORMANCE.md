# PERFORMANCE.md — The Valor SMP

Performance budget, common pitfalls specific to Paper/Minecraft plugins, and the process for investigating/fixing regressions.

---

## 1. The Budget

Minecraft's server tick runs at a target of 20 ticks/second (50ms/tick). Anything this plugin does on the main thread competes with world simulation, entity ticking, and every other plugin for that 50ms budget.

- **Hard rule:** no synchronous database I/O, no synchronous network I/O, and no unbounded computation on the main thread from within a listener or command. If a repository call is needed in response to a Bukkit event, either (a) the operation is proven trivially fast and bounded (a cached in-memory lookup), or (b) it's dispatched async and the result is applied back on the main thread via the scheduler once available.
- Per-event-handler target: sub-millisecond for anything registered on a high-frequency event (`PlayerMoveEvent`, `BlockPhysicsEvent`-adjacent checks). Anything heavier needs a different strategy (throttling, caching, moving logic to a lower-frequency check).

## 2. Common Pitfalls (Minecraft-Plugin-Specific)

- **`PlayerMoveEvent` fires extremely often** (multiple times per tick per moving player). Never do a database call, a full claim-lookup scan, or any non-trivial computation directly in a `PlayerMoveEvent` handler without first cheaply checking "did the player actually change block/chunk position" before doing the expensive part.
- **`BlockPhysicsEvent` fires enormously often** and is a well-known performance trap; avoid subscribing to it at all unless genuinely necessary, and if necessary, exit as early as possible for the overwhelming majority of irrelevant invocations.
- **N+1 repository calls**: iterating a list of players and calling a repository method once per player inside the loop, instead of a single batched query. Batch wherever the repository interface supports it (add a batch method if a hot path needs one — don't force a service into N+1 just because only a single-item method exists).
- **Unbounded caches**: an in-memory `Map` that grows with every player who's ever joined and is never evicted is a slow memory leak over months of uptime. Every cache has a bound (`Caffeine`-style max-size/TTL cache, not a raw `HashMap` used as a cache).
- **Synchronous chunk loading**: use Paper's async chunk API instead of blocking `getChunkAt` calls wherever the calling context allows deferring the result.
- **Excessive `Bukkit.getOnlinePlayers()` iteration** on a hot path when only nearby players matter — use a spatial index (Bukkit's `getNearbyEntities`, or the plugin's own claim/region index) instead of iterating everyone online.

## 3. Async Boundary Discipline

```java
// WRONG — blocks the main thread on a DB call
@EventHandler
public void onJoin(PlayerJoinEvent event) {
    PlayerProfile profile = repository.findByUuid(event.getPlayer().getUniqueId())
        .orElseGet(() -> repository.save(new PlayerProfile(event.getPlayer().getUniqueId())));
    // ...
}

// RIGHT — dispatch async, apply result back on main thread
@EventHandler
public void onJoin(PlayerJoinEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    asyncExecutor.submit(() -> {
        PlayerProfile profile = profileService.loadOrCreateProfile(uuid);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                applyProfileToPlayer(player, profile);
            }
        });
    });
}
```

Note the null-check after hopping back to the main thread — the player may have disconnected while the async work was in flight. This pattern (dispatch async → re-check player is still online → apply on main thread) is the standard shape for any join-time or command-triggered repository access in this codebase.

## 4. Measuring Before Optimizing

- Never optimize based on a guess. Use Paper's `/timings` (or its current equivalent) or a lightweight in-code timer around the suspected hot path to confirm where time is actually going before changing anything.
- For anything claimed as a performance fix in a PR description, state the actual before/after measurement, not an estimate (`AI_RULES.md` §1.7, `TESTING.md` §6).
- JMH micro-benchmarks are appropriate for isolated algorithmic hot spots (e.g., "which data structure is faster for leaderboard top-N lookups at N=10,000 players") but overkill for most feature-level performance questions, where a real-server before/after TPS/MSPT comparison is more representative and less effort.

## 5. Memory

- Prefer primitive collections / well-sized initial capacities for collections known to hold a bounded, predictable number of items (e.g., a claim's flag map has a small, known upper bound — no need for a `HashMap` sized for thousands of entries).
- Watch for accidental retention: a listener holding a reference to a `Player` object past their session (memory leak risk if the reference outlives the player's actual session and Bukkit's own player object lifecycle) — hold `UUID`s in long-lived structures, resolve to a live `Player` object only when needed and only if currently online.

## 6. Performance Review in PRs

Any PR touching combat resolution, any per-tick or high-frequency listener, or claim/region lookup logic must include a brief performance note in the PR description: what was measured (if anything changed on a hot path), and confirmation that no new synchronous I/O was introduced on the main thread. Reviewers treat a missing performance note on a hot-path PR as a blocking review comment, per `REVIEW.md`.

## 7. Escalation

If a live performance issue is suspected in production (TPS drops correlating with a specific feature), follow the debugging prompt template in `PROMPTS.md` ("Optimize Performance") and treat root-cause identification as higher priority than a fast guess-and-patch, per `AGENTS.md` §12's risk-management principle — a wrong guess that doesn't fix the real bottleneck wastes more time than a slightly slower, correct diagnosis.
