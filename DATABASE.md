# DATABASE.md — The Valor SMP

Persistence architecture, schema reference, and migration process.

---

## 1. Supported Backends

| Backend | Use Case | Driver |
|---|---|---|
| SQLite | Default; small-to-mid servers, zero external setup | `org.xerial:sqlite-jdbc` |
| MySQL/MariaDB | Larger servers, multi-process tooling reading the same DB | `mysql-connector-j` / `mariadb-java-client` |

Backend is selected via `config/database.yml` (`CONFIGURATION.md`). Repository SQL is written to be backend-portable ANSI SQL wherever feasible; backend-specific syntax (e.g., `AUTOINCREMENT` vs `AUTO_INCREMENT`) is isolated in the migration runner's dialect layer, not scattered through repository query strings.

## 2. Connection Management

- A single `DataSource` (HikariCP-backed connection pool) is created in `ValorPlugin#onEnable`, sized conservatively by default (`maximumPoolSize: 10`, configurable) — this is a Minecraft plugin, not a web service; over-provisioning connections wastes resources for no benefit at this scale.
- The pool is closed last in `onDisable`, after all managers/services have flushed pending writes.
- Repositories receive the `DataSource` via constructor injection (through `CompositionRoot`), never a raw `Connection` held across calls.

## 3. Migration Process

- Migrations live in `src/main/resources/db/migration/`, following `V<sequence>__<description>.sql` naming (Flyway-compatible convention, whether or not Flyway itself is the runner — see `DECISIONS.md` for the specific tool choice once made).
- Migrations run automatically on `onEnable`, before any repository is constructed.
- **Additive-first policy**: prefer `ADD COLUMN`, new tables, and new indexes over `ALTER`/`DROP` on existing columns. If a column must be removed, do it in two releases: (1) stop writing to it and stop reading it in code, ship, confirm stable; (2) drop it in a later migration. This gives a safe rollback window.
- **Never** write a migration that unconditionally `DELETE`s or `DROP`s rows containing player-earned data without an explicit archival step first (copy to an `*_archive` table or export) — see `AGENTS.md` §12.
- Every migration that touches a table with existing production data must be tested against a copy of representative production-shaped data (realistic row counts and edge-case values), not just an empty schema, before merge.
- Down-migrations are written when the migration tool/version in use supports them; where not supported, the rollback plan is documented in the migration's PR description and in this file's changelog section instead.

## 4. Schema Reference

> This section is the living source of truth for table shapes. Update it in the same PR as any migration.

### `player_profiles`
| Column | Type | Notes |
|---|---|---|
| `uuid` | `CHAR(36)` PK | Player UUID |
| `first_joined_at` | `TIMESTAMP` | |
| `last_seen_at` | `TIMESTAMP` | |
| `display_name_cache` | `VARCHAR(16)` | Cached for offline lookups; not authoritative |

### `valor_scores`
| Column | Type | Notes |
|---|---|---|
| `uuid` | `CHAR(36)` | FK → `player_profiles.uuid` |
| `season` | `INT` | Part of composite PK with `uuid` |
| `score` | `BIGINT` | Never negative; floor at 0 enforced in service layer |
| `updated_at` | `TIMESTAMP` | |

### `wallets`
| Column | Type | Notes |
|---|---|---|
| `uuid` | `CHAR(36)` PK | FK → `player_profiles.uuid` |
| `balance_minor_units` | `BIGINT` | **Never** `DOUBLE`/`FLOAT`. Minor units (e.g., cents) per `docs/economy.md` |
| `updated_at` | `TIMESTAMP` | |

### `land_claims`
| Column | Type | Notes |
|---|---|---|
| `id` | `CHAR(36)` PK | |
| `owner_uuid` | `CHAR(36)` | Nullable if owned by a team instead |
| `team_id` | `CHAR(36)` | Nullable if owned by a player instead |
| `world` | `VARCHAR(64)` | |
| `min_x`,`min_z`,`max_x`,`max_z` | `INT` | Bounding box; indexed for intersection queries |
| `created_at` | `TIMESTAMP` | |

### `land_claim_flags`
| Column | Type | Notes |
|---|---|---|
| `claim_id` | `CHAR(36)` | FK → `land_claims.id` |
| `flag_key` | `VARCHAR(64)` | e.g. `pvp`, `mob-spawning` |
| `flag_value` | `VARCHAR(64)` | Stored as string, parsed by service layer |

