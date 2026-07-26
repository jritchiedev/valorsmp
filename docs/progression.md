# Progression

The mechanical implementation of the Valor rank ladder: thresholds, display, and what rank gates. For the conceptual "what is Valor and why" discussion, see `docs/valor-system.md` — this document is the practical "how the ladder works" reference.

---

## 1. Rank Ladder (Example Shape)

Actual thresholds live in `progression.yml` (`CONFIGURATION.md`) and are expected to be tuned post-launch; the shape below illustrates the intended structure, not final numbers.

| Rank | Valor Threshold | Display Color | Gates |
|---|---|---|---|
| Unranked | 0 | Gray | — |
| Bronze | 100 | Bronze/orange | Bronze-tier cosmetics |
| Silver | 500 | Silver/white | Silver-tier cosmetics, Silver crate eligibility |
| Gold | 1,500 | Gold/yellow | Gold-tier cosmetics, Gold crate eligibility |
| Platinum | 4,000 | Cyan | Platinum-tier cosmetics, leaderboard highlight |
| Valor Champion | 10,000 | Custom gradient | Top-tier cosmetics, seasonal recognition (title, possibly a physical/digital reward at operator discretion — out of scope for this codebase) |

## 2. Rank Computation

- Pure function of current-season `valor_scores.score` against `progression.yml`'s configured thresholds — no hysteresis/stickiness by default (crossing back below a threshold demotes the displayed rank immediately), unless a "rank floor" feature is explicitly requested and designed (not currently planned; would need a `DECISIONS.md` entry given it changes the "rank always reflects current standing" simplicity).
- Computed on every Valor score mutation, but `ValorRankChangedEvent` only fires when the *tier* actually changes, not on every point gained (`EVENTS.md`).

## 3. Display

- Rank is shown in: chat prefix (via `ChatFormatService`, reacting to `ValorRankChangedEvent`), tab list, `/rank` command output, and leaderboard listings.
- Rank display uses Adventure `Component` with the configured color/style per rank tier — never legacy color codes (`CODING_STANDARDS.md` §9).

## 4. What Rank Gates

- Cosmetic unlock eligibility (`CosmeticsService` checks current rank on `ValorRankChangedEvent` and on-demand at equip time).
- Crate-type eligibility (some crate tiers require a minimum rank to open, even if the player has a key — `CrateService`).
- Optionally, specific permission nodes for rank-gated commands/areas, via `RankService`'s mapping (distinct from the permission-tier `RankService` used for staff ranks — naming collision risk flagged for `DECISIONS.md` if both concepts end up needing a class literally named `Rank`; consider `ValorRank` vs `StaffRank` as the disambiguated model names once both are implemented).

## 5. Season Reset Behavior

- At season end, current-season rank is archived alongside score (`docs/future-features.md`'s season section).
- New season starts every player at Unranked/0, per `docs/valor-system.md` §6.
- Whether all-time/lifetime rank achievements (e.g., "reached Valor Champion at least once") persist across seasons as a separate achievement-style record is the same open question flagged in `docs/valor-system.md` §7.

## 6. Class Responsibilities

| Class | Responsibility |
|---|---|
| `ValorScoreService` | Score mutation, rank computation, firing `ValorRankChangedEvent` |
| `Rank` (model) | Enum/value type for the ladder tiers, ordered, with threshold and display metadata loaded from config |
| `ValorRankChangedEvent` | Domain event carrying old/new rank for a player |
| `ChatFormatService` (consumer) | Reacts to rank changes to update cached chat prefix |
| `CosmeticsService` (consumer) | Reacts to rank changes to re-check unlock eligibility |
