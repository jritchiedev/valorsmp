# MODELS.md — The Valor SMP

Model-selection policy for AI agents working on this repository. This is guidance for whoever/whatever is dispatching tasks to specific AI agents or model tiers — not a claim that any specific model is embedded in this repo.

Principle: **match model capability class to task shape**, not to brand loyalty. Use the smallest/cheapest/fastest capable model for a task; escalate to a larger reasoning-focused model only when the task genuinely needs it. Over-provisioning wastes time and money for no quality benefit on mechanical tasks; under-provisioning produces subtly wrong architecture or security decisions.

---

## 1. Task-to-Model-Class Mapping

| Task | Recommended Model Class | Why |
|---|---|---|
| **Planning / architecture** | Frontier reasoning-tier model (e.g., a top-tier "thinking" model) | Requires weighing tradeoffs across the whole system, anticipating second-order effects, and producing a design that a smaller model might get locally-plausible but globally wrong. Mistakes here are the most expensive to unwind. |
| **Java coding (well-specified)** | Mid-tier coding-optimized model | Once a design exists, translating it to idiomatic Java against a clear architecture is a well-bounded task most competent coding models handle reliably and cheaply. |
| **Java coding (ambiguous/novel)** | Frontier reasoning-tier model | If the ticket itself requires design judgment mid-implementation (no existing pattern to copy), treat it like planning. |
| **Refactoring** | Frontier reasoning-tier model for scope/plan, mid-tier for mechanical execution | Deciding *what* to refactor and *why* needs judgment; *doing* the extraction once decided is mechanical. Split the task across two passes if the agent architecture allows it. |
| **Documentation generation** | Mid-tier model, frontier for anything requiring the writer to reconcile inconsistencies in existing docs | Most doc-writing is summarization/expansion from known facts (the code, the ticket). Reconciling contradictions across multiple existing docs benefits from stronger reasoning. |
| **Testing (writing tests)** | Mid-tier coding-optimized model | Given a service's contract, generating edge-case and failure-case tests is a bounded, pattern-matchable task. |
| **Testing (deciding what "correct" means for a fuzzy requirement)** | Frontier reasoning-tier model | If acceptance criteria are underspecified, this shades into design work. |
| **Debugging (reproducible, clear stack trace)** | Mid-tier model | Fast iteration matters more than depth for a well-bounded repro. |
| **Debugging (intermittent, cross-system, or performance-related)** | Frontier reasoning-tier model | Root-causing flaky/perf issues benefits heavily from broader context-holding and hypothesis generation. |
| **Performance optimization** | Frontier reasoning-tier model for analysis, mid-tier for mechanical rewrites once the bottleneck is identified | Misidentifying the bottleneck wastes more time than any coding speed gain. |
| **Security review** | Frontier reasoning-tier model, always | Security review is precisely the class of task where a plausible-looking-but-wrong answer is most costly. Never delegate this to the cheapest available model as a cost-saving measure. |
| **Code review (general)** | Mid-tier model for style/convention checks; frontier model for architectural/security-relevant PRs | Route by PR risk classification, not uniformly. |
| **Ticket generation (breaking down a feature)** | Mid-tier model, with a frontier pass to sanity-check the decomposition for large/ambiguous features | Decomposition quality matters, but a frontier design note as input makes this largely mechanical. |
| **Release notes** | Mid-tier model (or even a lightweight/fast model) | Purely summarization from merged PR titles/descriptions; low risk, high volume. |

---

## 2. Escalation Signal

Regardless of the default mapping above, **escalate to a frontier reasoning-tier model** whenever any of these are true for the current task:

- The change touches Valor score math, permission checks, or database migrations against live data.
- Two mid-tier attempts have already failed to produce a passing, sensible solution.
- The task requires synthesizing more than ~3 documents' worth of context to get right (e.g., reconciling `ARCHITECTURE.md`, a `docs/*.md` page, and `DATABASE.md` simultaneously).
- The output will be merged with reduced human review (e.g., a low-risk-classified PR that's likely to get a fast rubber-stamp) — put more model capability in up front since less will be applied at review time.

## 3. De-escalation Signal

Use a smaller/faster model when:

- The task is mechanical repetition of an established pattern (e.g., "add a new tier perk identical in shape to the existing ones").
- The task is pure formatting/lint fixing.
- The task is generating boilerplate (getters, a new DTO matching an existing shape, a config key with a documented default).

## 4. Model Output Verification

Regardless of model tier used, no output skips `AGENTS.md` §14's self-review checklist, and no output claims test results it didn't actually produce (`AI_RULES.md` §1.7). Model tier affects *quality of first draft*, not *what gets verified before merge*.
