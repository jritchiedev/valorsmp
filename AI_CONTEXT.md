# AI_CONTEXT.md — The Valor SMP

Purpose: give any AI agent, cold, enough context to start working productively without re-deriving decisions already made. Read this file in full before touching code.

---

## 1. What This Server Is

The Valor SMP is a survival multiplayer Minecraft server (Java Edition, PaperMC) with a persistent, competitive-but-fair progression meta layered on top of vanilla survival: a "Valor" rank/progression track, structured PvP combat rules, a player-driven economy, land claiming, teams, quests, achievements, timed events, leaderboards, cosmetics, crates, custom chat formatting, ranks/permissions tiers, NPC-driven interactions, and periodic season resets.

It is intended to run continuously for a general playerbase (not a private friend group), which means:

- Data integrity matters (real player time/investment is at stake).
- Anti-abuse and permission correctness matter (see `SECURITY.md`).
- Performance matters at a scale of dozens to low hundreds of concurrent players, not just a handful.

## 2. What "Valor" Means (the core hook)

"Valor" is the server's central progression currency/score, distinct from the economy currency. Full detail: `docs/valor-system.md`. In short: players earn Valor through PvP victories, quest completion, and event participation; Valor determines rank tier, which gates cosmetic unlocks, crate tiers, and certain permissions. Valor is **not** directly purchasable and is **not** the same thing as in-game money — keep these concepts separate in code (`ValorScore` vs `Wallet`/`Balance`).

## 3. Current System Status

| System | Status | Primary Doc |
|---|---|---|
| Valor progression | Planned / early design | `docs/progression.md`, `docs/valor-system.md` |
| Combat | Planned | `docs/combat.md` |
| Economy | Planned | `docs/economy.md` |
| Land claims | Planned | `docs/future-features.md` (until promoted to its own doc) |
| Teams | Planned | `docs/future-features.md` |
| Quests | Planned | `docs/future-features.md` |
| Achievements | Planned | `docs/future-features.md` |
| Events (timed) | Planned | `docs/future-features.md` |
| Leaderboards | Planned | `docs/future-features.md` |
| Cosmetics | Planned | `docs/future-features.md` |
| Crates | Planned | `docs/future-features.md` |
| Chat | Planned | `docs/future-features.md` |
| Ranks/Permissions | Planned | `docs/future-features.md` |
| NPCs | Planned | `docs/future-features.md` |
| Season resets | Planned | `docs/future-features.md` |

When a system moves from "Planned" to "In Progress," it should get its own `docs/<system>.md` file (promoted out of `future-features.md`), and this table should be updated in the same PR.

## 4. Non-Negotiable Constraints

- **Java 21**, PaperMC (not Spigot, not Bukkit-only — Paper-specific APIs are allowed and preferred when they simplify code, e.g., Paper's async chunk API, Adventure `Component` for text instead of legacy `ChatColor` strings).
- **Gradle Kotlin DSL** for the build.
- All player-facing text uses **Adventure `Component`**, not legacy `&`-color-coded strings, except inside YAML config values where a MiniMessage-style string is deserialized into a `Component` at load time.
- Money and Valor scores are **never** represented as floating point in persistence or in any calculation that affects balances — use `long` minor-units (e.g., cents) or `BigDecimal` consistently per `docs/economy.md`, never `double`.
- Season resets must be able to archive rather than destroy prior-season data (see `docs/future-features.md` and `DATABASE.md`).

## 5. Things That Look Like Good Ideas But Aren't (Learned Constraints)

- Do not put gameplay balance numbers as magic literals in Java code — they belong in config (`CONFIGURATION.md`), even if "nobody will ever change them." Server operators tune these constantly in practice.
- Do not use `BlockPhysicsEvent` or other extremely high-frequency events for anything beyond the cheapest possible checks — they fire enormously often and are a well-known performance trap.
- Do not assume single-server. Even though The Valor SMP currently runs as a single Paper instance, avoid designs that would be painful to later put behind a proxy (e.g., avoid storing authoritative state only in a static in-memory map with no persistence path).

## 6. How to Use the Rest of This Repository

1. Start here (`AI_CONTEXT.md`) for orientation.
2. Read `AGENTS.md` for process rules.
3. Read `ARCHITECTURE.md` and `CODING_STANDARDS.md` before writing code.
4. Read the specific `docs/<feature>.md` for the feature you're touching.
5. Check `DECISIONS.md` for any prior ruling that affects your task.
6. Check `TICKETS.md`-formatted ticket for acceptance criteria.
7. Follow `AI_WORKFLOW.md` for the lifecycle, `TESTING.md` for coverage expectations, and `REVIEW.md`/PR conventions in `AGENTS.md` §10 before submitting.

## 7. Glossary

| Term | Meaning |
|---|---|
| Valor / Valor Score | The server's core progression score, distinct from economy currency |
| Wallet / Balance | Economy currency held by a player |
| Rank | A tier derived from Valor Score, gates cosmetics/permissions |
| Claim | A protected land region owned by a player or team |
| Team | A persistent group of players sharing claims/permissions |
| Season | A bounded time period after which certain stats/leaderboards reset and archive |
| Crate | A lootbox-style reward container, opened with a key |
| Tag (combat) | Temporary PvP-flagged state preventing logout-to-escape |
