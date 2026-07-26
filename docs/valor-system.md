# The Valor System

The central progression concept the server is named for. This document defines what Valor *is*, how it's earned/spent, and the integrity guarantees around it. For the mechanical progression track (ranks, thresholds, UI), see `docs/progression.md` — this document is the conceptual/design source of truth; `progression.md` is the mechanical implementation detail.

---

## 1. What Valor Is

Valor is a **non-purchasable, non-transferable score** representing a player's demonstrated skill and engagement on the server — earned through PvP victories, quest completion, and participation in timed server events. It is explicitly **not** the same thing as the server's economy currency (`docs/economy.md`), and the two must never be conflated in code, database schema, or player-facing UI.

| | Valor Score | Wallet Balance (Economy) |
|---|---|---|
| Earned via | PvP, quests, events | Trading, selling, quest/event rewards (some overlap in *source*, but tracked separately) |
| Purchasable with real money | No, never | Server-operator decision, out of scope for this doc |
| Transferable between players | No | Yes |
| Can go negative | No (floors at 0) | Server-configurable (default: no) |
| Primary purpose | Rank/prestige, cosmetic/permission gating | Trading, purchasing, economy gameplay |

## 2. Why Two Separate Currencies

Keeping Valor and economy currency separate preserves Valor's meaning as a *skill/engagement* signal rather than a *wealth* signal — a wealthy player who never fights or quests shouldn't be able to buy their way to a high Valor rank. This distinction is load-bearing for the server's competitive integrity and must not be eroded by future features (e.g., a "buy Valor with money" crate would violate this design principle and requires a `DECISIONS.md`-level reconsideration, not a quiet feature addition).

## 3. Earning Valor

| Source | Typical Amount | Notes |
|---|---|---|
| PvP kill (tracked combat) | Scales with victim's current Valor rank relative to killer's, to discourage farming low-rank players | See `docs/combat.md` for what counts as a "tracked" kill |
| Quest completion | Scales with quest tier (see `docs/future-features.md`'s quest section) | |
| Timed event participation/placement | Scales with event type and placement | |

Exact numeric formulas live in `progression.yml` config (`CONFIGURATION.md`), not hardcoded — this document describes the *shape* of the system, not the tunable constants, which change more often than the design.

## 4. Rank Derivation

A player's rank is a pure function of their current-season Valor score against configured thresholds (`progression.yml`). Rank is recomputed whenever score changes; a `ValorRankChangedEvent` fires only when the computed rank tier actually changes (not on every score change), per `EVENTS.md`.

## 5. Integrity Guarantees

Per `SECURITY.md` §9, Valor mutation is treated with the same rigor as currency:

- All mutations go through `ValorScoreService` — no direct repository writes from elsewhere.
- Score never goes negative (floored at 0 in the service layer, not just by convention).
- Every non-obvious mutation (event rewards, admin grants) is logged with enough detail to audit after the fact.
- Any change to the *scoring formula* itself (not just tunable constants) requires a `DECISIONS.md` entry, since it affects the server's core competitive fairness contract with players.

## 6. Season Scoping

Valor score is season-scoped (`valor_scores` table keyed by `(uuid, season)`, `DATABASE.md`). At season end, the current season's scores are archived (never destroyed) and a new season begins at zero for all players — see `docs/future-features.md`'s season section and `SeasonService` in `SERVICES.md`.

## 7. Open Questions

- Whether all-time (cross-season) Valor totals should be tracked and displayed alongside current-season score, for players who value long-term prestige separately from competitive-season standing. Not yet decided — flag as a `ROADMAP.md`/`DECISIONS.md` item before implementing either way, since it affects the `valor_scores` schema shape.
