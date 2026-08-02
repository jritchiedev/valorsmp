# ROADMAP.md — The Valor SMP

Prioritized, living plan for feature delivery. This is a planning artifact, updated as priorities shift — treat dates as targets, not commitments, and treat this document (not verbal/chat consensus) as the source of truth for "what are we building next."

---

## Milestone 0 — Foundation (Prerequisite for Everything Else)

- [ ] `CompositionRoot` / DI wiring bootstrap
- [ ] Database connection pooling + migration runner (`DATABASE.md`)
- [ ] `PlayerProfileService` (every other feature depends on a player having a profile row)
- [ ] Config loading/validation bootstrap (`CONFIGURATION.md`)
- [ ] CI pipeline stood up (`DEVOPS.md`)
- [ ] Base observability (logging conventions, `/valorsmp health`) (`OBSERVABILITY.md`)

**Exit criteria:** a player can join the server, get a profile row created, and the plugin enables/disables cleanly with zero errors, on a fresh database and on a database with prior test data.

## Milestone 1 — Core Progression & Combat

- [ ] `ValorScoreService` + tier computation (`docs/progression.md`, `docs/valor-system.md`)
- [ ] `CombatService` (kill/death tracking feeding the +1/-1 Valor award) (`docs/combat.md`)
- [ ] Tier perk application (extended potions, permanent speed/strength effects)
- [ ] Tier-based chat formatting (basic version)

**Exit criteria:** players can fight, gain/lose Valor, see their tier change and perks apply, all persisted correctly across restarts, with duplication-safety tests passing (`SECURITY.md` §9).

## Milestone 2 — Custom Item Mechanics

- [ ] Mace: 30-second cooldown; not enchantable via enchantment table or anvil
- [ ] Spears (netherite/diamond/gold/iron/copper/wood): 10-second cooldown for the `lunge` enchant
- [ ] Dragon egg: grants permanent Strength III while in inventory; cannot be placed in an ender chest
- [ ] `/speed set 1|2` command for Valor tier IV and V players

**Exit criteria:** each item behaves per `docs/combat.md`, cooldowns and restrictions are enforced server-side, and edge cases (dropping/relogging with the dragon egg, cooldown persistence) are tested.

## Milestone 3 — Social & Staff Systems

- [ ] Permission-tier `RankService` (staff ranks, distinct from Valor tier) and `ranks.yml`
- [ ] `LeaderboardService` (Valor score, PvP kills)

**Exit criteria:** staff rank-based permissions gate the intended actions, and leaderboards reflect real Valor/combat stats accurately.

## Milestone 4 — Mod Integration

- [ ] Simple Voice Chat (1.21.11) added alongside the plugin
- [ ] String Duper Returns (1.21.11) added alongside the plugin

**Exit criteria:** both mods load cleanly with the server and plugin, verified in a live test instance.

## Milestone 5 — Season Structure

- [ ] `SeasonService` (lifecycle, archival per `AGENTS.md` §12)
- [ ] Season-scoped Valor score and leaderboard resets

**Exit criteria:** a full season can start, run, and end with correct archival, no data loss, and no manual database intervention required.

## Milestone 6 — v1.0

- [ ] All of the above stable in live production for at least one full season with no data-integrity incidents.
- [ ] `API.md` surface finalized and stabilized (no more expected breaking changes without a documented major-version reason).
- [ ] Bump to `1.0.0` per `RELEASE.md` §1.

## Beyond v1.0 (Speculative — see `docs/future-features.md`)

Ideas not yet committed to a milestone: cross-server/proxy support, additional item mechanics, community-requested quality-of-life features. These stay in `docs/future-features.md` until a milestone above is created for them via a `DECISIONS.md`-backed prioritization decision.

## How This Document Is Maintained

- Updated whenever a milestone's scope changes materially, or when a milestone completes (move it to a "Shipped" section below with the release version it landed in).
- Individual tickets (`TICKETS.md` format) are the granular execution unit; this document tracks milestone-level grouping only.

## Shipped

*(Empty until Milestone 0 ships — populate with `## Milestone N — <name> — shipped in vX.Y.Z` entries going forward.)*
