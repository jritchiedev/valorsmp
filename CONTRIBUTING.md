# CONTRIBUTING.md — The Valor SMP

How to contribute to this repository, whether you're a human contributor or an AI agent. This document is the human-facing front door; `AGENTS.md` is the fuller process specification that this document points into.

---

## 1. Before You Start

- Read `AI_CONTEXT.md` for project orientation.
- Read `ARCHITECTURE.md` and `CODING_STANDARDS.md` before writing any code.
- Check `TICKETS.md`-formatted issue tracker for an existing ticket matching what you want to work on, or open one first for anything beyond a trivial fix — this avoids duplicated effort and gives maintainers a chance to weigh in on approach before code is written.

## 2. Setting Up

Follow `DEVELOPMENT.md` §1 for environment setup and `REPOSITORY.md` §4–5 for building/running locally.

## 3. Making a Change

1. Fork or branch per `AGENTS.md` §10's naming convention.
2. Follow the implementation workflow in `AGENTS.md` §8 and `AI_WORKFLOW.md`.
3. Write tests per `TESTING.md`'s minimum bar — PRs without adequate test coverage will be sent back for more work, not merged with a promise to "add tests later."
4. Update relevant documentation (`docs/<feature>.md`, `CONFIGURATION.md`, `API.md`, `EVENTS.md` as applicable).
5. Run `./gradlew check` locally and confirm it passes before opening a PR.

## 4. Opening a Pull Request

- Use the PR description template implied by `AGENTS.md` §10: Problem, Design summary, Testing performed, Config/migration impact, Agent Notes (if AI-authored).
- Link the ticket.
- Keep the PR scoped to one ticket — no unrelated drive-by changes (`AI_RULES.md` §3).
- Expect review feedback; respond promptly and don't take it personally — see `REVIEW.md` for what reviewers are checking and why.

## 5. Code of Conduct (Human Contributors)

Be respectful and constructive in review discussions. Disagreements about technical approach are normal and productive when focused on the work; personal criticism isn't welcome. Maintainers may close PRs or tickets that don't align with the project's direction (`ROADMAP.md`) — this isn't a judgment of the contributor, just a scoping decision.

## 6. AI Agent Contributors

AI agents contributing to this repository follow `AGENTS.md`, `AI_RULES.md`, and `AI_WORKFLOW.md` in full, not just this document's summary. Every AI-authored PR must include the `## Agent Notes` section described in `AGENTS.md` §10. Human contributors reviewing AI-authored PRs apply the same standard as any other PR — no lower (nor artificially higher) bar because of authorship.

## 7. What Gets Prioritized

See `ROADMAP.md` for current priorities. In general: bug fixes affecting data integrity or security (`SECURITY.md`) are always high priority; new features are prioritized against the roadmap; documentation-only improvements and small refactors are welcome anytime and reviewed on a best-effort basis.

## 8. Questions

For anything not covered by the documents linked above, open a discussion/issue rather than guessing — see `AGENTS.md` §11 for the general principle of when to ask vs. proceed with a stated assumption.
