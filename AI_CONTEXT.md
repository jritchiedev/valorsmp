# AI_CONTEXT.md — The Valor SMP

Purpose: give any AI agent, cold, enough context to start working productively without re-deriving decisions already made. Read this file in full before touching code.

---

## 1. What This Server Is

The Valor SMP is a survival multiplayer Minecraft server (Java Edition, PaperMC, targeting 1.21.11) with a persistent, competitive-but-fair PvP progression meta layered on top of vanilla survival: a "Valor" tier progression track earned exclusively through PvP, structured PvP combat rules, custom item mechanics (mace and spear cooldowns, a dragon-egg strength buff), leaderboards, custom chat formatting, staff rank/permission tiers, and periodic season resets. Two mods are added alongside the plugin: Simple Voice Chat and String Duper Returns.

It is intended to run continuously for a general playerbase (not a private friend group), which means:

- Data integrity matters (real player time/investment is at stake).
- Anti-abuse and permission correctness matter (see `SECURITY.md`).
- Performance matters at a scale of dozens to low hundreds of concurrent players, not just a handful.

## 2. What "Valor" Means (the core hook)

"Valor" is the server's central progression score. Full detail: `docs/valor-system.md`. In short: a player gains **+1 Valor Point** for killing another player and loses **1** when killed by a player (floored at 0). Every 4 Valor Points unlocks a new tier; the maximum tier is 5, and each tier grants better perks (extended potion durations, permanent speed/strength effects). Valor can **only** be earned from kills — there is no quest, event, or purchase path — and it is non-transferable and non-withdrawable.

## 3. Current System Status

| System | Status | Primary Doc |
|---|---|---|
| Valor progression | Planned / early design | `docs/progression.md`, `docs/valor-system.md` |
| Combat | Planned | `docs/combat.md` |
| Custom item mechanics (mace/spear cooldowns, dragon egg) | Planned | `docs/combat.md` (until promoted to its own doc) |
| Leaderboards | Planned | `docs/future-features.md` |
| Chat | Planned | `docs/future-features.md` |
| Ranks/Permissions (staff) | Planned | `docs/future-features.md` |
| Season resets | Planned | `docs/future-features.md` |

When a system moves from "Planned" to "In Progress," it should get its own `docs/<system>.md` file (promoted out of `future-features.md`), and this table should be updated in the same PR.

## 4. Non-Negotiable Constraints

- **Java 21**, PaperMC (not Spigot, not Bukkit-only — Paper-specific APIs are allowed and preferred when they simplify code, e.g., Paper's async chunk API, Adventure `Component` for text instead of legacy `ChatColor` strings).
- **Gradle Kotlin DSL** for the build.
- All player-facing text uses **Adventure `Component`**, not legacy `&`-color-coded strings, except inside YAML config values where a MiniMessage-style string is deserialized into a `Component` at load time.
- Valor scores are **never** represented as floating point — they are small non-negative integers. Persist and compute them with integer types, never `double`/`float`.
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
| Valor / Valor Score | The server's core progression score; small non-negative integer, earned only from PvP kills |
| Valor Tier | A tier (I–V) derived from Valor Score; every 4 points unlocks the next, each granting better perks |
| Staff Rank | A permission tier for staff, distinct from Valor Tier |
| Season | A bounded time period after which Valor scores/leaderboards reset and archive |
