# Changelog

All notable changes to The Valor SMP are documented here, following
[Keep a Changelog](https://keepachangelog.com/) and SemVer (`RELEASE.md`).

## [0.01-alpha] - 2026-08-01

First playable alpha: the core Valor loop, tiers/perks, and custom item mechanics on
Paper 1.21.11. See `docs/alpha-setup.md` for setup and testing.

### Added
- **Valor loop:** `+1` Valor on a PvP kill and `-1` on a death to a player (floored at 0),
  with the exact player-facing messages; drop-everything on PvP death. No combat tagging.
- **Tiers I–V** derived from configurable thresholds (`0,5,9,13,17`); `ValorRankChangedEvent`
  fires on tier change. `/valor` shows your score and tier.
- **Tier perks:** permanent Speed/Strength effects for tiers III–V; `/speed set 1|2` for
  Valor IV/V; **extended potion durations** for Valor II+ (8:00→10:00, 1:30→3:00).
- **Mace:** 30-second attack cooldown; non-enchantable via enchantment table or anvil.
- **Spears** (wood–netherite) as custom items with a **lunge** right-click dash on a
  10-second cooldown; `/valorspear` operator grant command.
- **Dragon egg:** grants Strength III while carried and cannot be placed in an ender chest.
- **Simple Voice Chat** bundled with the server via `scripts/setup-alpha-server.sh`.
- Foundation: per-player profiles, per-feature validated config, SQLite/MariaDB storage
  with migrations, `/valorsmp health|version` diagnostics.

### Known limitations
- Spears use a base-item model until a resource pack lands.
- "String Duper Returns" (Fabric-only) is not included; no Paper equivalent exists.
- Config changes require a restart (no hot reload yet).
