# Future Features

Design notes for systems not yet promoted to their own `docs/<feature>.md` file. Each section here graduates to its own file (and this section is removed from this doc) once the system enters active implementation per `ROADMAP.md` — at which point `AI_CONTEXT.md` §3's status table must also be updated in the same PR.

---

## Season Resets

Periodic (e.g., quarterly) reset of season-scoped stats (Valor score, season leaderboards) with archival, not destruction, of prior data (`docs/valor-system.md` §4, `DATABASE.md`'s `season_archives` table).

- `SeasonService` orchestrates the reset: archive current data → reset season-scoped tables to fresh state → fire `SeasonEndedEvent` then `SeasonStartedEvent` → other features react.
- Only Valor score and Valor-derived leaderboards are season-scoped. Any expansion of what's season-scoped is a `DECISIONS.md`-level product decision, not an implementation detail to decide casually mid-build.
