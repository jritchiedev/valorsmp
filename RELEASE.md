# RELEASE.md — The Valor SMP

Versioning scheme, release cadence, changelog format, and the release checklist.

---

## 1. Versioning

Semantic Versioning: `MAJOR.MINOR.PATCH`.

- **MAJOR** — breaking `API.md` change, or a migration requiring manual operator action beyond replacing the jar (rare; avoid where possible per `DATABASE.md`'s additive-first policy).
- **MINOR** — new feature, backward-compatible.
- **PATCH** — bug fix only, no new feature, backward-compatible.

Pre-1.0 (`0.x.y`) versions may break compatibility in a `MINOR` bump instead of requiring a `MAJOR` bump, standard SemVer pre-1.0 convention — this project should move to `1.0.0` once the core systems in `ROADMAP.md`'s initial milestone are stable in production.

## 2. Release Cadence

No fixed calendar cadence mandated — releases happen when a coherent, tested set of changes on `main` is ready to ship, following `DEVOPS.md`'s CD pipeline. In practice, expect roughly weekly-to-biweekly minor/patch releases once the project is in active live operation, with hotfix patch releases as needed for urgent bug/security fixes outside the normal cadence.

## 3. Changelog Format

Maintained as `CHANGELOG.md` at repo root (generated/updated per release, following [Keep a Changelog](https://keepachangelog.com/) structure):

```markdown
## [1.4.0] - 2026-08-15

### Added
- Valor tier IV/V players can now switch their permanent Speed effect
  between Speed I and Speed II with `/speed set 1|2`. (#142)

### Changed
- The mace's cooldown is now enforced server-side and is no longer bypassable
  by rapid re-equipping. (#151)

### Fixed
- Fixed a race condition in Valor score updates that could, under rare
  concurrent conditions, cause a kill's +1 award to be lost. (#158)

### Security
- Patched a gap where a single death could, under lag, be counted as more than
  one kill for Valor purposes. (#160)
```

Categories used: `Added`, `Changed`, `Fixed`, `Security`, `Deprecated`, `Removed`, `Performance`. Omit empty categories for a given release. Written in player/operator-facing language, not internal implementation detail — see the release-notes prompt template in `PROMPTS.md`.

## 4. Release Checklist

- [ ] All PRs intended for this release are merged to `main` and CI is green on `main`.
- [ ] `CHANGELOG.md` updated for this version.
- [ ] Version bumped in `build.gradle.kts` following the SemVer rule above.
- [ ] Any new migrations have been tested against representative existing data (`DATABASE.md` §3).
- [ ] Any breaking `API.md` changes are called out prominently at the top of the changelog entry, with the deprecation history referenced.
- [ ] Git tag `vX.Y.Z` created and pushed, triggering the CD pipeline (`DEVOPS.md` §3).
- [ ] Staging smoke test passes.
- [ ] Production deploy approved and executed.
- [ ] Post-deploy verification performed (`AI_WORKFLOW.md` Checkpoint 4): server starts cleanly, no new startup errors, smoke test of the release's headline change(s) performed on the live server.

## 5. Hotfix Process

For an urgent production issue:

1. Branch `fix/<ticket-id>-<slug>` from `main` (or from the current release tag if `main` has since accumulated unrelated in-flight changes not ready to ship).
2. Minimal, targeted fix — resist the urge to bundle other pending work into a hotfix.
3. Full test coverage per `TESTING.md` still applies — "urgent" is not an excuse to skip the regression test.
4. Expedited review (still requires human approval, per `REVIEW.md` §6, but reviewers should prioritize it).
5. Release as a `PATCH` version immediately following the hotfix checklist above (abbreviated where genuinely safe to do so, but never skipping post-deploy verification).

## 6. Deprecation and Breaking Changes

- Any `API.md` deprecation gets at least one `MINOR` release cycle of coexistence (old + new both work) before removal in a subsequent `MAJOR` release, per `API.md` §4.
- Any config key rename/removal follows the same spirit: support both old and new key names for at least one release cycle where feasible, logging a `WARN` when the old key is used, before removing support for the old key.

## 7. Long-Term Support

No formal LTS branch policy at this project's current scale — the expectation is that operators stay reasonably current with releases. If this ever becomes a real operational pain point (e.g., a large operator community needing longer support windows), revisit via a `DECISIONS.md` entry rather than informally patching old versions on an ad hoc basis.
