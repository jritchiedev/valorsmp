# Combat

Defines PvP rules, combat tagging, and how combat outcomes feed into the Valor system and economy.

---

## 1. Goals

- PvP should feel fair and intentional, not accidental (no logging-out to escape a losing fight; no griefing an AFK player for free Valor).
- Combat outcomes are the primary Valor-earning path (`docs/valor-system.md` §3) and must be resistant to farming/abuse.

## 2. Combat Tagging

When a player deals or receives PvP damage from another player, both are "combat tagged" for a configurable duration (`combat.yml`, default 15 seconds).

- While tagged: logging out does not fully disconnect the player from the server's combat-tracking perspective for the tag's duration — implementation approach (e.g., a brief server-side "ghost" holding period, or a re-log penalty) is an implementation decision documented once `CombatService` is built; the *requirement* is that quitting mid-fight must not be a safe escape.
- Tag persists across a quick relog (per `AI_CONTEXT.md` §6 glossary) — reconnecting within the tag's duration re-applies the tagged state rather than clearing it, to prevent relog-to-escape as an alternate exploit of the same underlying problem.
- Tagged players may have movement/teleport restrictions (e.g., can't use an ender pearl to escape, can't `/tpa` away) — exact restrictions configurable in `combat.yml`.

## 3. What Counts as a "Tracked" Kill (for Valor purposes)

Not every PvP kill grants Valor. To prevent farming:

- Both players must have taken meaningful, roughly-recent combat actions against each other (not a one-shot sneak kill on someone who never fought back — configurable "meaningful engagement" threshold in `combat.yml`).
- A cooldown exists between the same two players' kills counting for full Valor reward repeatedly in a short window (prevents two colluding players from farming each other for Valor).
- Killing a player significantly lower-ranked grants sharply reduced (or zero) Valor, per `docs/valor-system.md` §3's rank-relative scaling — discourages farming new/weak players.

## 4. Combat Zones

- Land claims may set a `pvp` flag (`docs/future-features.md`'s land-claims section) disabling combat entirely within the claim's bounds, regardless of global combat rules.
- The server may designate specific world regions as permanently PvP-enabled or PvP-disabled (e.g., spawn is always PvP-disabled) via config, independent of claims.

## 5. Death Handling

- On a tracked PvP death: `PlayerKilledEvent` fires (`EVENTS.md`), consumed by `EconomyService` (potential loot/bounty transfer, if that mechanic is enabled), `ValorScoreService` (score award), `LeaderboardService` (stat update).
- Item/inventory drop behavior on PvP death is a server-operator-configurable choice (`combat.yml`: keep inventory, drop everything, drop a subset) — not hardcoded to vanilla default behavior, since this is a common point of server-specific tuning.

## 6. Class Responsibilities

| Class | Responsibility |
|---|---|
| `CombatService` | Business rules: is this a tracked kill, how much Valor/reward it grants, tagging eligibility rules |
| `CombatTagManager` | Runtime state: who's currently tagged, tag expiry scheduling |
| `CombatListener` | Bukkit adapter: translates `EntityDamageByEntityEvent`/`PlayerDeathEvent` into `CombatService` calls |
| `PlayerKilledEvent` | Domain event carrying the fact of a tracked PvP kill, for other features to react to |

## 7. Anti-Abuse Summary (cross-reference `SECURITY.md` §8)

- Farming prevention per §3 above.
- Rate-limiting on any command that could be abused to grief combat state (e.g., a hypothetical "flee" command, if ever added, would need its own cooldown).
- Combat-related exploits (duplicate death events, double-counted kills) are treated as high-severity per `SECURITY.md` §9's duplication-bug severity classification, since they directly affect Valor integrity.

## 8. Open Questions

- Exact numeric tag duration, engagement threshold, and rank-differential scaling curve are tuning decisions for `combat.yml`, not finalized in this design doc — expect iteration post-launch based on real player behavior data.