### `teams` / `team_members`
| Table | Column | Type | Notes |
|---|---|---|---|
| `teams` | `id` | `CHAR(36)` PK | |
| `teams` | `name` | `VARCHAR(32)` | Unique |
| `teams` | `created_at` | `TIMESTAMP` | |
| `team_members` | `team_id`, `uuid` | `CHAR(36)` each | Composite PK |
| `team_members` | `role` | `VARCHAR(16)` | e.g. `owner`, `member` |

### `quests` / `quest_progress`
| Table | Column | Type | Notes |
|---|---|---|---|
| `quests` | `id` | `VARCHAR(64)` PK | Definition may be config-mirrored; DB row tracks metadata for stats |
| `quest_progress` | `uuid`, `quest_id` | Composite PK | |
| `quest_progress` | `progress_json` | `TEXT` | Structured progress payload, shape defined per quest type |
| `quest_progress` | `completed_at` | `TIMESTAMP` | Nullable |

### `achievements_unlocked`
| Column | Type | Notes |
|---|---|---|
| `uuid`, `achievement_id` | Composite PK | |
| `unlocked_at` | `TIMESTAMP` | |

### `server_events`
| Column | Type | Notes |
|---|---|---|
| `id` | `CHAR(36)` PK | |
| `event_type` | `VARCHAR(64)` | |
| `started_at`,`ended_at` | `TIMESTAMP` | `ended_at` nullable while active |
| `result_json` | `TEXT` | Nullable until concluded |

### `cosmetic_unlocks` / `cosmetic_equipped`
| Table | Column | Type | Notes |
|---|---|---|---|
| `cosmetic_unlocks` | `uuid`, `cosmetic_id` | Composite PK | |
| `cosmetic_equipped` | `uuid`, `slot` | Composite PK | e.g. slot = `hat`, `trail` |

### `crate_keys` / `crate_open_log`
| Table | Column | Type | Notes |
|---|---|---|---|
| `crate_keys` | `uuid`, `crate_type` | Composite key with `quantity` | |
| `crate_open_log` | `id` | `BIGINT` PK auto-increment | |
| `crate_open_log` | `uuid`,`crate_type`,`reward_id`,`opened_at` | | Audit trail for drop-rate fairness |

### `player_ranks`
| Column | Type | Notes |
|---|---|---|
| `uuid` | `CHAR(36)` PK | |
| `rank_key` | `VARCHAR(32)` | References config-defined rank, not FK to a DB table |

### `seasons` / `season_archives`
| Table | Column | Type | Notes |
|---|---|---|---|
| `seasons` | `season_number` | `INT` PK | |
| `seasons` | `started_at`,`ended_at` | `TIMESTAMP` | `ended_at` nullable while current |
| `season_archives` | `season_number`,`archived_table`,`archive_blob` | | Snapshot of season-scoped data at close, per `AGENTS.md` §12 |

## 5. Indexing Guidance

- Every foreign-key-shaped column (`uuid`, `team_id`, `claim_id`, etc.) is indexed.
- `land_claims` bounding-box columns are indexed together to support the intersection query pattern in `LandClaimRepository#findIntersecting` — application code narrows candidates via the box index, then does precise checks in Java rather than relying on full spatial SQL, which isn't portable across SQLite/MySQL without extensions.
- Avoid indexing low-cardinality columns alone (e.g., don't index `flag_key` by itself on a small table) — index only where query patterns in the actual repository code justify it.

## 6. Backup Expectations

Operationally (not code-enforced): nightly backup of the database file (SQLite) or a `mysqldump`/equivalent snapshot (MySQL) is expected as part of server operations, documented further in `DEVOPS.md`. Season-close archival (`season_archives`) is a defense-in-depth measure, not a replacement for regular backups.

## 7. Migration Changelog

| Version | Description | Type |
|---|---|---|
| `V1__init_player_profiles.sql` | Creates `player_profiles` | Additive |
| `V2__init_wallets.sql` | Creates `wallets` | Additive |
| `V3__init_valor_scores.sql` | Creates `valor_scores` | Additive |

New entries are appended here in the same PR as the migration file itself.
