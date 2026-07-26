# CODING_STANDARDS.md — The Valor SMP

Concrete style and structure rules. Where this document and a linter/formatter config disagree, the linter config wins for anything mechanical (whitespace, import order); this document governs everything requiring judgment.

---

## 1. Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Package | lowercase, no underscores, feature-named | `net.thevalorsmp.landclaims` |
| Class | UpperCamelCase, descriptive noun (+ role suffix) | `LandClaimService`, `SqlLandClaimRepository` |
| Interface | UpperCamelCase, no `I` prefix | `LandClaimRepository` (not `ILandClaimRepository`) |
| Method | lowerCamelCase, verb-first | `createClaim`, `findByOwner` |
| Boolean method/field | reads as a yes/no question | `isOwner()`, `hasPermission()` |
| Constant | `UPPER_SNAKE_CASE` | `DEFAULT_CLAIM_RADIUS` |
| Test method | `methodUnderTest_condition_expectedResult` | `createClaim_overlappingExisting_returnsTooClose` |
| Generic type parameter | single uppercase letter or short descriptive (`T`, `ID`) | |

Avoid abbreviations that aren't immediately obvious (`cfg` is fine for `config` in a local variable; `mgr` for `manager` is borderline — prefer the full word in anything public).

## 2. Package Conventions

Every feature package follows the internal shape from `ARCHITECTURE.md` §9: `model/`, `repository/`, `service/`, `events/`, `listener/`, `command/`. Cross-cutting code (DI wiring, shared utilities) lives in `core/` and `util/`, not duplicated per-feature.

## 3. Class Conventions

- One public top-level class per file, filename matches class name.
- Prefer `final` classes unless deliberately designed for extension (and if so, document the extension contract in Javadoc).
- Favor composition over inheritance. Inheritance is reserved for genuine is-a relationships with shared behavior (e.g., a base `AbstractQuest` type if multiple quest types share substantial logic) — not for code reuse convenience alone.
- Records (`record LandClaimId(UUID value)`) are preferred over plain classes for simple immutable data carriers with no behavior beyond equality/accessors.

## 4. Method Conventions

- Soft ceiling: ~40 lines. If longer, look for an extractable sub-step.
- Parameters: prefer 4 or fewer. Beyond that, introduce a parameter object (e.g., a `ClaimCreationRequest` record) rather than a long positional parameter list.
- Avoid boolean parameters that change method behavior in non-obvious ways (`createClaim(owner, origin, radius, true)` — what does `true` mean at the call site?). Use an enum or split into two named methods instead.
- Early-return for validation/guard clauses rather than deeply nested `if` blocks.

## 5. Null Safety

- Never return `null` from a method whose return type is an object type, unless explicitly and consistently documented as nullable with `@Nullable` — and even then, prefer `Optional<T>` in almost all cases. The only common exception is methods intentionally mirroring a Java/Bukkit API convention that itself returns `null` (rare; call it out in Javadoc when it happens).
- Use `Optional<T>` for "may legitimately be absent" return values.
- Use `@NotNull`/`@Nullable` (JetBrains annotations, already a project dependency) on fields, parameters, and return types where the type alone (`Optional`) doesn't already communicate nullability.
- Validate constructor/method arguments at the boundary (`Objects.requireNonNull`, or a small validation helper) rather than letting a `NullPointerException` surface deep in unrelated code later.

## 6. Logging

- Use the plugin's SLF4J-backed logger (`plugin.getSLF4JLogger()` on Paper, or an injected `Logger` in services) exclusively. No `System.out.println`, no `Bukkit.getLogger()` directly inside services (services shouldn't depend on `Bukkit` statically at all where avoidable).
- Log levels:
  - `ERROR`: something failed that shouldn't have (I/O failure, unexpected exception caught at a boundary).
  - `WARN`: something recoverable but noteworthy (a config value was invalid and a default was substituted).
  - `INFO`: significant lifecycle events (plugin enabled, migration ran, season ended).
  - `DEBUG`: detailed diagnostic info, off by default, useful when investigating a specific issue.
- Never log a player's full inventory, chat content indiscriminately, or anything sensitive at `INFO` or above by default.

## 7. Exceptions

- Checked exceptions are avoided in this codebase's own APIs (Java's checked exception model doesn't compose well with lambdas/streams used throughout); wrap unavoidable checked exceptions from libraries (e.g., `SQLException`) at the repository boundary into an unchecked `RepositoryException` (extends `RuntimeException`), documented as part of the repository's contract.
- Never catch `Exception` broadly and swallow it silently. Catch the specific exception type you can meaningfully handle; let everything else propagate to the top-level Bukkit event/command error boundary, which logs it.
- Expected business outcomes (insufficient funds, claim overlap) are **not** modeled as exceptions — use a result type per `SERVICES.md` §2.

## 8. Performance

- No blocking I/O (database calls, file reads) on the main server thread from a listener/command unless the operation is proven trivially fast and bounded (see `PERFORMANCE.md`).
- Avoid allocating in hot paths (per-tick or high-frequency event handlers) where avoidable — reuse buffers/collections where profiling justifies it, but don't prematurely micro-optimize cold paths at the cost of readability.
- Prefer `EnumMap`/`EnumSet` over `HashMap`/`HashSet` when keys are a known enum.

## 9. Paper API Rules

- Use Adventure `Component` for all player-facing text; never legacy `ChatColor`-coded `String`s in new code.
- Prefer Paper-specific APIs over Bukkit/Spigot equivalents when they're more efficient or more correct (e.g., Paper's async chunk loading API over blocking `getChunkAt`).
- Never use NMS (`net.minecraft.server`) or reflection into internals unless there is genuinely no public API path, and if so, isolate it behind a narrow internal abstraction with a comment explaining exactly what Paper/Spigot version compatibility risk this introduces, and file a `DECISIONS.md` entry.
- Register all listeners/commands through the plugin's lifecycle (`CompositionRoot`), never via static initializers or ad-hoc registration scattered through the codebase.

## 10. Dependency Injection

- Constructor injection only, per `ARCHITECTURE.md` §3. No field injection, no static service locators (`ServiceLocator.get(LandClaimService.class)`-style patterns are forbidden — they hide dependencies and break testability).

## 11. Configuration Usage

- Services receive typed config objects via constructor injection (see `CONFIGURATION.md`), never call `plugin.getConfig().getInt(...)` directly.
- Config objects are validated at load time (fail fast with a clear error at `onEnable`, not a silent `NullPointerException` deep in a service later).

## 12. Documentation Expectations

- Every public class has a class-level Javadoc explaining its role in one or two sentences, referencing the relevant `ARCHITECTURE.md`/`SERVICES.md`/`REPOSITORIES.md` section where useful.
- Every public method on a service or repository interface has Javadoc describing its contract (see `SERVICES.md` §2 example).
- Non-obvious code (a genuine algorithmic subtlety, a workaround for a Paper API quirk) gets an inline comment explaining *why*, with a link to relevant tracking issue/documentation if applicable.

## 13. Formatting

- 4-space indentation, no tabs.
- Line length target: 120 characters soft limit.
- Import order and unused-import removal enforced by the project's configured formatter/linter (see `DEVOPS.md` for the exact tool wired into CI) — do not hand-format against the tool's decisions.
