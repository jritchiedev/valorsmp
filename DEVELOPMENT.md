# DEVELOPMENT.md — The Valor SMP

Practical day-to-day development guide, distinct from `REPOSITORY.md` (repo orientation) and `DEVOPS.md` (CI/CD) — this is "how do I actually sit down and work on this codebase productively."

---

## 1. Setup

1. Install JDK 21 (Temurin recommended) and confirm `java -version` reports 21.
2. Clone the repository.
3. Import into your IDE of choice as a Gradle project (IntelliJ IDEA is well-supported; VS Code with the Java extension pack works too).
4. Run `./gradlew build` once to confirm a clean baseline build before making changes.
5. (Optional, for manual testing) set up a local Paper test server per `REPOSITORY.md` §5.

## 2. IDE Configuration

- Enable the project's formatter config (see `.editorconfig` at repo root; if the IDE has a corresponding code-style import, use it) so formatting matches CI's expectations and doesn't produce noisy whitespace diffs.
- Enable annotation processing if using Lombok-style code-generation annotations anywhere in the dependency set (check `build.gradle.kts` for what's actually in use before assuming).
- Point the IDE's JDK setting at JDK 21 specifically for this project, even if your default IDE JDK is newer/older for other projects.

## 3. Day-to-Day Loop

```bash
./gradlew compileJava        # fast feedback while iterating
./gradlew test               # unit tests only, fast
./gradlew integrationTest    # MockBukkit integration tests, slower
./gradlew check              # everything CI runs, before pushing
```

Run `./gradlew check` before opening a PR — don't rely on CI to be your first feedback loop for basic compile/test failures.

## 4. Working on a Feature

Follow `AI_WORKFLOW.md`'s stages regardless of whether you're a human or an AI agent — the process doesn't change based on who's driving. Practically, day to day:

1. Create a branch per `AGENTS.md` §10's naming convention.
2. Read the relevant `docs/<feature>.md` page and any linked ticket.
3. Implement in layer order (model → repository → service → listener/command → config → docs), committing at coherent checkpoints.
4. Run the local test loop above frequently, not just at the end.
5. Self-review against `AGENTS.md` §14's checklist before opening the PR.

## 5. Debugging Against a Local Server

- Attach your IDE's debugger to the local Paper server process (standard remote-JVM-debug attach, with the appropriate `-agentlib:jdwp=...` JVM flag added to the test server's startup script) to step through code interactively.
- For faster iteration without a full server restart per change, prefer writing a MockBukkit integration test that reproduces the scenario over manually clicking through a real server repeatedly — it's faster and becomes a permanent regression guard.

## 6. Common Local Issues

| Symptom | Likely Cause | Fix |
|---|---|---|
| Plugin fails to enable, "migration failed" in console | Local test server's `plugins/TheValorSMP/data.db` is from an older schema version incompatible with in-progress migration work | Delete the local test database (it's disposable local dev data) and let it re-migrate from scratch, or point at a fresh test server directory |
| `NoClassDefFoundError` for a Paper API class at runtime | Local Paper server jar version older than the API version targeted in `build.gradle.kts` | Update the local test server's Paper jar to a matching/newer version |
| MockBukkit test failing with a `HandlerList` related error | A custom event class's static `HANDLERS` field wasn't reset between tests | Ensure `MockBukkit.unmock()` runs in `@AfterEach`; avoid static state leaking across test classes |
| Gradle build slow on first run | Dependency resolution / Gradle daemon cold start | Subsequent runs are faster; consider enabling the Gradle build cache if not already default in `gradle.properties` |

## 7. Getting Help

If stuck beyond reasonable independent effort: check `DECISIONS.md` for prior precedent, check `AI_CONTEXT.md`/`docs/*.md` for domain context you might be missing, and if still stuck, escalate per `AGENTS.md` §13 rather than pushing forward on a guess for anything security- or data-integrity-sensitive.
