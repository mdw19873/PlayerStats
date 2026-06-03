# Testing

PlayerStats uses **JUnit 5** with **AssertJ** assertions, **Mockito** for mocking,
**MockBukkit** for server-backed integration tests, and **JaCoCo** for coverage.

## Running the tests

```bash
mvn test                       # run the whole suite
mvn test -Dtest=UnitTest       # run a single test class
mvn test -Dtest=UnitTest#fallsBackToNumberForUnknown   # single method
```

Coverage is **report-only** (no enforced threshold). After `mvn test`, open:

```
target/site/jacoco/index.html
```

## Layout

The test tree mirrors the production tree package-for-package:

```
src/main/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/NumberFormatter.java
src/test/java/com/artemis/the/gr8/playerstats/core/msg/msgutils/NumberFormatterTest.java
```

Server-backed tests load the plugin's real bundled resources (`config.yml`,
`language.yml`) through MockBukkit, so no separate fixtures are needed yet. Any
future test fixtures should go under `src/test/resources/`.

## Conventions

- One test class per production class, named `<ClassName>Test`.
- Use `@DisplayName` for human-readable names and `@Nested` classes to group a
  method's cases (see `NumberFormatterTest`).
- Method names describe behaviour, not implementation:
  `fallsBackToNumberForUnknown`, not `testFromString2`.
- Prefer AssertJ (`assertThat(x).isEqualTo(...)`) over JUnit asserts for readability.
- Use `@ParameterizedTest` + `@CsvSource` for table-style cases (alias mappings, etc.).
- Keep tests deterministic: pin anything locale- or environment-dependent
  (e.g. `NumberFormatterTest` pins `Locale.US` because `DecimalFormat` grouping
  separators are locale-specific).

## Test tiers

The plugin leans heavily on singletons (`getInstance()`) and a handful of Bukkit
statics, so tests fall into three tiers:

1. **Pure** — no Bukkit runtime. Plain JUnit 5.
   Examples: `StringUtils`, `UnixTimeHandler`, `CommandCounter`, `NumberFormatter`.

2. **Enum-backed** — needs the Bukkit enums (`Statistic`/`Material`/`EntityType`)
   on the classpath but no running server. Still plain JUnit 5.
   Example: `Unit` (its `getTypeFromStatistic` inspects a real `Statistic`).

3. **Server-backed** — needs a simulated server. Use MockBukkit: load the plugin
   with `MockBukkit.load(Main.class)` in `@BeforeEach` and `MockBukkit.unmock()`
   in `@AfterEach` (see `MockBukkitTestBase`). Used for the singleton managers
   (`EnumHandler`, `LanguageKeyHandler`) and command parsing (`StatCommand`).

   `StatActionTest` is the exception: rather than a full server, it stubs the
   `OfflinePlayerHandler` singleton with Mockito (see the note below) because the
   fork/join computation only needs that one collaborator.

## Notes / testability seams

A few small production accommodations exist purely to keep classes testable; keep
them intact:

- `MyLogger`'s static initializer falls back to a standalone `Logger` when no
  Bukkit server is present, so utility classes that log can be unit-tested.
- Singletons retain their reset-on-read or reload behaviour that tests rely on
  to avoid cross-test state leakage (e.g. `CommandCounter.getCommandCounts()`
  drains its counters; drain it in `@BeforeEach`).
- `StatActionTest` runs on the `ForkJoinPool`, whose worker threads can't see a
  thread-local `Mockito.mockStatic`. Instead it injects a mock into
  `OfflinePlayerHandler`'s static `instance` field via reflection (and resets it
  to `null` in `@AfterEach`) so the stub is visible on every worker thread.

## Toolchain note

The Surefire config sets `-Dnet.bytebuddy.experimental=true`. Mockito's ByteBuddy
mock maker otherwise refuses to start on JDK versions it doesn't explicitly know
about yet (it fails with "Unknown Java version"). The flag lets it fall back to
its latest-known class-file version. Remove it once Mockito/ByteBuddy ships
explicit support for the JDK in use.
