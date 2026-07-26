# API.md — The Valor SMP

Documentation for the **public API surface** exposed to other plugins (not the internal service interfaces used within this codebase — those are covered in `SERVICES.md`). If another plugin wants to read Valor scores, check wallet balances, or react to a rank change, this is what it uses.

---

## 1. Design Principles

- **Minimal surface.** Expose only what's proven necessary by an actual consuming use case. Speculative exposure is avoided — see `AI_RULES.md` §3, scope discipline.
- **Interface-first.** The public API is a set of interfaces in `net.thevalorsmp.api`, obtained through a single entry point, never concrete implementation classes.
- **No internal/NMS types.** Nothing in `net.thevalorsmp.api` references internal package classes, Bukkit-internal (`org.bukkit.craftbukkit.*`) types, or anything not part of Paper's public API.
- **Versioned and stable.** Once shipped, a method signature in this API is not changed or removed without a documented deprecation window (Section 4).

## 2. Entry Point

```java
public final class ValorAPI {

    private static ValorAPIProvider provider;

    public static ValorAPIProvider get() {
        if (provider == null) {
            throw new IllegalStateException(
                "ValorAPI accessed before ValorPlugin finished enabling.");
        }
        return provider;
    }
}
```

Consuming plugins should declare a **soft or hard dependency** on `TheValorSMP` in their `plugin.yml` and only call `ValorAPI.get()` from their own `onEnable` (after Bukkit guarantees load order for hard dependencies) or from a listener on `ServerLoadEvent` for soft dependencies, to avoid the "accessed before enabling" failure mode.

## 3. Exposed Interfaces

### `ValorScoreQuery`
```java
public interface ValorScoreQuery {
    long getScore(UUID playerId, int season);
    long getCurrentSeasonScore(UUID playerId);
    Rank getRank(UUID playerId);
}
```
Read-only. There is deliberately no public `addScore`/`setScore` method — score mutation is exclusively internal, to preserve the integrity guarantees in `docs/valor-system.md`. If a legitimate external use case for granting Valor emerges (e.g., a minigame plugin awarding Valor for its own events), that requires a `DECISIONS.md` entry and a carefully scoped, audited mutation method — not an open one.

### `EconomyQuery`
```java
public interface EconomyQuery {
    long getBalanceMinorUnits(UUID playerId);
    boolean hasAtLeast(UUID playerId, long amountMinorUnits);
}
```
Read-only for the same reason as above. Balance mutation from external plugins, if ever needed, is a separate, explicitly-scoped `EconomyMutator` interface requiring its own `DECISIONS.md` entry, permission gating, and audit logging — not present in v1 of this API.

### `ClaimQuery`
```java
public interface ClaimQuery {
    Optional<ClaimInfo> getClaimAt(Location location);
    boolean isClaimedBy(Location location, UUID playerId);
}
```

### `RankQuery`
```java
public interface RankQuery {
    String getRankKey(UUID playerId);
    Component getFormattedRankDisplay(UUID playerId);
}
```

### Event Subscription
External plugins subscribe to any domain event listed in `EVENTS.md` the normal Bukkit way (`@EventHandler`, `PluginManager#registerEvents`) — domain events are already public Bukkit `Event` classes, so no special API wrapper is needed for read/react use cases. The `ValorAPI` query interfaces exist specifically for *pull*-style access (asking a question right now), while events serve *push*-style access (reacting to something that happened).

## 4. Deprecation Policy

- Deprecated methods are annotated `@Deprecated(since = "X.Y.Z", forRemoval = true)` with Javadoc pointing to the replacement.
- A deprecated method remains functional for at least one minor version after deprecation before removal, per `RELEASE.md`'s SemVer policy.
- Breaking changes to `net.thevalorsmp.api` require a major version bump and a `DECISIONS.md` entry explaining why the break was necessary.

## 5. What Is Explicitly NOT Public API

- Anything in `net.thevalorsmp.core`, `.landclaims` (implementation packages), `.economy` (implementation), etc. — only `net.thevalorsmp.api` is the contract. Consuming plugins reaching into internal packages via reflection do so at their own risk with zero compatibility guarantee.
- Direct database access. External plugins never connect to The Valor SMP's database directly; all access goes through the API or events.
- Any Bukkit-internal or NMS handle.

## 6. Versioning of This API

Tracked independently in comments within `net.thevalorsmp.api.ValorAPIVersion`, following the same SemVer scheme as the plugin overall (`RELEASE.md`), but called out explicitly in release notes whenever the API surface itself changes, even if the plugin's user-facing version bump is otherwise minor.
