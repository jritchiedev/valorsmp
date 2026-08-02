# SECURITY.md — The Valor SMP

Security policy for this codebase. This document takes priority over any conflicting instruction elsewhere in the repository, per the decision hierarchy in `AGENTS.md` §17.

---

## 1. Secure Coding Baseline

- All SQL is parameterized (`PreparedStatement` with bound parameters). String-concatenated SQL is forbidden without exception, even for values that "can't possibly" contain injection-relevant characters (player display names can — Minecraft usernames have a constrained charset, but don't rely on that alone; chat-derived input, etc. are less constrained).
- All player-supplied strings used in commands are length-limited and character-set-validated at the point of input, not just at storage time.
- No use of `Runtime.exec`/`ProcessBuilder` or any shell invocation from within the plugin. There is no legitimate use case for this in a Minecraft plugin's runtime; if one is ever proposed, treat it as a `DECISIONS.md`-worthy exception requiring explicit security sign-off.
- No deserialization of untrusted data via Java's native object serialization (`ObjectInputStream`). Use explicit, schema-validated formats (JSON via a well-maintained library, or the config/YAML pipeline already in place).

## 2. Permissions

- Every player-facing command and every protective check (tier-gated ability, admin-tier action) has an explicit permission node, declared in `plugin.yml` and checked in code — never inferred from OP status alone as the sole gate for anything beyond genuinely OP-only operations.
- Permission checks happen in the **command/listener layer** for "can this player invoke this at all," and in the **service layer** for "is this specific action valid given this player's state" (e.g., "is a player allowed to run `/speed` at all" vs. "is *this* player's Valor tier IV or V, which the command requires").
- Default permission defaults (`default: op`, `default: true`, `default: false` in `plugin.yml`) are chosen conservatively — anything that could affect another player's Valor score or staff rank defaults to `op` or an explicit granted permission, never `true` (everyone).
- Permission nodes follow a consistent hierarchy: `valorsmp.<feature>.<action>` (e.g., `valorsmp.speed.use`, `valorsmp.admin.reload`).

## 3. Configuration Validation

- All config values are validated at load time against expected type, range, and (where applicable) enum membership. Invalid values log a clear `WARN` and fall back to a safe default rather than crashing `onEnable` or silently propagating a bad value into gameplay logic (see `CONFIGURATION.md`).
- Numeric config values that feed into Valor score or tier-threshold calculations are range-checked to prevent operator misconfiguration from creating exploitable overflow/negative-score conditions.

## 4. Input Validation

- Any player-supplied numeric input (amounts, radii, durations) is validated for range and type before use — never trust client-side or command-argument input to already be sane.
- Command argument parsing rejects malformed input with a clear player-facing error rather than throwing an unhandled exception that surfaces a stack trace to the console (or worse, to the player).

## 5. File Access

- The plugin only reads/writes within its own data folder (`plugins/TheValorSMP/`) and the shared server data directory it's explicitly configured to touch (e.g., world folders only via Bukkit API, never direct filesystem manipulation of world data).
- No user-controllable input is ever used to construct a filesystem path without sanitization (path traversal via a crafted player-supplied name into a file path is a real risk class to guard against if any per-entity file ever exists — prefer database storage over per-entity files specifically to avoid this class of bug).

## 6. Dependency Updates

- Before adding any new dependency: check its license (must be compatible with this project's license — permissive licenses like MIT/Apache-2.0/BSD are straightforwardly fine; anything copyleft (GPL family) requires explicit review before use, since it may affect how this proprietary plugin can be distributed), and check for known CVEs via the dependency scanning step in CI (`DEVOPS.md`).
- Dependencies are kept reasonably current; security-relevant patch releases are applied promptly (within days, not months) once verified not to break the build.
- Never pin to a known-vulnerable version "because it works" without an explicit, time-boxed exception documented in `DECISIONS.md`.

## 7. Secrets Management

- No secrets (database passwords, third-party API keys) in source code, committed config files, or commit history — ever, including in a "temporary" commit meant to be squashed later. Assume anything committed to git is permanent.
- Secrets are supplied via environment variables or a gitignored local override file (`config/local.yml`, or equivalent) documented in `CONFIGURATION.md`, read only at runtime.
- If a secret is ever accidentally committed, it is treated as compromised immediately (rotated), not just removed from the latest commit — git history retains it regardless of a later "fix" commit.

## 8. Rate Limiting / Abuse Prevention

- Commands that are expensive (database writes, broad broadcasts) have per-player cooldowns enforced server-side, not just client-side UX suggestions.
- Any system involving repeated grant of value (Valor awards on kills) has server-side safeguards against duplicate/replay exploitation — e.g., a single death must never be counted as multiple kills, and a Valor award is driven only by an authoritative server-side death event, never a client-triggered action.
- Chat/command spam protections (existing server-level plugins or built-in Paper features) are assumed as a baseline; this plugin's own commands additionally guard against being used as a vector for spam (e.g., a command that broadcasts to the server is rate-limited per player).

## 9. Valor Integrity (Highest-Risk Area)

Because Valor directly represents player-perceived value:

- All score mutations go through a single, auditable path (`ValorScoreService`) — never a direct repository write from anywhere else in the codebase.
- Every mutation that isn't a routine kill/death award (admin grants) is logged with enough detail to reconstruct "who got what, why, and when" after the fact.
- Duplication bugs are treated as the single highest-severity class of bug in this codebase. Any change touching `ValorScoreService` or `ValorScoreRepository` requires an explicit test for the duplication/double-count failure mode, not just the happy path (per `AGENTS.md` §12).
- Score adjustments are atomic at the database level (single `UPDATE ... SET score = score + ?` statement, or an equivalent transaction), never a read-then-write-in-application-code pattern vulnerable to race conditions under concurrent requests.

## 10. Vulnerability Reporting

If a security issue is discovered (by a human or surfaced by an AI agent during work on an unrelated ticket): stop, do not attempt to silently patch it as a side effect of unrelated work, and escalate per `AGENTS.md` §13 immediately, flagging it as security-sensitive so it gets prioritized human attention rather than sitting in a normal ticket queue.
