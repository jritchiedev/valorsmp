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

## Worked Example: "Valor Kill/Death Points" Feature

```markdown
# Valor Kill/Death Points

## User Story
As a player, I want to gain a Valor Point for killing another player and lose
one when I'm killed by a player, so that my Valor score reflects my PvP record.

## Acceptance Criteria
- [ ] Killing a player awards the killer +1 Valor Point
- [ ] The killer sees "You've gained 1 Valor Point from killing a player. You
      now have <current valor> Valor Points."
- [ ] Dying to a player deducts 1 Valor Point from the victim (floored at 0)
- [ ] The victim sees "You've lost 1 Valor Point due to dying. You now have
      <current valor> Valor Points."
- [ ] Score never goes below 0
- [ ] A tier change (every 4 points) is reflected immediately

## Architecture
- Services introduced: `ValorScoreService` (add `awardKill`), `CombatService`
- Repositories introduced: `ValorScoreRepository` (backed by `valor_scores`)
- Models introduced: `ValorTier`
- Domain events introduced: `PlayerKilledEvent`, `ValorRankChangedEvent`
- Config keys introduced: `valor.tiers.*`

## Classes
| Class | Layer | Responsibility |
|---|---|---|
| `ValorTier` | model (enum) | Ordered tiers I–V with thresholds and perk metadata |
| `ValorScoreService#awardKill` | service | Applies +1/-1 (floored at 0), computes tier, fires event |
| `SqlValorScoreRepository` | repository | Reads/writes `valor_scores` |
| `CombatListener` | listener | Translates `PlayerDeathEvent` (player killer) into a service call and messages both players |

## Data Model
See `DATABASE.md` §"valor_scores". Created by migration
`V2__init_valor_scores.sql`.

## Tests
- Unit: `ValorScoreServiceTest#awardKill_incrementsKillerDecrementsVictim`,
  `#awardKill_victimAtZero_staysAtZero`,
  `#awardKill_crossingThreshold_firesRankChanged`
- Integration: `CombatListenerIntegrationTest` (MockBukkit) verifying a PvP
  death awards/deducts points and sends the correct player-facing messages

## Documentation
- [x] docs/valor-system.md and docs/combat.md updated
- [x] CONFIGURATION.md updated with `valor.tiers.*`
- [ ] API.md — not applicable, no external API change
- [x] EVENTS.md updated with `PlayerKilledEvent`, `ValorRankChangedEvent`

## Review Checklist
- [x] Follows ARCHITECTURE.md layering
- [x] Follows CODING_STANDARDS.md
- [x] Meets TESTING.md minimum coverage bar
- [x] No SECURITY.md concerns unaddressed (score floor enforced in service)
- [x] Config defaults are sane (tier thresholds match docs/progression.md)
```
