# DECISIONS.md — The Valor SMP

Architecture Decision Records (ADRs). Every nontrivial, hard-to-reverse decision gets an entry here — this is the project's institutional memory, specifically so that future contributors (human or AI) don't re-litigate settled questions or accidentally violate a past decision out of ignorance that it was ever made.

Per `AGENTS.md` §17, `DECISIONS.md` entries take precedence over general guidance in other documents when they conflict, but never over `SECURITY.md` or `AGENTS.md` itself.

---

## Format

```markdown
## ADR-<number>: <short title>

**Date:** YYYY-MM-DD
**Status:** Proposed | Accepted | Superseded by ADR-<n> | Deprecated

### Context
What situation/problem prompted this decision.

### Decision
What was decided, stated plainly.

### Consequences
What this makes easier, what this makes harder, what it forecloses.

### Alternatives Considered
What else was considered and why it wasn't chosen.
```

---

## ADR-001: Repository Pattern with Constructor-Injected DI Over a Framework (e.g. Spring)

**Date:** 2026-07-26
**Status:** Accepted

### Context
The plugin needs a way to structure business logic separately from Bukkit's event/plugin lifecycle for testability, and needs a dependency injection approach.

### Decision
Use a hand-wired, explicit `CompositionRoot` with constructor injection, rather than a full DI framework (Spring, Guice). See `ARCHITECTURE.md` §3.

### Consequences
- Easier: no framework learning curve, no classpath-scanning surprises, DI wiring is explicit and traceable by reading one file.
- Harder: wiring is manual — adding a new service means adding a line to `CompositionRoot`, not relying on auto-wiring. Considered an acceptable, even beneficial, tradeoff at this project's scale (explicitness over magic).

### Alternatives Considered
- Guice: lighter than Spring but still adds a dependency and a learning curve for marginal benefit at this scale.
- Spring Boot: fundamentally mismatched with a Bukkit plugin's lifecycle (Spring expects to own application startup; Bukkit's `onEnable` doesn't map cleanly).

---

## ADR-002: SQLite Default, MySQL Opt-In

**Date:** 2026-07-26
**Status:** Accepted

### Context
Need a storage backend that works with zero setup for small servers but scales to larger/clustered deployments.

### Decision
SQLite as the zero-config default; MySQL/MariaDB as an opt-in config switch for larger deployments. See `DATABASE.md` §1.

### Consequences
- Easier: new server operators get a working plugin with no database setup.
- Harder: repository SQL must remain reasonably portable between the two backends; backend-specific syntax must be isolated (`DATABASE.md` §1).

### Alternatives Considered
- MySQL-only: simpler codebase (one dialect) but a worse out-of-box experience for small server operators.
- Flat-file (YAML/JSON) storage: rejected for anything relational (claims, teams, transactions) due to poor query support and concurrency-safety concerns at scale.

---

## ADR-003: Long Minor-Units for Currency, Never Double

**Date:** 2026-07-26
**Status:** Accepted

### Context
Floating-point arithmetic introduces rounding errors that are unacceptable for currency/score values representing real player-perceived value.

### Decision
All currency and Valor score values are stored and calculated as `long` minor units (or `BigDecimal` where fractional-but-exact values are genuinely needed), never `double`/`float`. See `AI_CONTEXT.md` §4, `SECURITY.md` §9, `DATABASE.md` schema.

### Consequences
- Easier: exact arithmetic, no accumulated rounding drift over thousands of transactions.
- Harder: display formatting must explicitly convert minor units to major units for player-facing text (division/formatting logic centralized in `docs/economy.md`'s formatting utility, not scattered ad hoc).

### Alternatives Considered
- `double`: rejected outright — well-known unacceptable for money due to binary floating-point representation error.
- `BigDecimal` everywhere: viable, but `long` minor units are simpler and faster for the common case (whole-cent amounts); reserved `BigDecimal` for any future case needing sub-minor-unit precision, which hasn't arisen yet.

---

## ADR-004: Domain Events for Cross-Feature Coupling, Direct Calls Reserved for Tight Dependencies

**Date:** 2026-07-26
**Status:** Accepted

### Context
Many features (Combat, Economy, Progression, Cosmetics, Leaderboards) need to react to each other's outcomes without becoming a tangled web of direct dependencies.

### Decision
Default to firing a custom Bukkit domain event for cross-feature relationships; reserve direct service-to-service calls for genuinely tight, intentional dependencies (documented explicitly when used). See `ARCHITECTURE.md` §10, `SERVICES.md` §4.

### Consequences
- Easier: new features can react to existing ones without modifying the firing feature at all.
- Harder: tracing "what happens when X occurs" requires checking the event catalogue (`EVENTS.md`) rather than following a single call stack — mitigated by keeping `EVENTS.md` rigorously up to date.

### Alternatives Considered
- Direct calls everywhere: simpler to trace in an IDE, but creates a dependency web that makes the codebase fragile to change as features multiply.
- A generic pub/sub bus instead of Bukkit's own event system: rejected — Bukkit's event system already provides this, and reusing it means other plugins can subscribe to our domain events the standard way, for free.

---

*(Future ADRs are appended below this line, numbered sequentially, never renumbered or deleted — a superseded decision gets a new ADR marking the old one "Superseded by ADR-<n>," preserving history.)*
