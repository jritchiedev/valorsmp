# Valor SMP — Alpha Setup & Tester Guide (v0.01)

How to stand up the alpha server and what to test. This is an early alpha: the core Valor
loop, tiers/perks, and custom item mechanics are in; some extras are not yet.

---

## Requirements

- **Java 21** (Gradle and Paper 1.21.11 both require it). Point `JAVA_HOME` at a JDK 21.
- Minecraft **Java Edition 1.21.11** client.
- For voice: the **Simple Voice Chat** client mod (Fabric/Forge) — the matching server plugin is
  bundled automatically by the setup script. Players without it can still play; they just won't
  have voice.

## Build & run the server

```bash
export JAVA_HOME=/path/to/jdk-21
scripts/setup-alpha-server.sh run       # builds plugin, downloads Paper + Simple Voice Chat
cd run && "$JAVA_HOME/bin/java" -Xms1G -Xmx2G -jar paper-1.21.11.jar --nogui
```

The server uses SQLite by default (`plugins/TheValorSMP/config/database.yml`); no external
database is needed. Config is generated on first run under `plugins/TheValorSMP/config/`.

## What to test

### Valor loop
- Kill another player → `+1` Valor and the message *"You've gained 1 Valor Point…"*.
- Die to another player → `-1` Valor (never below 0) and the message *"You've lost 1 Valor Point…"*.
- `/valor` shows your current score and tier.
- On a PvP death you **drop everything** (no keep-inventory).

### Tiers & perks (every 4 points = next tier; max tier 5)
| Tier | Points | Perk |
|---|---|---|
| I | 0–4 | none |
| II | 5–8 | extended potion durations (8:00→10:00, 1:30→3:00) |
| III | 9–12 | permanent Speed I |
| IV | 13–16 | permanent Strength I + Speed II, `/speed set 1\|2` |
| V | 17–20 | permanent Strength II + Speed II, `/speed set 1\|2` |

Perks apply on join, after respawn, and when your tier changes.

### Custom items (all vanilla items; no operator grant command)
- **Mace:** 60-second attack cooldown, shown in the hotbar; cannot be enchanted at a table or anvil.
- **Spear + Lunge:** every jab (left-click) with a vanilla spear enchanted with Lunge puts it on a
  30-second vanilla item cooldown (hotbar-visible); a lunge attempted while on cooldown costs no
  hunger and the dash is cancelled server-side. The right-click charge attack is unaffected.
- **Dragon egg:** grants Strength III while it's in your inventory; **cannot** be placed in an
  ender chest.

## Operator commands
- `/valorsmp health|version` — diagnostics (permission `valorsmp.admin`).

## Known limitations (this alpha)
- "String Duper Returns" (a Fabric mod) is not included; there is no Paper equivalent.
- Config is applied at startup; there is no hot reload yet (restart to change config).
