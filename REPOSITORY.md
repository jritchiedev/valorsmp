# REPOSITORY.md — The Valor SMP

General orientation to this Git repository: what it is, how it's organized at the top level, and how to get a local build running. For architectural detail see `ARCHITECTURE.md`; for the data-access "Repository" *pattern*, see `REPOSITORIES.md` (different meaning of the word — this file is about the Git repository itself).

---

## 1. What's In This Repository

A single Gradle project producing one PaperMC plugin JAR: `the-valor-smp.jar`. There is no multi-module split at this time — the codebase is organized by Java package (see `ARCHITECTURE.md` §9), not by Gradle subproject. This is a deliberate simplicity choice; revisit only via a `DECISIONS.md` entry if the codebase grows large enough that build-time modularity becomes valuable.

## 2. Top-Level Layout

```
valor-smp/
├── AGENTS.md, ARCHITECTURE.md, ... (governance/process docs, repo root)
├── docs/                            (feature-specific design docs)
├── src/main/java/...                (plugin source)
├── src/main/resources/              (plugin.yml, default configs)
├── src/test/java/...                (unit + integration tests)
├── gradle/                          (Gradle wrapper files)
├── build.gradle.kts
├── settings.gradle.kts
├── .github/workflows/                (CI/CD pipelines, see DEVOPS.md)
├── .editorconfig
├── .gitignore
└── README.md
```

## 3. Requirements to Build Locally

- JDK 21 (Temurin recommended).
- Git.
- No local database server required for the default profile — SQLite is used out of the box for local development; MySQL is opt-in via config for testing that backend specifically (see `DATABASE.md`).

## 4. Building

```bash
./gradlew build          # compiles, runs tests, produces the plugin JAR
./gradlew check          # compile + tests + static analysis, no packaging
./gradlew test           # unit tests only
./gradlew integrationTest  # MockBukkit integration tests (separate source set)
```

The built JAR lands in `build/libs/the-valor-smp-<version>.jar`.

## 5. Running Locally Against a Test Server

This repository does not vendor a Paper server jar. To run locally:

1. Download a Paper server jar for the target Minecraft version into a scratch directory outside this repo (e.g., `~/paper-test-server/`).
2. Run it once to accept the EULA and generate a default `server.properties`.
3. Copy `build/libs/the-valor-smp-<version>.jar` into that server's `plugins/` directory.
4. Start the server, verify the plugin enables cleanly in the console log.

Do not commit a Paper server jar, world data, or `server.properties` into this repository.

## 6. Branch Strategy

See `DEVOPS.md` §"Branch Strategy" for the full policy. Summary: `main` is always releasable; feature/fix/refactor work happens on short-lived branches per the naming convention in `AGENTS.md` §10, merged via PR.

## 7. Where Things Live — Quick Reference

| I want to... | Look at / edit |
|---|---|
| Understand overall design philosophy | `AGENTS.md`, `ARCHITECTURE.md` |
| Understand a specific feature's intended behavior | `docs/<feature>.md` |
| Add a new service | `SERVICES.md`, `ARCHITECTURE.md` §2.2 |
| Add/change persistence | `REPOSITORIES.md`, `DATABASE.md` |
| Add a new domain event | `EVENTS.md` |
| Add a new config key | `CONFIGURATION.md` |
| Add/expose a public API for other plugins | `API.md` |
| Understand test expectations | `TESTING.md` |
| Understand release/versioning process | `RELEASE.md` |
| Understand CI/CD | `DEVOPS.md` |
| Find historical rationale for a past decision | `DECISIONS.md` |
| File or read a ticket | `TICKETS.md` (format), issue tracker (actual tickets) |

## 8. License & Ownership

The Valor SMP codebase is proprietary to its operating team unless a `LICENSE` file at the repository root states otherwise. Third-party dependencies retain their own licenses; see `SECURITY.md` §"Dependency Updates" for the license-compatibility check required before adding any new dependency.
