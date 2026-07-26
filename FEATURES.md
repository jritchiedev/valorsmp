# FEATURES.md — The Valor SMP

Standard template every feature follows, from concept to shipped-and-documented. Use this as the skeleton for any new `docs/<feature>.md` page and as the checklist for what a "complete" feature implementation includes.

---

## Feature Template

```markdown
# <Feature Name>

## User Story
As a [player/operator], I want to [action], so that [benefit].

## Acceptance Criteria
- [ ] Specific, testable condition 1
- [ ] Specific, testable condition 2
- [ ] ...

## Architecture
- Services introduced/modified: [...]
- Repositories introduced/modified: [...]
- Models introduced/modified: [...]
- Domain events introduced: [...]
- Config keys introduced: [...]

## Classes
| Class | Layer | Responsibility |
|---|---|---|
| ... | ... | ... |

## Data Model
[Table(s) affected, referencing DATABASE.md]

## Tests
- Unit: [list of test classes/key scenarios]
- Integration: [list of test classes/key scenarios]

## Documentation
- [ ] docs/<feature>.md created/updated
- [ ] CONFIGURATION.md updated if new config keys added
- [ ] API.md updated if public API surface changed
- [ ] EVENTS.md updated if new domain events added

## Review Checklist
- [ ] Follows ARCHITECTURE.md layering
- [ ] Follows CODING_STANDARDS.md
- [ ] Meets TESTING.md minimum coverage bar
- [ ] No SECURITY.md concerns unaddressed
- [ ] Config defaults are sane and documented
```

---

## Worked Example: "Land Claim Flags" Feature

```markdown
# Land Claim Flags

## User Story
As a claim owner, I want to toggle specific protections (PvP, mob spawning,
explosions) within my claim, so that I can customize how my land behaves.

## Acceptance Criteria
- [ ] Claim owner can set a flag via `/claim flag <key> <value>`
- [ ] Non-owners (without a delegated permission) cannot change flags
- [ ] Flags persist across server restarts
- [ ] Unset flags fall back to server-wide defaults from config
- [ ] Invalid flag keys/values are rejected with a clear player-facing message

## Architecture
- Services modified: `LandClaimService` (add `setFlag`, `getFlag`)
- Repositories modified: `LandClaimRepository` (add flag read/write methods
  backed by `land_claim_flags`)
- Models modified: `LandClaim` gains a `Map<ClaimFlag, String> flags` field
- Domain events introduced: `LandClaimFlagChangedEvent`
- Config keys introduced: `land-claims.default-flags.*`

## Classes
| Class | Layer | Responsibility |
|---|---|---|
| `ClaimFlag` | model (enum) | Known flag keys and their value types |
| `LandClaimService#setFlag` | service | Validates permission + value, persists, fires event |
| `SqlLandClaimRepository` | repository | Reads/writes `land_claim_flags` |
| `ClaimFlagCommand` | command | Parses `/claim flag ...`, calls service, reports result |
| `ClaimProtectionListener` | listener | Consults `LandClaimService#getFlag` on relevant Bukkit events (e.g., `EntityDamageByEntityEvent` for PvP flag) |

## Data Model
See `DATABASE.md` §"land_claim_flags". No migration needed if the table
already exists from initial claims implementation; otherwise, additive
migration `V<n>__add_land_claim_flags.sql`.

## Tests
- Unit: `LandClaimServiceTest#setFlag_asOwner_succeeds`,
  `#setFlag_asNonOwnerWithoutPermission_rejected`,
  `#getFlag_unset_returnsConfigDefault`
- Integration: `ClaimFlagCommandIntegrationTest` (MockBukkit) verifying command
  parsing and player-facing feedback messages;
  `ClaimProtectionListenerIntegrationTest` verifying PvP flag actually
  cancels damage when set to `false`

## Documentation
- [x] docs/future-features.md's land-claims section updated (or promoted to
  its own docs/land-claims.md if this is the feature that crosses that
  threshold)
- [x] CONFIGURATION.md updated with `land-claims.default-flags.*`
- [ ] API.md — not applicable, no external API change
- [x] EVENTS.md updated with `LandClaimFlagChangedEvent`

## Review Checklist
- [x] Follows ARCHITECTURE.md layering
- [x] Follows CODING_STANDARDS.md
- [x] Meets TESTING.md minimum coverage bar
- [x] No SECURITY.md concerns unaddressed (permission check tested explicitly)
- [x] Config defaults are sane (all flags default to server's existing
  vanilla-equivalent behavior unless explicitly documented otherwise)
```
