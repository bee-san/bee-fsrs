# bee-fsrs

FSRS spaced-repetition memory mathematics for the JVM, in Kotlin.

Dependency-free apart from the Kotlin stdlib, with no clock and no I/O. Elapsed days
are an input, so every result is deterministic and reproducible — which is what makes
a stored schedule auditable years later.

```kotlin
val engine = FsrsEngine.latestDefault()

val first = engine.initialState(FsrsRating.GOOD)

val result = engine.review(
    FsrsReviewInput(
        previousState = first,
        rating = FsrsRating.GOOD,
        elapsedDays = 3,
        desiredRetention = 0.9,
        maximumInterval = 36_500,
    ),
)

result.nextIntervalDays   // when to show it again
result.nextState          // stability and difficulty to store
result.retrievability     // probability of recall at review time
```

## Which algorithm this is

**FSRS-6.x, the 21-parameter snapshot.** Specifically a port of
[`open-spaced-repetition/py-fsrs`](https://github.com/open-spaced-repetition/py-fsrs)
at tag `v6.3.1`.

Worth being precise about, because "FSRS 7" is sometimes used loosely: upstream
py-fsrs has published no v7, and its latest release is v6.3.1. The algorithm identity
is asserted in code by `FsrsAlgorithmInfo` and verified by a test, so a silent swap
fails the build rather than quietly rescheduling every learner's queue.

Full chain of custody, including the exact upstream commit and scheduler blob, is in
[PROVENANCE.md](PROVENANCE.md).

## Install

```kotlin
dependencies {
    implementation("dev.bee:bee-fsrs:0.1.0")
}
```

Requires JVM 17 or later.

## What this library does and does not do

It owns the memory mathematics:

- initial state from a first rating;
- retrievability given elapsed days;
- next difficulty and stability;
- next interval for a target retention;
- one complete review calculation.

It deliberately does **not** own scheduling policy. Which ratings your app permits,
how it decides a review happened, due-queue ordering, daily limits, persistence, and
timezone handling are all yours. That separation is why the engine can be pinned as a
tested artifact: it has no opinions to disagree with.

## Design rules

These are enforced, not aspirational — see [PROVENANCE.md](PROVENANCE.md):

1. **No dependencies** beyond the Kotlin stdlib. No coroutines, serialization,
   datetime, or logging, so a consumer inherits nothing transitively.
2. **No clock and no I/O.** Elapsed days are an input, never read from a clock.
3. **Pure and total.** Every function is deterministic and either returns a value or
   throws on invalid input.
4. **Additive API changes only** within a major version, because consumers record the
   package version in stored schedule transitions and must be able to interpret old
   rows.

## Verification

- **38 upstream reference vectors** in [`testdata/`](testdata/), which act as the
  engine's oracle: if the mathematics ever drifts during a refactor, the fixture is
  the thing that notices.
- **A clean-consumer smoke test** in [`consumer-smoke/`](consumer-smoke/) — a separate
  Gradle build resolving this library the way an unrelated project would. It catches
  an undeclared dependency or a leaked `internal` type, which the engine's own tests
  cannot.

```bash
./gradlew test                      # engine, 14 tests
cd consumer-smoke && ../gradlew test  # external consumer, 7 tests
```

## Upgrade policy

Changing the engine version or the default parameters is not a routine dependency
bump: it silently rewrites future due dates for every existing learner. The reference
fixture must pass, and a consumer should record `FsrsAlgorithmInfo` and the parameter
set alongside each stored schedule so an old row remains interpretable.

## Used by

- [BeeCode](https://github.com/bee-san/BeeCode) — offline spaced-repetition practice
  for algorithm problems.
- [kanji_anki](https://github.com/bee-san/kanji_anki) — the original home of this
  implementation.

## License

MIT. The upstream algorithm is from py-fsrs, also MIT.
