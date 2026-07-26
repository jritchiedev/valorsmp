# AGENTS.md — The Valor SMP

**Master instruction file for all AI coding agents** (ChatGPT/GPT-5.5+, Devin, Claude Code, Cursor, GitHub Copilot Agent, Windsurf, OpenHands, and any future AI IDE agent operating on this repository).

If you are an AI agent reading this file, treat it as binding operating procedure, not advisory background. When any other document in this repository conflicts with this file, **this file wins** unless the conflicting document is more specific to the task at hand (see Decision Hierarchy below).

---

## 1. Project Philosophy

The Valor SMP is a Java 21 / PaperMC survival multiplayer server built as **production software**, not a hobby plugin. That means:

1. **Correctness over cleverness.** A boring, obviously-correct solution beats a clever one that's hard to verify.
2. **Testable business logic, thin Bukkit glue.** Anything that can be tested without a running server, must be structured so it can be tested without a running server.
3. **Configuration over recompilation.** Server operators change behavior through YAML config, not code edits.
4. **Everything is a service with a contract.** If a class does meaningful work, it implements an interface, is registered through dependency injection, and is replaceable.
5. **Documentation is not an afterthought.** A feature is not "done" until its behavior, its data model, and its test coverage are documented in the locations this repo defines.
6. **AI agents are first-class contributors.** The repository is structured, annotated, and documented specifically so that autonomous agents can plan and ship features with minimal human hand-holding — but never with zero human accountability (see Section 13, Escalation Process).

---

## 2. Repository Layout

```
valor-smp/
├── AGENTS.md
├── ARCHITECTURE.md
├── AI_CONTEXT.md
├── AI_RULES.md
├── AI_WORKFLOW.md
├── MODELS.md
├── PROMPTS.md
├── REPOSITORY.md
├── SERVICES.md
├── REPOSITORIES.md
├── EVENTS.md
├── DATABASE.md
├── API.md
├── FEATURES.md
├── CODING_STANDARDS.md
├── TESTING.md
├── SECURITY.md
├── CONFIGURATION.md
├── OBSERVABILITY.md
├── PERFORMANCE.md
├── DEVOPS.md
├── DEVELOPMENT.md
├── CONTRIBUTING.md
├── REVIEW.md
├── RELEASE.md
├── ROADMAP.md
├── DECISIONS.md
├── TICKETS.md
├── docs/
│   ├── valor-system.md
│   ├── combat.md
│   ├── progression.md
│   ├── economy.md
│   └── future-features.md
├── src/
│   ├── main/java/net/thevalorsmp/
│   │   ├── ValorPlugin.java
│   │   ├── core/                # DI container bootstrap, lifecycle
│   │   ├── config/               # Config models + loaders
│   │   ├── events/                # Custom Bukkit events (domain events)
│   │   ├── listeners/            # Bukkit listeners (thin, delegate to services)
│   │   ├── services/             # Business logic (Bukkit-agnostic where possible)
│   │   ├── repositories/         # Data access layer
│   │   ├── models/               # Domain models / DTOs
│   │   ├── managers/             # Stateful runtime coordinators
│   │   ├── commands/             # Command executors (thin, delegate to services)
│   │   └── util/                 # Pure utility code
│   ├── main/resources/
│   │   ├── plugin.yml
│   │   └── config/                # Default YAML configs per feature
│   └── test/java/net/thevalorsmp/
│       ├── unit/
│       ├── integration/            # MockBukkit-based
│       └── fixtures/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── .github/workflows/
```

**Rule:** if you cannot find an obvious home for new code in this layout, stop and propose a layout change in `DECISIONS.md` rather than inventing a parallel structure.

---

## 3. Coding Standards (Summary — full detail in `CODING_STANDARDS.md`)

- Java 21, Gradle Kotlin DSL, PaperMC API (latest stable at time of task).
- Constructor-based dependency injection everywhere. No static singletons for stateful services.
- No business logic in `Listener` or `CommandExecutor` classes — they translate Bukkit events/commands into service calls and translate results back into Bukkit-facing effects (messages, sounds, etc.).
- All public service methods have Javadoc describing contract, not implementation.
- Null safety: use `Optional<T>` for absent-but-expected values; never return `null` from a service method that returns an object type. Use `@Nullable`/`@NotNull` (JetBrains annotations) on all fields/params/returns where nullability is not obvious from an `Optional` wrapper.
- No `System.out.println`. Use the plugin's SLF4J-backed logger exclusively.

---

## 4. Architecture Overview (Summary — full detail in `ARCHITECTURE.md`)

Layered, repository-pattern architecture:

```
Bukkit Event / Command
        │
        ▼
  Listener / CommandExecutor   (thin adapters)
        │
        ▼
      Service                 (business logic, orchestration)
        │
        ▼
    Repository                (data access abstraction)
        │
        ▼
  Storage Backend             (SQLite / MySQL / flat-file, pluggable)
```

Dependency flow is strictly downward. Repositories never call services. Services never call listeners. Listeners never contain business logic.

