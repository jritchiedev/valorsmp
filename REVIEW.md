# REVIEW.md — The Valor SMP

What a reviewer (human or a designated review-capable AI agent) checks on every PR, and how to give/receive feedback effectively.

---

## 1. Review Checklist

For every PR, check, in this order (stop and request changes at the first blocking issue category if it's severe enough that further review would be wasted effort — e.g., no point reviewing test quality on code that violates the architecture layering):

### Architecture
- [ ] Respects the layered dependency flow (`ARCHITECTURE.md` §4) — no repository calling a service, no listener containing business logic, no service reaching into `ValorPlugin` directly.
- [ ] New feature (if applicable) follows the vertical-slice shape (`ARCHITECTURE.md` §7).
- [ ] Cross-feature relationships use events where appropriate, direct calls only where genuinely tight/intentional (`SERVICES.md` §4).

### Standards
- [ ] Naming, structure, null-safety per `CODING_STANDARDS.md`.
- [ ] Javadoc present on new public service/repository methods.
- [ ] No `double`/`float` for currency or Valor score math.

### Tests
- [ ] Meets `TESTING.md`'s minimum bar for the change type (unit for service logic, integration for listener/command wiring, real-SQL for repository changes, regression test for bug fixes).
- [ ] Tests actually test behavior, not implementation details (`TESTING.md` §8).
- [ ] No reduction in overall coverage without justification.

### Security
- [ ] Permission checks present and correctly scoped (`SECURITY.md` §2).
- [ ] No SQL injection risk (parameterized queries).
- [ ] No secrets committed.
- [ ] Currency/Valor mutations go through the single auditable path, atomic where required (`SECURITY.md` §9).

### Scope
- [ ] PR matches its linked ticket's scope — no unrelated bundled changes.
- [ ] No speculative/unused code ("might need this later").

### Documentation
- [ ] Relevant `docs/<feature>.md` updated to reflect actual shipped behavior.
- [ ] `CONFIGURATION.md`/`EVENTS.md`/`API.md` updated if applicable.
- [ ] PR description is complete (Problem / Design / Testing / Config impact / Agent Notes if AI-authored).

### Performance (if hot-path-relevant)
- [ ] Performance note present per `PERFORMANCE.md` §6 if the change touches combat resolution, a high-frequency listener, or claim/region lookups.

## 2. Severity Labeling

Every review comment is labeled:

- **[BLOCKING]** — must be fixed before merge. Architecture violations, missing required tests, security issues, secrets, scope violations.
- **[SHOULD-FIX]** — a real improvement, strongly recommended, but the author/reviewer can agree to defer to a fast-follow ticket if there's a good reason (e.g., time pressure on an urgent fix) — this must be an explicit, stated agreement, not a silent skip.
- **[NIT]** — stylistic/minor preference, non-blocking, author may take-or-leave.

A PR is not approved while any `[BLOCKING]` item is outstanding.

## 3. Giving Feedback

- Be specific: point to the exact line/file, explain *why* it's an issue (not just "this is wrong"), and where reasonable, suggest a direction (not necessarily a full solution — the author should still own the fix).
- Distinguish "this violates a documented rule" from "this is my personal preference" — cite the specific document/section for the former.
- Ask questions genuinely open to being answered ("why did you choose X over Y?") rather than as a disguised directive, unless it really is a directive — in which case, just say so directly and label it appropriately.

## 4. Receiving Feedback

- Address `[BLOCKING]` items directly; don't argue the review process itself unless you believe the review is factually wrong about what a document requires (in which case, point to the specific text).
- For `[SHOULD-FIX]` items you disagree with, explain your reasoning; it's fine to reach a different conclusion than the reviewer if the reasoning holds up, but don't just silently ignore the comment either way — respond to every comment.
- Scope-creep suggestions (a good idea that's beyond this ticket) go into a new ticket, referenced in your response, not folded into the current PR (`AI_RULES.md` §3).

## 5. PR Size Guidance

- Target: reviewable in one sitting, roughly under ~600 lines of non-generated diff (`AGENTS.md` §7). Beyond that, split by layer or by sub-feature.
- If a PR genuinely can't be split further (e.g., a single cohesive migration touching many call sites), call this out explicitly in the description so the reviewer can budget appropriate time rather than being surprised by size.

## 6. Approval

- At least one human maintainer approval required for merge (`AI_WORKFLOW.md` Checkpoint 3), except for narrowly-scoped, low-risk change classes explicitly delegated to an agent's approval authority via a `DECISIONS.md` entry.
- Self-approval is never permitted, regardless of author (human or agent).
