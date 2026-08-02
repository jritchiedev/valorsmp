# AI_RULES.md — The Valor SMP

Hard constraints for AI agents, distinct from `AGENTS.md` (process) and `CODING_STANDARDS.md` (style). These are rules whose violation should be treated as a build-breaking or PR-blocking issue, not a style nit.

---

## 1. Absolute Rules (Never Violate)

1. **Never commit secrets.** No database passwords, API tokens, webhook URLs, or credentials in source, config defaults, or commit history. Use environment variables or a gitignored local override file (`config/local.yml`, already in `.gitignore`).
2. **Never disable a failing test to "fix" a build.** Fix the underlying issue or escalate per `AGENTS.md` §13. If a test is genuinely wrong, that requires a human-reviewed PR explaining why.
3. **Never use `double`/`float` for Valor score math.** Valor scores are whole, non-negative integers (`int`/`long`), floored at 0.
4. **Never push directly to `main`, `develop`, or any release branch.** All changes go through PR review, including agent-authored ones.
5. **Never remove or weaken a permission check** without an explicit ticket instructing it and a `SECURITY.md`-aligned justification in the PR description.
6. **Never introduce a new runtime dependency** (Maven/Gradle artifact) without checking its license compatibility and noting it in `DECISIONS.md`. Prefer zero new dependencies over adding one for trivial convenience.
7. **Never fabricate results.** If you did not run the tests, do not say you did. If you did not verify behavior against a real server, say so explicitly in the PR description.
8. **Never delete player data** as part of a migration without an additive/archival path (see `DATABASE.md`). Season resets archive; they do not `DROP` or `DELETE` without a backup step first.

## 2. Structural Rules

- Every new feature is a vertical slice through `model/ → repository/ → service/ → events/ → listener|command/`, per `ARCHITECTURE.md` §7.
- Every public service method needs Javadoc stating its contract (preconditions, what it returns, what exceptions/Optional-empty means).
- No class exceeds roughly 400 lines as a soft ceiling; if you're approaching it, that's a signal to extract a collaborator class, not to keep going.
- No method exceeds roughly 40 lines as a soft ceiling for the same reason.

## 3. Scope Discipline

- Agents implement what the ticket asks for — not a "better" scope the agent invented. Extra ideas go into a new ticket in `TICKETS.md` format, proposed in the PR description, not silently implemented.
- Agents do not perform drive-by refactors of unrelated code in a feature PR. If a refactor is genuinely needed to complete the ticket safely, it's called out explicitly and kept minimal, or split into its own PR first.

## 4. Communication Rules

- When an agent's PR description claims something is tested, it must state exactly how (unit test names, integration test names, or manual steps performed against a local server instance).
- When an agent is uncertain about a design choice it made, it states the assumption explicitly in the PR rather than hiding the uncertainty.
- When an agent disagrees with an instruction in a ticket because it conflicts with `SECURITY.md` or `ARCHITECTURE.md`, it says so and escalates rather than silently complying or silently doing something else.

## 5. Model-Output Hygiene

- No placeholder code (`// TODO: implement this`) merged into `main`. If a ticket is genuinely partial, it is broken into smaller tickets, each of which is fully implemented before merge.
- No commented-out blocks of old code left "just in case" — rely on git history instead.
- Generated documentation must reflect the actual code in the same PR, not an aspirational future state.

## 6. Enforcement

These rules are enforced by:
- CI checks where mechanically possible (dependency license scanning, test coverage gates — see `DEVOPS.md`).
- Human review for everything else, per `REVIEW.md`.
- Retroactively: any violation found post-merge gets a follow-up ticket and, for repeated violations by the same agent/process, a review of whether that agent should have write access for that class of change (see `AGENTS.md` §13 escalation).
