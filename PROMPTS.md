# PROMPTS.md — The Valor SMP

Reusable prompt templates for dispatching work to AI agents on this repository. Fill in the bracketed fields. These templates assume the agent has repository access and will read `AGENTS.md`/`AI_CONTEXT.md` on its own, but restating the load-bearing constraints inline reduces the chance of an agent skipping them.

---

## Implement a Feature

```
You are implementing ticket [TICKET-ID] on The Valor SMP.

Read AGENTS.md, AI_CONTEXT.md, ARCHITECTURE.md, CODING_STANDARDS.md, and
docs/[feature].md before starting.

Ticket summary: [paste ticket]

Requirements:
- Follow the layered architecture in ARCHITECTURE.md exactly: model → repository
  (+ in-memory test fake) → service (+ unit tests) → listener/command
  (+ integration tests) → config → docs.
- No business logic in listeners/commands.
- No double/float for any currency or Valor score value.
- Add unit tests covering happy path, one edge case, one failure case for all new
  service logic. Add MockBukkit integration tests for new listener/command wiring.
- Update docs/[feature].md to reflect the shipped behavior.
- If you must make an assumption because the ticket is ambiguous, state it
  explicitly in the PR description rather than guessing silently.

Produce: the code changes, the tests, the doc update, and a PR description
following the template in AGENTS.md §10 (Problem / Design / Testing / Config
impact / Agent Notes).
```

---

## Refactor Existing Code

```
You are refactoring [module/class] on The Valor SMP. Ticket: [TICKET-ID].

Read AGENTS.md and ARCHITECTURE.md first.

Constraints:
- This is a refactor, not a behavior change. All existing tests must still pass
  without modification unless a test was asserting on an implementation detail
  that's explicitly being changed (call this out explicitly if so).
- Do not bundle unrelated cleanup. Stay scoped to [specific reason for refactor].
- If the refactor reveals a genuine bug, do not silently fix it — note it and
  propose a separate ticket, unless fixing it is required to complete the
  refactor safely (in which case, call it out explicitly in the PR).
- Preserve or improve test coverage; never reduce it without justification.

Produce: the refactored code, a diff-focused PR description explaining what
moved where and why, and confirmation that the full test suite passes.
```

---

## Review a Pull Request

```
You are reviewing PR #[number] on The Valor SMP against REVIEW.md's checklist.

Read the PR description and diff in full before commenting.

Check:
- Architecture: does it respect the layered dependency flow in ARCHITECTURE.md?
- Standards: does it follow CODING_STANDARDS.md (naming, null-safety, Javadoc)?
- Tests: does coverage match TESTING.md's minimum bar for this change type?
- Security: any permission, currency, or input-validation concern per SECURITY.md?
- Scope: is anything bundled in that doesn't belong in this ticket?
- Docs: are relevant docs/*.md pages updated to match the actual shipped behavior?

Produce: a structured review with each finding labeled [BLOCKING], [SHOULD-FIX],
or [NIT], and an explicit overall recommendation (Approve / Request Changes).
Do not approve a PR with any [BLOCKING] item outstanding.
```

---

## Generate Tests for Existing Code

```
You are writing tests for [class/service] on The Valor SMP, which currently has
[none / partial] coverage. Read TESTING.md first.

Requirements:
- Unit tests for all public methods: happy path, at least one edge case, at
  least one failure/invalid-input case.
- Use the existing in-memory repository fakes where the class under test depends
  on a repository interface; do not hit a real database in unit tests.
- If the class is a listener/command, write a MockBukkit integration test instead
  of/in addition to unit tests, per TESTING.md's guidance on what belongs at
  which test level.
- Name tests descriptively: methodName_condition_expectedResult().

Produce: the test file(s) and a short note on any behavior you discovered while
writing tests that seems like a bug (do not fix it silently — flag it).
```

---

## Optimize Performance

