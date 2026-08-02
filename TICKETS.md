# TICKETS.md — The Valor SMP

Template and conventions for engineering tickets (whether tracked in GitHub Issues, a separate tracker, or inline in a planning doc). Consistent ticket shape is what makes `AI_WORKFLOW.md`'s Stage 1 intake reliable for autonomous agents.

---

## Ticket Hierarchy

```
Epic
 └── Story
      └── Task
```

- **Epic**: a milestone-sized body of work (roughly maps to a `ROADMAP.md` milestone or a large sub-part of one). Not directly implemented; broken into Stories.
- **Story**: a player/operator-facing unit of value, typically one PR or a small tightly-related set of PRs (`AGENTS.md` §7).
- **Task**: an internal, non-directly-player-facing unit of work supporting a Story (e.g., "add the migration for X" as a Task under a Story that also needs the service and listener).

## Ticket Template

```markdown
## [VAL-XXX] <Title — imperative, describes the outcome>

**Type:** Epic | Story | Task
**Parent:** [VAL-YYY] (if Story/Task)
**Priority:** Critical | High | Medium | Low

### Description
[1–3 sentences: what this is and why it matters, in player/operator terms
where applicable.]

### Acceptance Criteria
- [ ] Specific, testable condition 1
- [ ] Specific, testable condition 2
- [ ] ...

### Definition of Done
- [ ] Code implemented following ARCHITECTURE.md / CODING_STANDARDS.md
- [ ] Unit tests per TESTING.md's minimum bar
- [ ] Integration tests per TESTING.md's minimum bar (if listener/command involved)
- [ ] Relevant docs/*.md updated
- [ ] CONFIGURATION.md / EVENTS.md / API.md updated if applicable
- [ ] PR merged with human approval
- [ ] Deployed and post-deploy-verified (if this ticket alone triggers a release;
      otherwise, verified as part of its containing release per RELEASE.md)

### Technical Notes
[Known constraints, relevant existing services/repositories to reuse, links to
relevant docs/*.md sections, any design decisions already made in DECISIONS.md
that apply.]

### Testing Notes
[Specific scenarios that must be covered beyond the generic Definition of Done
bar — e.g., "must include a duplication-safety test per SECURITY.md §9" for
anything Valor-scoring-related.]
```

---

## Worked Example

```markdown
## [VAL-142] Award +1/-1 Valor on PvP kills and deaths

**Type:** Story
**Parent:** [VAL-100] (Epic: Core Progression & Combat — Milestone 1)
**Priority:** High

### Description
Valor is the server's core progression score. A player who kills another player
should gain a Valor Point, and a player killed by another player should lose
one, so that Valor reflects PvP performance.

### Acceptance Criteria
- [ ] Killing a player awards the killer +1 Valor Point and shows the "gained"
  message from docs/valor-system.md
- [ ] Dying to a player deducts 1 Valor Point (floored at 0) and shows the
  "lost" message
- [ ] Score never goes below 0
- [ ] Crossing a tier threshold (every 4 points) fires ValorRankChangedEvent
- [ ] Changes persist across restarts

### Definition of Done
- [ ] Code implemented following ARCHITECTURE.md / CODING_STANDARDS.md
- [ ] Unit tests: ValorScoreServiceTest covering award/deduct/floor/tier-change cases
- [ ] Integration tests: CombatListenerIntegrationTest
- [ ] docs/valor-system.md and docs/combat.md kept in sync
- [ ] CONFIGURATION.md updated with `progression.tier-thresholds`
- [ ] EVENTS.md updated with PlayerKilledEvent / ValorRankChangedEvent
- [ ] PR merged with human approval
- [ ] Deployed and post-deploy-verified

### Technical Notes
See FEATURES.md's worked example for this exact feature — full design already
sketched there. Score mutation goes exclusively through ValorScoreService
(SECURITY.md §9); the floor at 0 lives in the service, not the repository.

### Testing Notes
Explicitly test the duplication path (a single death must never be counted as
multiple kills) per SECURITY.md §9, not just the happy path.
```

## Ticket ID Convention

`VAL-<sequential number>`, assigned by whatever tracker is in use (GitHub Issues number, or a manually maintained counter if tickets are tracked in a flat file before a tracker is set up). IDs are never reused, even for a closed/abandoned ticket.

## Linking Tickets to Code

Every commit and PR references its ticket ID per `AGENTS.md` §10's commit/PR conventions (`Refs: VAL-142`). This is what makes it possible to trace "why does this code exist" back to its originating decision months later.
