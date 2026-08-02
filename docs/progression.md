# Progression

The mechanical implementation of the Valor tier ladder: thresholds, display, and the perks each tier grants. For the conceptual "what is Valor and why" discussion, see `docs/valor-system.md` — this document is the practical "how the ladder works" reference.

---

## 1. Tier Ladder

Every 4 Valor Points unlocks a new tier. The maximum tier is 5. Perks are cumulative in effect as described per tier.

| Tier | Valor Points | Perks |
|---|---|---|
| Valor I | 0–4 | No perks. |
| Valor II | 5–8 | Extended potion effects — effects that are 8 minutes long become 10 minutes; effects that are 1 min 30 sec become 3 minutes. |
| Valor III | 9–12 | Permanent Speed I. (`/effect give <player> minecraft:speed infinite`) |
| Valor IV | 13–16 | Permanent Strength I and Speed II. (`/effect give <player> minecraft:speed infinite 1`, `/effect give <player> minecraft:strength infinite`) |
| Valor V | 16–20 | Permanent Strength II and Speed II. (`/effect give <player> minecraft:speed infinite 1`, `/effect give <player> minecraft:strength infinite 1`) |

- Valor IV and V players may switch their permanent Speed effect between Speed I and Speed II with `/speed set 1|2`.
- **Note:** the source spec lists Valor IV as `13–16` and Valor V as `16–20`, so point 16 appears in both. Confirm the intended boundary (likely Valor V = `17–20`) before finalizing config thresholds.

## 2. Tier Computation

- Pure function of current-season `valor_scores.score` against the fixed thresholds above — no hysteresis/stickiness by default (crossing back below a threshold demotes the displayed tier immediately), unless a "tier floor" feature is explicitly requested and designed (not currently planned; would need a `DECISIONS.md` entry given it changes the "tier always reflects current standing" simplicity).
- Computed on every Valor score mutation, but `ValorRankChangedEvent` only fires when the *tier* actually changes, not on every point gained (`EVENTS.md`).

## 3. Display

- Tier is shown in: chat prefix (via `ChatFormatService`, reacting to `ValorRankChangedEvent`), tab list, `/rank` command output, and leaderboard listings.
- Tier display uses Adventure `Component` with the configured color/style per tier — never legacy color codes (`CODING_STANDARDS.md` §9).

## 4. Season Reset Behavior

- At season end, current-season tier is archived alongside score (`docs/future-features.md`'s Season Resets section).
- New season starts every player at Valor I / 0 points, per `docs/valor-system.md` §4.

## 5. Class Responsibilities

| Class | Responsibility |
|---|---|
| `ValorScoreService` | Score mutation, tier computation, firing `ValorRankChangedEvent`, applying/removing tier perks |
| `ValorTier` (model) | Enum/value type for the ladder tiers, ordered, with threshold and perk metadata |
| `ValorRankChangedEvent` | Domain event carrying old/new tier for a player |
| `ChatFormatService` (consumer) | Reacts to tier changes to update cached chat prefix |