```
You are investigating a performance concern in [system/hot path] on The Valor
SMP. Read PERFORMANCE.md first.

Context: [describe symptom — TPS drop, lag spike, high memory, etc., and when
it occurs]

Requirements:
- Profile/measure before changing anything. State what you measured and how.
- Identify the actual bottleneck; do not guess-and-optimize.
- Prefer algorithmic/structural fixes (reducing repository calls, avoiding
  main-thread I/O, batching) over micro-optimizations.
- Any optimization that trades off readability must include a comment
  explaining why the non-obvious approach is necessary.
- Add or update a benchmark/test that demonstrates the improvement if feasible.

Produce: findings (with actual numbers, not estimates presented as measurements),
the fix, and before/after comparison.
```

---

## Generate Documentation

```
You are writing/updating docs/[feature].md on The Valor SMP to reflect the
current shipped behavior of [feature/system].

Read the actual code in [package] as the source of truth — not the original
design ticket, which may be stale.

Requirements:
- Cover: player-facing behavior, operator-facing configuration, data model
  summary, and any known limitations.
- No placeholders, no "TBD" sections. If something is genuinely undecided,
  say so explicitly and note it as an open question rather than omitting it.
- Match the structure/tone of other docs/*.md files in this repository.

Produce: the complete doc file.
```

---

## Investigate a Bug

```
You are investigating a bug report on The Valor SMP: [paste bug report /
ticket].

Requirements:
- Reproduce first. State the exact steps and observed vs. expected behavior.
- Write a regression test that fails against current code before fixing
  anything.
- Root-cause the actual defect; do not patch a symptom if the root cause is
  identifiable and fixable within reasonable scope.
- If the fix requires a data migration to correct already-corrupted state,
  say so explicitly and follow DATABASE.md's migration process — do not
  silently mutate production data as a side effect of a code fix.

Produce: the regression test, the fix, and a root-cause explanation in the
PR description.
```

---

## Design an API (Plugin-to-Plugin Surface)

```
You are designing a new public API surface in API.md for [capability], intended
for other plugins to consume.

Requirements:
- Design for the smallest surface area that satisfies the known use case; do
  not speculatively expose internals "in case someone needs them."
- Follow existing API.md conventions (interface-first, versioned, documented
  deprecation path).
- Consider: what happens if a consuming plugin calls this before ValorPlugin
  has finished enabling? Document the contract.
- No Bukkit-internal or NMS types in the public surface.

Produce: the interface definition(s), Javadoc, an addition to API.md, and a
note on backward-compatibility implications if this changes an existing surface.
```

---

## Design a New Service

```
You are designing a new service, [ServiceName], for [purpose] on The Valor SMP.

Read ARCHITECTURE.md §2.2 (Services) first.

Requirements:
- Define the public interface first: method signatures, return types
  (Optional<T> where absence is expected, never null), and Javadoc contracts.
- Identify which repository interface(s) it depends on (new or existing).
- Identify which domain events it fires, if any, and who's expected to consume
  them (informational — for docs, not for coupling).
- Identify the worst-case failure mode if this service has a bug, per AGENTS.md
  §12, and how a test will guard against it.

Produce: the interface, a brief design rationale, and the dependency list
(repositories, other services, config).
```

---

## Create a Database Migration

```
You are creating a migration for [schema change] on The Valor SMP. Read
DATABASE.md's migration process first.

Requirements:
- Additive/backward-compatible by default. If destructive, get explicit
  sign-off per AGENTS.md §12 and document the rollback path.
- Migration must be idempotent-safe (re-running it on an already-migrated
  database is a no-op or clean failure, not data corruption).
- Include the up-migration; include a down-migration if DATABASE.md's
  versioning scheme supports it for this migration type.
- Test the migration against a representative sample dataset, not just an
  empty database.

Produce: the migration file(s), an update to DATABASE.md's schema reference,
and a description of what was tested.
```

---

## Generate Release Notes

```
You are drafting release notes for version [X.Y.Z] of The Valor SMP, per
RELEASE.md's format.

Input: the list of merged PRs/commits since the last release: [paste list]

Requirements:
- Group by category: Features, Fixes, Performance, Security, Internal/Docs.
- Player-facing language for Features/Fixes (what changed for someone playing
  the server), not internal implementation detail.
- Flag any breaking config/migration changes prominently at the top.
- Do not invent changes not present in the input list.

Produce: the release notes in the format specified by RELEASE.md.
```
