# Future Features

Design notes for systems not yet promoted to their own `docs/<feature>.md` file. Each section here graduates to its own file (and this section is removed from this doc) once the system enters active implementation per `ROADMAP.md` — at which point `AI_CONTEXT.md` §3's status table must also be updated in the same PR.

---

## Land Claims

Players (or teams) can claim a bounded region of land, protected from other players' block breaking/placing, and configurable per-claim via flags (PvP, mob spawning, explosions — see `FEATURES.md`'s worked example, which already designs this flag system in full).

- Claims are created via `/claim` at the player's current location with a configurable default radius (`land-claims.yml`).
- Overlap with existing claims is rejected.
- Claims can be transferred to another player or to a team.
- A per-player claim count limit is configurable (`land-claims.yml: max-claims-per-player`).

## Teams

Persistent groups of players sharing claim access and (potentially) a shared team-visible chat channel and team-scoped leaderboard standing.

- Team creation, invite/join/leave, and a designated owner/admin role structure (`team_members.role`).
- Claims can be owned by a team instead of an individual (`land_claims.team_id`), granting all current members claim access per the claim's flags.
- Team disbanding is a destructive-adjacent operation (removes shared claim access) — requires confirmation (`/team disband confirm`) per the same spirit as `docs/economy.md` §5's large-transfer confirmation pattern.

## Quests

Structured objectives players complete for Valor and/or currency rewards (`docs/valor-system.md` §3, `docs/economy.md` §2).

- Quest *definitions* (objective type, reward amount, tier) are config-driven (`quests.yml`), allowing operators to add/tune quests without a code change.
- Quest *progress* per player is database-tracked (`quest_progress` table, `DATABASE.md`).
- Quest types anticipated: kill-N-of-X, gather-N-of-X, reach-location, complete-in-time-limit (event-tied).
- Idempotent completion: a quest can only be "completed" (and rewarded) once per its defined reset cadence (one-time, daily, weekly — config-driven), enforced server-side per `SECURITY.md` §8's anti-replay guidance.

## Achievements

One-time (or season-scoped) recognitions distinct from repeatable quests — milestone-style ("first PvP kill," "reached Gold rank," "opened 100 crates").

- Achievement *definitions* config-driven (`achievements.yml`, not yet created — will be added when this feature is promoted out of this doc).
- Unlock state is database-tracked (`achievements_unlocked` table).
- Achievements may grant cosmetic unlocks directly (a badge/title) rather than currency/Valor, keeping them distinct in purpose from quests.

## Events (Timed Server Events)

Scheduled, time-boxed server-wide happenings (e.g., a PvP tournament window, a double-Valor weekend, a scavenger hunt).

- `EventSchedulerService` manages lifecycle: scheduled → active → concluded, firing `ServerEventStartedEvent`/`ServerEventEndedEvent` (`EVENTS.md`).
- Event *types* are config-driven where the event's mechanics are generic enough to parameterize (e.g., "double Valor for duration X"); genuinely novel event mechanics (a full scavenger hunt with custom logic) require dedicated code, not just config.
- Historical event results are retained (`server_events.result_json`) for leaderboard/stat purposes even after the event concludes.

## Leaderboards

Read-mostly aggregation and display of player stats: Valor rank, PvP kills, quest completions, wealth (if made public — a server-operator privacy choice, `economy.yml`), crate opens.

- `LeaderboardService` reads from relevant repositories (no independent leaderboard table for most stats — computed from source-of-truth tables, with a caching layer for expensive aggregate queries, per `REPOSITORIES.md` §4).
- Season-scoped leaderboards reset at season boundary; historical season leaderboards remain viewable via archived data (`docs/valor-system.md` §6).

## Cosmetics

Purely cosmetic unlocks (particle trails, hats/cosmetic armor overlays, chat badges) gated by Valor rank, currency purchase, achievement unlock, or crate reward — potentially multiple paths to the same cosmetic.

- `CosmeticsService` checks eligibility across all unlock paths on-demand (equip attempt) and reactively (re-check on `ValorRankChangedEvent`/`BalanceChangedEvent`/`AchievementUnlockedEvent`).
- No cosmetic provides a gameplay advantage — this is a firm design principle to preserve PvP/progression fairness (a cosmetic sword trail must never be, or even appear to be, a stat boost).

## Crates

Lootbox-style reward containers, opened with keys earned or purchased.

- Crate *definitions* (drop table, weights, tier) config-driven (`crates.yml`).
- Every open is logged (`crate_open_log`) for drop-rate fairness auditing (`SECURITY.md` §8, `DATABASE.md` schema).
- No real-money purchase path is in scope for this codebase's design (avoiding loot-box-adjacent monetization controversy is a deliberate, if unstated-elsewhere, design stance — flag to a human decision-maker if a future ticket proposes real-money crate key sales, since it has legal/regulatory implications beyond this codebase's normal scope).

## Chat

Custom chat formatting reflecting rank (Valor and/or staff), team tag, and possibly per-message cosmetic flair (an equipped chat badge).

- `ChatFormatService` resolves the full prefix/format for a player at message-send time (not cached indefinitely, to reflect rank changes promptly) but cheaply (avoid a database call on every single chat message — cache with invalidation on the relevant domain events instead, per `REPOSITORIES.md` §4's caching guidance).

## Ranks / Permissions

Staff/operator permission tiers (distinct from Valor rank — see `docs/progression.md` §4's naming-collision flag), likely integrating with a standard permissions plugin (LuckPerms or similar) rather than reinventing permission management from scratch — this plugin's `RankService` would then be a thin layer translating staff rank into this plugin's own feature-gating decisions, delegating the actual permission-node storage to the established permissions plugin via its API. Decision on exact integration approach deferred to a `DECISIONS.md` entry once this feature is scheduled.

## NPCs

Non-player characters (via Citizens or a similar NPC plugin, likely, rather than building NPC rendering from scratch) serving as quest-givers, lore/dialogue sources, or shop fronts.

- `NpcInteractionService` resolves what dialogue/quest-hook fires on interaction, delegating actual NPC entity management to whatever NPC plugin dependency is chosen (again, a `DECISIONS.md`-worthy dependency choice once scheduled).

## Season Resets

Periodic (e.g., quarterly) reset of season-scoped stats (Valor score, season leaderboards) with archival, not destruction, of prior data (`docs/valor-system.md` §6, `DATABASE.md`'s `season_archives` table).

- `SeasonService` orchestrates the reset: archive current data → reset season-scoped tables to fresh state → fire `SeasonEndedEvent` then `SeasonStartedEvent` → other features react (e.g., `AchievementService` may grant a season-specific achievement based on final standing before the reset takes effect).
- Non-season-scoped data (claims, teams, cosmetic unlocks, currency balance) is explicitly **not** reset by a season boundary — only Valor score and Valor-derived leaderboards are season-scoped, per current design intent. Any expansion of what's season-scoped is a `DECISIONS.md`-level product decision, not an implementation detail to decide casually mid-build.

---

## Promotion Checklist

When any system above moves from "planned" to "in progress" per `ROADMAP.md`:

1. Create `docs/<feature>.md` using the `FEATURES.md` template structure adapted to a full feature (not just the worked-example flag addition).
2. Remove (or substantially shrink to a cross-reference) this file's corresponding section.
3. Update `AI_CONTEXT.md` §3's status table.
4. Update `SERVICES.md`/`REPOSITORIES.md`/`EVENTS.md` catalogues with concrete entries once real classes exist (they already contain forward-looking placeholder-free entries for planned services — confirm those entries still match the actual as-built design, and correct them if implementation diverged from the original plan).
