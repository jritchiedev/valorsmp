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

## Milestone 1 — Core Progression & Economy

- [ ] `ValorScoreService` + rank computation (`docs/progression.md`, `docs/valor-system.md`)
- [ ] `EconomyService` + wallet persistence (`docs/economy.md`)
- [ ] `CombatService` (basic PvP tagging, kill/death tracking feeding Valor) (`docs/combat.md`)
- [ ] Rank-based chat formatting (basic version)

**Exit criteria:** players can fight, earn Valor, see their rank change, and hold/spend currency, all persisted correctly across restarts, with duplication-safety tests passing (`SECURITY.md` §9).

## Milestone 2 — Land & Social Systems

- [ ] `LandClaimService` (creation, boundaries, flags)
- [ ] `TeamService` (creation, membership, shared claim access)
- [ ] Permission-tier `RankService` (distinct from Valor rank) and `ranks.yml`

**Exit criteria:** players can claim land individually or as a team, protections work correctly, and rank-based permissions gate the intended actions.

## Milestone 3 — Engagement Systems

- [ ] `QuestService` + initial quest set
- [ ] `AchievementService`
- [ ] `LeaderboardService`
- [ ] `EventSchedulerService` (timed server events, initial event types)

**Exit criteria:** players have ongoing reasons to log in beyond raw sandbox play; leaderboards reflect real Valor/economy/combat stats accurately.

## Milestone 4 — Rewards & Retention

- [ ] `CrateService` + initial crate types
- [ ] `CosmeticsService` (unlock + equip, reacting to Valor rank and economy events)
- [ ] `NpcInteractionService` (quest-giving NPCs at minimum)

**Exit criteria:** players have cosmetic progression goals layered on top of the core Valor/economy loop, with crate fairness auditable via `crate_open_log`.

## Milestone 5 — Season Structure

- [ ] `SeasonService` (lifecycle, archival per `AGENTS.md` §12)
- [ ] Season-scoped leaderboard resets
- [ ] Season-end rewards tied into `CrateService`/`CosmeticsService`

**Exit criteria:** a full season can start, run, and end with correct archival, no data loss, and no manual database intervention required.

## Milestone 6 — v1.0

- [ ] All of the above stable in live production for at least one full season with no data-integrity incidents.
- [ ] `API.md` surface finalized and stabilized (no more expected breaking changes without a documented major-version reason).
- [ ] Bump to `1.0.0` per `RELEASE.md` §1.

## Beyond v1.0 (Speculative — see `docs/future-features.md`)

Ideas not yet committed to a milestone: cross-server/proxy support, additional crate/cosmetic content cadence, expanded NPC dialogue systems, community-requested quality-of-life features. These stay in `docs/future-features.md` until a milestone above is created for them via a `DECISIONS.md`-backed prioritization decision.

## How This Document Is Maintained

- Updated whenever a milestone's scope changes materially, or when a milestone completes (move it to a "Shipped" section below with the release version it landed in).
- Individual tickets (`TICKETS.md` format) are the granular execution unit; this document tracks milestone-level grouping only.

## Shipped

*(Empty until Milestone 0 ships — populate with `## Milestone N — <name> — shipped in vX.Y.Z` entries going forward.)*
