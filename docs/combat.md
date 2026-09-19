# Combat

Defines PvP rules and how combat outcomes feed into the Valor system.

---

## 1. Goals

- PvP should feel fair and intentional, not accidental (no griefing an AFK player for free Valor).
- Combat outcomes are the **only** Valor-earning path (`docs/valor-system.md`) and must be resistant to farming/abuse.

## 2. No Combat Tagging

There is **no combat tagging** on the server. Players are never "tagged" for engaging in PvP — logging out, teleporting, or otherwise disengaging mid-fight is always permitted and carries no combat-state penalty.

## 3. Kills and Valor

- A PvP kill grants the killer **+1 Valor Point**; dying to a player costs the victim **-1 Valor Point** (floored at 0), per `docs/valor-system.md`.

## 4. Death Handling

- On a PvP death: `PlayerKilledEvent` fires (`EVENTS.md`), consumed by `ValorScoreService` (score award/deduction) and `LeaderboardService` (stat update).
- Item/inventory drop behavior on PvP death is **drop everything**.

## 5. Item Cooldowns

- **Mace:** 60-second server-enforced attack cooldown (`combat.mace-cooldown-seconds`), also shown via the vanilla hotbar cooldown overlay; maces cannot be enchanted at a table or anvil.
- **Spears:** vanilla spear items (`Tag.ITEMS_SPEARS`) only — there is no custom spear item or `/valorspear` command. Using a spear enchanted with vanilla **Lunge** applies a 30-second vanilla item cooldown (`combat.spear-lunge-cooldown-seconds`), visible in the hotbar and blocking re-use. Unenchanted spears are unaffected.

## 6. Open Questions

- None currently; the +1/-1 Valor model is fixed by the source spec.
