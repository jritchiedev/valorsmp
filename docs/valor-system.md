# The Valor System

The central progression concept the server is named for. This document defines what Valor *is*, how it's earned/lost, and the integrity guarantees around it. For the mechanical progression track (tiers, thresholds, perks), see `docs/progression.md` — this document is the conceptual/design source of truth; `progression.md` is the mechanical implementation detail.

---

## 1. What Valor Is

Valor is a **non-purchasable, non-transferable score** representing a player's demonstrated PvP skill. Valor can **only** be gained by killing other players — there is no quest, event, shop, or any other path to earning it. Valor Points cannot be paid to, traded with, or withdrawn by a player's Minecraft character.

- **On a kill:** the killer gains **+1 Valor Point** and sees: `You've gained 1 Valor Point from killing a player. You now have (current valor) Valor Points.`
- **On a death to a player:** the victim loses **-1 Valor Point** and sees: `You've lost 1 Valor Point due to dying. You now have (current valor) Valor Points.`
- Every 4 Valor Points unlocks a new **tier** of Valor, granting better perks. The maximum tier is 5. See `docs/progression.md` for the tier ladder and perks.

## 2. Tier Derivation

A player's tier is a pure function of their current-season Valor score against the fixed thresholds (see `docs/progression.md`). Tier is recomputed whenever the score changes; a `ValorRankChangedEvent` fires only when the computed tier actually changes (not on every score change), per `EVENTS.md`.

## 3. Integrity Guarantees

Valor mutation is treated with rigor per `SECURITY.md`:

- All mutations go through `ValorScoreService` — no direct repository writes from elsewhere.
- Score never goes negative (floored at 0 in the service layer, not just by convention).
- Every non-obvious mutation (admin grants) is logged with enough detail to audit after the fact.
- Any change to the *scoring rule* itself requires a `DECISIONS.md` entry, since it affects the server's core competitive fairness contract with players.

## 4. Season Scoping

Valor score is season-scoped (`valor_scores` table keyed by `(uuid, season)`, `DATABASE.md`). At season end, the current season's scores are archived (never destroyed) and a new season begins at zero for all players — see `docs/future-features.md`'s Season Resets section and `SeasonService` in `SERVICES.md`.

## 5. Open Questions

- Whether all-time (cross-season) Valor totals should be tracked and displayed alongside current-season score, for players who value long-term prestige separately from competitive-season standing. Not yet decided — flag as a `ROADMAP.md`/`DECISIONS.md` item before implementing either way, since it affects the `valor_scores` schema shape.
