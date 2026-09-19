# CONFIGURATION.md — The Valor SMP

How configuration is structured, loaded, validated, and used across the codebase.

---

## 1. Principles

- Anything a server operator might reasonably want to tune belongs in config, not in Java literals (`AI_CONTEXT.md` §5).
- Config is organized **per feature**, one YAML file per major feature area, not one monolithic `config.yml` — this keeps diffs small and merges clean when multiple features are worked on concurrently.
- Every config value has a documented default shipped in `src/main/resources/config/`, and every default is chosen to represent sane, vanilla-adjacent behavior unless a feature's whole purpose is to change that behavior.

## 2. File Layout

```
src/main/resources/config/
├── database.yml
├── combat.yml
├── progression.yml
├── ranks.yml
└── seasons.yml
```

Operator-facing copies land in `plugins/TheValorSMP/config/` at runtime, generated from these defaults on first run, and are not overwritten on subsequent updates (new keys are merged in; existing operator-set values are preserved) — see §5, Config Migration.

## 3. Typed Config Model Pattern

Each config file maps to a Java record (or small class hierarchy) deserialized at load time, injected into services via constructor — services never call `plugin.getConfig()` directly (`CODING_STANDARDS.md` §11).

```java
public record ValorTiersConfig(
    List<Integer> tierThresholds,
    boolean extendedPotionEffects
) {
    public static ValorTiersConfig load(ConfigurationSection section) {
        List<Integer> thresholds = section.getIntegerList("tier-thresholds");
        if (thresholds.size() != 5) {
            LOGGER.warn("progression.tier-thresholds must list 5 tiers; using defaults");
            thresholds = List.of(0, 5, 9, 13, 17);
        }
        // ... remaining fields, each validated similarly
        return new ValorTiersConfig(thresholds, section.getBoolean("extended-potion-effects", true));
    }
}
```

```yaml
# progression.yml
# Minimum Valor Points required to reach each tier (I..V).
tier-thresholds: [0, 5, 9, 13, 17]
extended-potion-effects: true
```

## 4. Validation Rules

- Every load method validates type, range, and (for enum-backed keys) membership.
- Invalid values log a `WARN` with the exact key, the invalid value, and the default being substituted — never fail silently, never crash `onEnable` over a single bad value (a typo in one feature's config shouldn't take down the whole plugin) unless the value is safety-critical enough that running with a default would be actively worse (rare; document explicitly if this path is taken for a specific key).
- Valor-affecting numeric config (tier thresholds, cooldown durations) is range-checked to prevent operator misconfiguration from creating an exploitable condition (`SECURITY.md` §3).

## 5. Config Migration (New Keys in Existing Files)

When a feature PR adds a new config key to an existing file:

- The loader must supply a safe default if the key is absent from an operator's existing file (never assume the key is present just because it's in the shipped default).
- The runtime-generated operator config file is updated to include the new key with its default value and an explanatory comment, without altering any key the operator has already customized.
- This is handled by the config-loading bootstrap's "merge defaults into existing file" step, exercised by a test in `ConfigBootstrapTest` for any new key added.

## 6. Reloading

- `/valorsmp reload` (permission-gated, `valorsmp.admin.reload`) re-reads config files and reconstructs config model objects, then re-injects them where feasible.
- Not every config change is safely hot-reloadable (e.g., changing `database.yml`'s backend requires a restart, since a live connection pool can't safely be swapped). Document any non-hot-reloadable key explicitly in its YAML file's comment and in this document's per-file notes below.

## 7. Per-File Notes

| File | Hot-reloadable? | Notes |
|---|---|---|
| `database.yml` | No | Requires restart to change backend/connection settings |
| `combat.yml` | Yes | Item cooldowns (mace 60s, Lunge-enchanted spear 30s), death-drop behavior |
| `progression.yml` | Yes | Valor tier thresholds, score cap (`max-valor`, default 20), and per-tier perk settings |
| `ranks.yml` | Partially | New staff rank definitions reload; currently-online players' displayed rank refreshes on next permission recalculation |
| `seasons.yml` | No | Season boundaries/timing should not change mid-season without an explicit `DECISIONS.md`-documented operational procedure |

## 8. Secrets

Database credentials, if using MySQL with a password, are **not** stored in `database.yml` directly in a way that would be committed anywhere — the shipped default file documents the environment-variable override mechanism (`VALORSMP_DB_PASSWORD`), and the operator's actual runtime config (outside version control) is where any real credential lives. See `SECURITY.md` §7.
