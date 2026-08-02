# TESTING.md — The Valor SMP

Testing strategy, tooling, and the minimum coverage bar for merging any change.

---

## 1. Test Levels

| Level | Scope | Tooling | Runs Against |
|---|---|---|---|
| Unit | Single service/repository/util in isolation | JUnit 5 + AssertJ | In-memory fakes, no Bukkit runtime, no real DB |
| Integration | Listener/command wiring, real SQL against a repository | JUnit 5 + MockBukkit + temp-file/in-memory SQLite | MockBukkit-simulated server, real (temp) database |
| Performance | Hot-path throughput/latency | JMH (for micro-benchmarks) or manual profiling with real server, per `PERFORMANCE.md` | Local test server |
| Regression | A previously-fixed bug doesn't reoccur | JUnit 5, same as unit/integration depending on the bug's layer | As appropriate to the bug |
| Acceptance | End-to-end feature behavior matches ticket's acceptance criteria | Manual smoke test against a local Paper server, or MockBukkit integration test where feasible | Local/staging server |

## 2. Unit Tests

- Target: all service and repository logic, all utility functions with any conditional logic.
- Use `InMemory<Noun>Repository` fakes (per `REPOSITORIES.md` §1) for service tests — never a real database connection in a unit test.
- Inject a controllable `Clock` for any time-dependent logic (cooldowns, expiry) rather than relying on wall-clock time in assertions.
- Minimum coverage per new/changed service method: happy path, at least one edge case (boundary values, empty collections, zero/negative numeric inputs where relevant), at least one failure/invalid-input case.

```java
class ValorScoreServiceTest {

    private InMemoryValorScoreRepository repository;
    private ValorScoreService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryValorScoreRepository();
        service = new ValorScoreService(repository, testConfig(), Clock.fixed(NOW, UTC));
    }

    @Test
    void awardKill_incrementsKillerDecrementsVictim() {
        var result = service.awardKill(KILLER, VICTIM);
        assertThat(result.killerScore()).isEqualTo(1);
        assertThat(result.victimScore()).isEqualTo(0); // was 0, floored
    }

    @Test
    void awardKill_victimAtZero_staysAtZero() {
        var result = service.awardKill(KILLER, VICTIM);
        assertThat(result.victimScore()).isZero();
    }

    @Test
    void awardKill_crossingThreshold_reportsTierChange() {
        for (int i = 0; i < 5; i++) {
            service.awardKill(KILLER, freshVictim());
        }
        assertThat(service.getTier(KILLER)).isEqualTo(ValorTier.II);
    }
}
```

## 3. Integration Tests (MockBukkit)

- Target: listener event handling, command parsing/execution, and any interaction with Bukkit API types that unit tests can't meaningfully exercise.
- Services under test in an integration test may still be mocked/stubbed if the integration test's purpose is proving the *wiring* works, not re-testing business logic already covered at the unit level.
- Use MockBukkit's simulated server/player objects; never require a real running Paper server for CI.

```java
class CombatListenerIntegrationTest {

    private ServerMock server;
    private PlayerMock killer;
    private PlayerMock victim;
    private ValorScoreService service; // real or stub, depending on test intent

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        killer = server.addPlayer();
        victim = server.addPlayer();
        service = new ValorScoreService(new InMemoryValorScoreRepository(), testConfig(), Clock.systemUTC());
        MockBukkit.load(TestValorPlugin.class, service);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void playerDeathByPlayer_awardsKillerAndMessagesBoth() {
        // arrange: simulate victim killed by killer
        // act: fire the PlayerDeathEvent through MockBukkit
        // assert: killer score +1, victim score floored at 0, both received the correct message
    }
}
```

## 4. Real-SQL Repository Tests

Every `Sql<Noun>Repository` has an integration test running against a temp-file or in-memory SQLite instance with the actual migrations applied, verifying:

- Round-trip save/find correctness.
- Query semantics under realistic conditions (e.g., `topN` actually returns the highest scores for a season in the right order).
- Atomicity of operations that must be atomic (`ValorScoreRepository#addScore` under concurrent-style test simulation).

## 5. Regression Tests

For any bug fix:

1. Write the test reproducing the bug.
2. Confirm it fails against the pre-fix code (state this in the PR description — e.g., "confirmed failing on commit `abc123` before the fix").
3. Apply the fix.
4. Confirm the test now passes.

This test then lives permanently in the suite as a regression guard — do not delete it once the immediate bug is old news.

## 6. Performance Testing

- Any change to a hot path (combat resolution per-hit, any per-tick listener, frequently invoked commands) gets a before/after measurement, per `PERFORMANCE.md`. This can be a simple wall-clock/TPS-impact measurement against a local test server if a full JMH benchmark isn't justified for the change's size.
- Performance claims in a PR description must be backed by an actual measurement, never an estimate presented as a measurement (`AI_RULES.md` §1.7).

## 7. Definition of Done (Testing Dimension)

A change is not done, regardless of what the ticket's functional acceptance criteria say, until:

- [ ] All new/changed service logic has unit tests per §2's minimum bar.
- [ ] All new/changed listener/command wiring has an integration test per §3.
- [ ] All new/changed repository SQL has a real-SQL test per §4.
- [ ] Any bug fix has a regression test per §5.
- [ ] `./gradlew check` passes (unit + integration + static analysis).
- [ ] No reduction in overall coverage without an explicit, reviewed justification in the PR description.

## 8. What NOT to Test

- Don't write tests asserting on private implementation details (a private helper method's exact intermediate value) — test observable behavior through the public interface.
- Don't write brittle tests asserting on exact log message text unless the log message itself is a documented contract.
- Don't duplicate the same assertion across unit and integration levels for the same logic — unit-test the business rule, integration-test the wiring, and let each level test what it's actually good at testing.