---

## 5. Autonomous Workflow

Every AI agent, on receiving a feature request, bug report, or refactor request, follows this loop. Full lifecycle detail lives in `AI_WORKFLOW.md`.

1. **Clarify scope** — read `AI_CONTEXT.md`, the relevant `docs/*.md` file, and any linked ticket (`TICKETS.md` format).
2. **Plan** — produce a short design note: affected services, repositories, models, events, config keys, and tests. For anything nontrivial, this plan should be posted as a PR description or ticket comment *before* code is written.
3. **Decompose** — break the plan into commits/PRs no larger than a single reviewable unit (see `REVIEW.md` for size guidance).
4. **Implement** — write code following `CODING_STANDARDS.md`.
5. **Test** — write unit tests for all service logic and integration tests (MockBukkit) for listener wiring, per `TESTING.md`.
6. **Self-review** — run the checklist in Section 14 before requesting review.
7. **Document** — update or create the relevant `docs/*.md` page, update `CHANGELOG`-equivalent entries described in `RELEASE.md`.
8. **Submit for review** — following `REVIEW.md` and PR conventions in Section 10 below.
9. **Respond to review** — address feedback; do not argue for scope creep; open a new ticket for out-of-scope suggestions.
10. **Ship** — once merged, verify the release process in `RELEASE.md` picks up the change correctly.

---

## 6. Planning Process

Before writing code for anything larger than a one-line fix, an agent must produce a plan containing:

- **Problem statement** — one paragraph, in terms of player/operator-facing behavior.
- **Design** — which services/repositories/models/events are added or changed, with a short justification.
- **Data model impact** — new tables/columns, migration needed? (see `DATABASE.md`)
- **Config impact** — new YAML keys, defaults, and validation rules (see `CONFIGURATION.md`)
- **Test plan** — what will be unit tested vs. integration tested.
- **Risk** — what could go wrong, and how it will be mitigated (see Section 12, Risk Management).

This plan does not need to be a separate document for small tickets — it can be the PR description — but it must exist in writing before implementation for anything touching more than ~3 files or introducing a new service/repository/table.

---

## 7. Task Decomposition

- One ticket = one coherent unit of behavior, not one class.
- One PR = one ticket, unless the ticket explicitly says otherwise.
- If implementing a feature requires a new repository, a new service, a new event, and a new command, that is still typically **one PR** if the total diff is reviewable in one sitting (roughly: under ~600 lines of non-generated code). Beyond that, split by layer: (1) models + repository + tests, (2) service + tests, (3) listener/command + integration tests.
- Never bundle unrelated refactors into a feature PR. File a separate ticket.

---

## 8. Implementation Workflow

1. Create/checkout branch (naming convention in Section 10).
2. Write or update model classes in `models/`.
3. Write or update repository interface + implementation in `repositories/`, plus a fixture/in-memory implementation for tests.
4. Write or update service in `services/`, injected with repository interfaces (never concrete implementations).
5. Wire listeners/commands as thin adapters.
6. Add/extend config schema and defaults.
7. Add domain events if other systems need to react to this change (see `EVENTS.md`).
8. Write tests (unit first, then integration).
9. Run the full test suite and static analysis locally (`./gradlew check`) before opening a PR.

---

## 9. Testing Workflow

See `TESTING.md` for full detail. Minimum bar for any PR:

- New service logic → unit tests covering the happy path, at least one edge case, and at least one failure case.
- New listener/command → MockBukkit integration test proving the wiring works end-to-end against the service (service itself can be mocked/stubbed here).
- Bug fixes → a regression test that fails on the old code and passes on the new code, referenced in the PR description.
- No PR merges with reduced overall coverage unless explicitly justified in the PR description and accepted by a human reviewer.

---

## 10. Commit, Branch, and PR Conventions

**Branch naming:**
```
feature/<ticket-id>-<short-slug>       e.g. feature/VAL-142-land-claim-flags
fix/<ticket-id>-<short-slug>           e.g. fix/VAL-201-duplicate-crate-drop
refactor/<ticket-id>-<short-slug>
chore/<short-slug>
docs/<short-slug>
```

**Commit messages** (Conventional Commits, enforced):
```
<type>(<scope>): <summary, imperative mood, no trailing period>

<body — what and why, not how>

Refs: VAL-142
```
Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `security`.
Scope: the top-level package touched, e.g. `combat`, `economy`, `land-claims`, `core`.

**PR conventions:**
- Title mirrors the primary commit's summary line.
- Description contains: Problem, Design summary, Testing performed, Config/migration impact, Screenshots/log excerpts if behavior-visible.
- PRs link the ticket ID.
- PRs are scoped to one ticket. No "drive-by" unrelated changes.
- AI-authored PRs must include a `## Agent Notes` section stating: which model/agent produced the change, what was self-reviewed, and any assumptions made per Section 11.

---

## 11. When to Ask for Clarification — and When NOT To

