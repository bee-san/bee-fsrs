# Provenance

`bee-fsrs` is a Kotlin implementation of FSRS-6.x, extracted from
[`bee-san/kanji_anki`](https://github.com/bee-san/kanji_anki) so that BeeCode,
kanji_anki, and any other consumer resolve the same tested artifact rather than
maintaining divergent copies.

## Chain of custody

| Layer | Identity |
|---|---|
| Algorithm | FSRS-6.x, 21-parameter snapshot |
| Upstream reference | [`open-spaced-repetition/py-fsrs`](https://github.com/open-spaced-repetition/py-fsrs) `v6.3.1` |
| Upstream commit | `3abe686e9c058d3f3c00bbeb92e68b71211b2b31` |
| Upstream scheduler blob | `6d42ecb259bbaaa02101f13c5e1b2ec7cdc77eae` |
| Kotlin implementation | `bee-san/kanji_anki`, module `fsrs-java` |
| Source commit | `93f8c3fe756944312d96c559b8d29701af43f5d0` |
| Source tree object | `c3a95c555bfc717de0606f1345cec3c3774d60e4` |
| Extracted | 2026-07-29 |
| Author / rights holder | Autumn Skerritt (`bee-san`) |
| License | MIT, as is the upstream algorithm |

The upstream identity is also asserted in code by `FsrsAlgorithmInfo` and verified by
`FsrsEngineReferenceTest`, so a silent algorithm swap fails the build rather than
quietly changing every learner's schedule.

## On the "FSRS 7" label

`kanji_anki`'s README describes its scheduler as "FSRS 7". That label is not accurate
for this code, and the discrepancy is worth recording rather than papering over:

- upstream py-fsrs has published **no v7**; its latest release is `v6.3.1`, which is
  what this port targets;
- `kanji_anki`'s own planning notes say so explicitly — *"Algorithm label is
  FSRS-6-family 21-parameter snapshot, not FSRS-7 unless upstream explicitly labels it
  that way"*;
- the parameter count, 21, matches the FSRS-6 family.

So this package labels itself FSRS-6.x. If upstream publishes a v7 with a different
parameter set, adopting it is a deliberate, gated change — not a silent relabel.

## What was and was not changed during extraction

Changed:

- the build file, since the `kani.*` convention plugins do not exist outside
  kanji_anki;
- one test path fallback, because the extracted repository has no module directory;
- added: a Maven publication, a clean-consumer smoke build, a license, and this file.

Not changed: every `src/main` source file is byte-identical to the source commit. The
38-case reference fixture is byte-identical. No mathematics, no parameter values, no
validation, and no public API were touched.

That matters because the fixture is the engine's oracle. If the mathematics had
drifted during a copy-paste, the fixture would be the only thing that noticed — and it
still passes.

## Extraction rules

These keep the package genuinely reusable rather than merely relocated:

1. **No consumer types.** The engine knows nothing about cards, problems, reviews, or
   sessions. It takes a rating and returns memory state.
2. **No dependencies beyond the Kotlin stdlib.** No coroutines, serialization,
   datetime, or logging. A consumer must never inherit a transitive dependency from
   the scheduler.
3. **No clock and no I/O.** Elapsed days are an input. This is what makes the engine
   testable and its results reproducible.
4. **Pure and total.** Every function is deterministic and either returns a value or
   throws on invalid input. No nullable-success returns.
5. **Additive API changes only** within a major version, because consumers record the
   package version in stored schedule transitions and must be able to interpret old
   rows.

Rules 2 and 5 are enforced by [`consumer-smoke/`](consumer-smoke/), a separate Gradle
build that resolves this library the way an unrelated project would. It fails on an
undeclared dependency or an `internal` type leaking into a public signature — neither
of which the engine's own tests can detect, because they can see everything.

## Why consumers should record so much per transition

A consumer is advised to store the algorithm label, package version, parameter set and
hash, previous-state hash, elapsed days, and rating alongside each resulting state.

That redundancy means operational state can be rebuilt by folding recorded *outputs*,
with no historical engine binary present. Recomputing history from inputs is then only
an integrity check, available while the exact historical implementation still is.

## Upgrade gate

Changing the engine version or the default parameters silently rewrites future due
dates for existing learners. The reference fixture must pass, and for a
multi-platform consumer it must pass on every target, before an upgrade may change any
schedule.
