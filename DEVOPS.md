# DEVOPS.md — The Valor SMP

CI/CD pipeline, branch strategy, versioning, artifact publishing, and rollback procedure.

---

## 1. Branch Strategy

- `main` is always releasable. Every commit on `main` has passed CI and been human-approved via PR.
- Short-lived feature/fix/refactor/chore/docs branches per `AGENTS.md` §10 naming convention, branched from `main`, merged back via PR (squash-merge preferred, to keep `main`'s history one-commit-per-PR and readable).
- No long-lived `develop` branch — at this project's scale, a single trunk with short-lived branches is simpler and sufficient. Revisit via `DECISIONS.md` only if release cadence/parallel-release-line needs genuinely require it.
- Release branches (`release/X.Y.Z`) are cut from `main` only if a release needs stabilization time separate from ongoing `main` development; otherwise, releases are tagged directly off `main`.

## 2. CI Pipeline (GitHub Actions)

Triggered on every PR and every push to `main`.

```yaml
# .github/workflows/ci.yml (illustrative structure)
name: CI
on:
  pull_request:
  push:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - run: ./gradlew check
      - run: ./gradlew integrationTest
      - name: Dependency license & CVE scan
        run: ./gradlew dependencyCheckAnalyze
      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: build/reports/tests/
```

CI stages, in order, any of which failing blocks merge:

1. Compile (`compileJava`, `compileTestJava`).
2. Static analysis / lint (whatever tool is configured — Checkstyle/SpotBugs/Error Prone or equivalent).
3. Unit tests.
4. Integration tests (MockBukkit + real SQLite).
5. Dependency license/CVE scan (`SECURITY.md` §6).
6. Build the plugin JAR artifact (proves packaging works, even though it isn't published on every PR — only on release, per §4).

## 3. CD / Release Pipeline

Triggered on a version tag (`vX.Y.Z`) pushed to `main`.

```yaml
# .github/workflows/release.yml (illustrative structure)
name: Release
on:
  push:
    tags: ['v*.*.*']

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - run: ./gradlew build
      - name: Publish artifact
        run: ./gradlew publish   # to internal artifact store / GitHub Releases
      - name: Deploy to staging
        run: ./scripts/deploy-staging.sh
      - name: Smoke test staging
        run: ./scripts/smoke-test.sh
      - name: Deploy to production
        run: ./scripts/deploy-production.sh
        if: success()
```

Deployment to production is gated on a passing staging smoke test. A manual approval gate before the production deploy step is recommended for this project's risk profile (real player data, real Valor standings) — configure as a GitHub Environments protection rule requiring a human approval, rather than fully automatic production deploys, until the release process has a long track record of staging smoke tests reliably catching problems.

## 4. Versioning

Semantic Versioning (`MAJOR.MINOR.PATCH`), per `RELEASE.md`:

- `MAJOR` — breaking changes to the public API (`API.md`) or a data migration requiring operator action beyond "just update the jar."
- `MINOR` — new features, backward-compatible.
- `PATCH` — bug fixes, no new features, backward-compatible.

Version is set in `build.gradle.kts` and tagged in git; the CI release job derives the artifact filename from the tag, not a manually-edited version string, to avoid drift between the two.

## 5. Artifact Publishing

- The built plugin JAR is attached to the corresponding GitHub Release for that tag.
- If an internal artifact repository (e.g., a private Maven/Gradle repo) is used for the `net.thevalorsmp.api` artifact so other in-house plugins can depend on it at compile time, that publish step is separate from the main plugin JAR release and versioned independently per `API.md` §6.

## 6. Rollback Strategy

If a release causes a production issue:

1. **Immediate mitigation**: if the issue is severe (data integrity, server crash loop), stop the server, restore the previous plugin JAR version, restart. Do not attempt a live hotfix under pressure — revert first, fix calmly afterward.
2. **Data considerations**: if the problematic release included a migration, confirm whether the migration is safe to have "run and stay" while reverting the *code* to the previous version (additive migrations usually are), or whether a down-migration/restore-from-backup is needed (destructive migrations — should be rare per `DATABASE.md` §3's additive-first policy).
3. **Root cause**: once stable, follow the debugging process (`PROMPTS.md` "Investigate a Bug") to find the actual defect before re-attempting the release.
4. **Retrospective**: per `AI_WORKFLOW.md` Stage 10, write up what happened and whether a checkpoint (design review, staging smoke test coverage) should have caught this earlier — update that checkpoint's process if so.

## 7. Environments

| Environment | Purpose | Data |
|---|---|---|
| Local (developer machine) | Individual development/testing | Disposable, fake |
| CI | Automated test execution | Ephemeral, generated per run |
| Staging | Pre-production smoke testing of a release candidate | Anonymized/synthetic copy of production shape, not real player PII where avoidable |
| Production | The live Valor SMP server | Real player data |

No developer or CI job ever has direct access to the production database credentials; staging and production are fully separate credential sets, per `SECURITY.md` §7.