**Ask a human when:**
- The request is ambiguous about player-facing behavior in a way that materially changes the design (e.g., "should land claims be transferable?" when nothing in `docs/` addresses it).
- The request conflicts with an existing `DECISIONS.md` entry.
- The request would require a breaking data migration on a live production database, and no rollback plan is obvious.
- The request touches `SECURITY.md`-classified areas (permissions, economy balances, admin commands) and the exact intended access level isn't specified.
- Two reasonable designs exist with materially different long-term maintenance cost, and the ticket doesn't indicate a preference.

**Do NOT ask, just proceed, when:**
- The answer is already specified in `AI_CONTEXT.md`, `docs/*.md`, `CODING_STANDARDS.md`, or a prior `DECISIONS.md` entry — read first.
- The question is about formatting, naming, or structure already covered by `CODING_STANDARDS.md`.
- The choice is a reversible implementation detail with no player-facing or architectural consequence (e.g., which internal helper method name to use).
- You could resolve the ambiguity by picking the most conservative, least-surprising default and stating the assumption in the PR description instead of blocking on it.

Default bias: **state an assumption and proceed**, rather than stall a ticket. Blocking should be rare and justified.

---

## 12. Risk Management

For any change touching money (economy), permissions, or persistent player data:

- Identify the worst-case failure mode explicitly in the plan (e.g., "a bug here could duplicate currency" or "a bug here could grant admin-tier permissions").
- Add a test specifically targeting that failure mode, not just the happy path.
- Prefer additive, backward-compatible database migrations. Destructive migrations require a `DECISIONS.md` entry and a documented rollback path in `DATABASE.md`.
- Feature-flag risky new systems behind a config toggle defaulting to `false` where feasible, so operators can disable without a redeploy.

---

## 13. Escalation Process

If an agent gets stuck (failing tests it cannot diagnose after reasonable effort, a design question it cannot resolve conservatively, a security-sensitive ambiguity):

1. Stop implementation.
2. Write up the blocker clearly: what was tried, what failed, what the open question is.
3. Post it as a comment on the ticket (or PR draft) rather than guessing silently.
4. If the blocker is security- or data-integrity-related, mark the ticket/PR with a `needs-human-decision` label (or equivalent text marker if labels aren't available to the agent) and do not merge anything related until a human responds.

---

## 14. Self-Review Checklist (run before requesting review)

- [ ] No business logic in listeners/commands.
- [ ] All new services depend on interfaces, not concrete repository/service classes.
- [ ] All new public methods have Javadoc.
- [ ] No `null` returned from any method that isn't explicitly typed to allow it.
- [ ] New config keys have documented defaults and validation, per `CONFIGURATION.md`.
- [ ] New data model changes have a migration and are documented in `DATABASE.md`.
- [ ] Unit tests added/updated for all new/changed service logic.
- [ ] Integration tests added/updated for all new/changed listener/command wiring.
- [ ] `./gradlew check` passes locally.
- [ ] Relevant `docs/*.md` file updated.
- [ ] PR description includes Problem / Design / Testing / Config impact / Agent Notes.
- [ ] No secrets, tokens, or credentials committed (see `SECURITY.md`).
- [ ] No unrelated changes bundled into this diff.

---

## 15. Forbidden Behaviors

Agents must never:

- Disable, skip, or weaken a test to make a build pass, without an explicit human-approved ticket documenting why.
- Introduce a new third-party dependency without checking it against `SECURITY.md` and noting it in `DECISIONS.md`.
- Hardcode credentials, API keys, or database passwords anywhere in source or config defaults.
- Modify `RELEASE.md`'s versioning rules or CI/CD pipeline behavior without a `DECISIONS.md` entry.
- Push directly to `main`. All changes go through PRs, even for agents with write access.
- Fabricate test results, benchmark numbers, or "verified" claims in a PR description. If something wasn't actually run, say so.
- Silently change public API surface (`API.md`) without a deprecation path.

---

## 16. Expected Output Quality

Treat every PR as if a senior engineer at a company with a multi-year-old, heavily used codebase will maintain it after you're gone. That means:

- Names are precise and unambiguous, not generic (`ClaimTransferService`, not `Helper2`).
- Edge cases are handled deliberately, not accidentally ignored.
- Comments explain *why*, not *what* (the code should already say what).
- Nothing is copy-pasted with minor variable renames — extract shared logic.

---

## 17. Decision Hierarchy

When documents conflict, resolve in this order:

1. `SECURITY.md` (safety/integrity always wins)
2. `AGENTS.md` (this file — process and behavior)
3. `ARCHITECTURE.md` / `CODING_STANDARDS.md` (structural rules)
4. Feature-specific `docs/*.md` (domain behavior)
5. `DECISIONS.md` (historical precedent — check for a more recent decision that supersedes an older document)
6. Ticket-specific instructions (most narrow scope, but cannot override 1–3)

If a ticket instruction conflicts with `SECURITY.md` or `AGENTS.md`, escalate per Section 13 instead of silently complying or silently refusing.
