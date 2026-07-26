# The Valor SMP

A Java 21 / PaperMC survival multiplayer server plugin, built as production software. This repository
produces a single plugin JAR: `the-valor-smp-<version>.jar`.

## Requirements

- JDK 21 (Temurin recommended)
- Git

No database server is required for local development — SQLite is the zero-config default backend.

## Build and test

```bash
./gradlew compileJava       # fast feedback while iterating
./gradlew test              # unit tests only
./gradlew integrationTest   # MockBukkit integration tests
./gradlew check             # Checkstyle + unit + integration tests (what CI runs)
./gradlew build             # check + package build/libs/the-valor-smp-<version>.jar
```

## Running against a local Paper server

This repository does not vendor a Paper server jar. See `REPOSITORY.md` §5: download a Paper jar into a
scratch directory outside the repo, copy the built plugin JAR into its `plugins/`, and start the server.

## Layout

| Path | Contents |
|---|---|
| `src/main/java/net/thevalorsmp/core` | Plugin lifecycle, dependency-injection wiring |
| `src/main/java/net/thevalorsmp/config` | Config bootstrap and typed config models |
| `src/main/java/net/thevalorsmp/storage` | Connection pool, migration runner, storage exceptions |
| `src/main/resources/config` | Shipped default YAML config, one file per feature |
| `src/main/resources/db/migration` | `V<n>__<description>.sql` migrations |
| `src/test/java` | Unit tests (JUnit 5 + AssertJ) |
| `src/integrationTest/java` | MockBukkit integration tests |
| `docs/` | Feature design docs |

## Contributing

Read `AGENTS.md` first — it is the binding operating procedure for humans and AI agents alike.
`ARCHITECTURE.md`, `CODING_STANDARDS.md`, and `TESTING.md` define structure, style, and the minimum
test bar; `CONTRIBUTING.md` and `REVIEW.md` cover the PR process.
