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
anything economy-related.]
```

---

## Worked Example

```markdown
## [VAL-142] Allow claim owners to toggle per-claim protection flags

**Type:** Story
**Parent:** [VAL-100] (Epic: Land & Social Systems — Milestone 2)
**Priority:** High

### Description
Claim owners currently get an all-or-nothing protection package. They need
granular control (PvP on/off, mob spawning on/off, explosions on/off) within
their own claim.

### Acceptance Criteria
- [ ] `/claim flag <key> <value>` lets the owner (or a permitted delegate) set
  a flag on their claim
- [ ] Non-owners without permission cannot change flags
- [ ] Flags persist across restarts
- [ ] Unset flags fall back to `land-claims.yml`'s configured defaults
- [ ] Invalid flag key/value combinations are rejected with a clear message

### Definition of Done
- [ ] Code implemented following ARCHITECTURE.md / CODING_STANDARDS.md
- [ ] Unit tests: LandClaimServiceTest covering owner/non-owner/invalid-value cases
- [ ] Integration tests: ClaimFlagCommandIntegrationTest,
  ClaimProtectionListenerIntegrationTest
- [ ] docs/future-features.md's land-claims section updated (or promoted to
  docs/land-claims.md)
- [ ] CONFIGURATION.md updated with `land-claims.default-flags.*`
- [ ] EVENTS.md updated with LandClaimFlagChangedEvent
- [ ] PR merged with human approval
- [ ] Deployed and post-deploy-verified

### Technical Notes
See FEATURES.md's worked example for this exact feature — full design already
sketched there. Reuses existing LandClaimService/LandClaimRepository; adds new
methods rather than new classes.

### Testing Notes
Explicitly test the permission-denial path (non-owner attempting to set a flag)
since this is a protection-integrity concern, not just a happy-path feature.
```

## Ticket ID Convention

`VAL-<sequential number>`, assigned by whatever tracker is in use (GitHub Issues number, or a manually maintained counter if tickets are tracked in a flat file before a tracker is set up). IDs are never reused, even for a closed/abandoned ticket.

## Linking Tickets to Code

Every commit and PR references its ticket ID per `AGENTS.md` §10's commit/PR conventions (`Refs: VAL-142`). This is what makes it possible to trace "why does this code exist" back to its originating decision months later.
