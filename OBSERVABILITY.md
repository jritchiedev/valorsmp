# OBSERVABILITY.md — The Valor SMP

How we know what the plugin is doing in production, and how we diagnose problems.

---

## 1. Logging

- SLF4J-backed logger throughout (`CODING_STANDARDS.md` §6). Levels used consistently:
  - `ERROR` — failures requiring attention (repository I/O failure, unexpected exception at a service boundary).
  - `WARN` — recoverable anomalies (invalid config value defaulted, a migration skipped because already applied, a player action rejected due to a caught business-rule violation that's unusual enough to be worth surfacing at scale).
  - `INFO` — lifecycle milestones (enable/disable, migrations run, season start/end, scheduled event start/end).
  - `DEBUG` — detailed diagnostic trace, off by default, enabled via `debug-mode: true` in a top-level config or `/valorsmp debug` toggle for live diagnosis without a restart.
- Structured where it helps: log lines that will be grepped/parsed (e.g., Valor mutation audit entries) include a stable, greppable prefix and consistent key=value formatting, even though this isn't a full structured-logging pipeline (see §4 for future direction).
- Never log full stack traces at `INFO` — reserve full traces for `ERROR`, and prefer a concise message at `WARN` for expected-but-notable conditions.

## 2. Metrics

At minimum, the plugin tracks and exposes (via `/valorsmp stats` admin command and/or a metrics endpoint if `DEVOPS.md`'s chosen monitoring stack calls for one):

- Per-tick server TPS/MSPT correlation points around known hot paths (combat resolution, item-ability cooldown checks) — not a full profiler, but enough to notice "this feature correlates with lag."
- Database connection pool utilization (active/idle connections, wait time) — an early warning sign of a pool sized too small or a leak.
- Valor mutation counts and totals per time window — both an operational health signal and a fraud/exploit early-warning signal (a sudden spike in Valor creation is the first sign of a duplication bug).
- Command invocation counts per command — informs which commands are worth optimizing and which are barely used.

## 3. Error Reporting

- All uncaught exceptions at the listener/command boundary are caught by a top-level handler that logs full context (player, command/event, stack trace) at `ERROR` and fails gracefully (the specific action doesn't complete; the server doesn't crash).
- Repeated errors of the same type within a short window are noted (not spammed identically hundreds of times per second) — a simple rate-limited/deduplicated error log wrapper is used for any error path reachable from a high-frequency event.
- Consider integration with an external error-tracking service (e.g., Sentry) as a `ROADMAP.md` item if error volume grows enough to need it — not implemented by default to avoid an external dependency/cost with no current proven need.

## 4. Performance Monitoring

- See `PERFORMANCE.md` for the full performance budget and profiling approach. Observability's role here is making sure the *signals* (TPS, MSPT, GC pause frequency, per-feature timing) are actually visible to operators without needing to attach a profiler cold in the middle of an incident.
- Paper's built-in `/timings` (or the modern Paper equivalent) remains the primary low-effort profiling entry point; this plugin's own metrics supplement it with feature-specific granularity Paper's generic timings can't provide.

## 5. Debug Mode

- `debug-mode: true` (or `/valorsmp debug on`) enables `DEBUG`-level logging for this plugin specifically, without needing to change the server's global log level (which would flood the console with every other plugin's debug output too).
- Debug mode should never be left on by default in shipped config — it's an operator-invoked diagnostic tool, not a normal running state.

## 6. Health Checks

- On `onEnable`, the plugin performs and logs the result of: database connectivity check, migration status check, config validation summary (how many keys fell back to defaults, if any). This gives an operator a single place in the startup log to confirm "did this actually come up healthy."
- `/valorsmp health` (admin command) reports current status: DB connection pool state, last successful write timestamp for critical repositories, any features currently disabled due to a failed dependency (e.g., MySQL unreachable at startup, running in a degraded read-only mode if that's ever implemented — otherwise, failing to enable at all is preferable to running in a silently broken state for anything data-integrity related).

## 7. What Good Observability Looks Like Here

The bar: when something goes wrong at 2am with no one watching, the next morning's operator should be able to answer, from logs and `/valorsmp stats`/`health` alone: what happened, roughly when, and which system it was in — without needing to reproduce the issue or read code. If a new feature can't meet that bar with the logging/metrics it ships with, that's a gap to close before merge, not after an incident.
