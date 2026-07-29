package consumer

import dev.bee.fsrs.FsrsAlgorithmInfo
import dev.bee.fsrs.FsrsEngine
import dev.bee.fsrs.FsrsMemoryState
import dev.bee.fsrs.FsrsParameters
import dev.bee.fsrs.FsrsRating
import dev.bee.fsrs.FsrsReviewInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The clean-consumer smoke test.
 *
 * An external project using bee-fsrs through its published coordinate, with no access
 * to the engine's own test sources or internals. It answers a question the engine's
 * own tests cannot: *is this package actually reusable?*
 *
 * Three things would fail here and nowhere else:
 *
 * - an undeclared dependency, since this build declares only `dev.bee:bee-fsrs`;
 * - a type that is `internal` but appears in a public signature, which the engine's
 *   own tests can see and a consumer cannot;
 * - a change to the default parameters or the algorithm identity, which would silently
 *   reschedule every existing learner's queue.
 */
class CleanConsumerSmokeTest {

    @Test
    fun theEngineIsConstructibleFromDefaults() {
        // The one-liner path a new consumer takes. If this needs anything else, the
        // package is not self-sufficient.
        val engine = FsrsEngine.latestDefault()
        val state = engine.initialState(FsrsRating.GOOD)
        assertTrue(state.stability > 0.0)
        assertTrue(state.difficulty in 1.0..10.0)
    }

    @Test
    fun theAlgorithmIdentityIsPinnedAndVisible() {
        // A consumer must be able to record *which* mathematics produced a schedule,
        // because changing it rewrites every future due date.
        assertEquals("FSRS-6.x 21-parameter snapshot", FsrsAlgorithmInfo.ALGORITHM_LABEL)
        assertEquals("open-spaced-repetition/py-fsrs", FsrsAlgorithmInfo.UPSTREAM_REPOSITORY)
        assertEquals("v6.3.1", FsrsAlgorithmInfo.UPSTREAM_RELEASE)
        assertEquals(21, FsrsAlgorithmInfo.PARAMETER_COUNT)
    }

    @Test
    fun theDefaultParameterSetIsExactlyTheExpectedTwentyOneValues() {
        // Pinned by value, not just by count. A silent parameter change is the most
        // consequential possible regression: it reschedules everyone.
        val expected = doubleArrayOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133,
            0.8334, 3.0194, 0.001, 1.8722, 0.1666,
            0.796, 1.4835, 0.0614, 0.2629, 1.6483,
            0.6014, 1.8729, 0.5425, 0.0912, 0.0658,
            0.1542,
        )
        val actual = FsrsParameters.latestDefaultValues()
        assertEquals(21, actual.size)
        expected.forEachIndexed { index, value ->
            assertEquals(value, actual[index], 0.0, "parameter $index")
        }
    }

    @Test
    fun betterRatingsProduceLongerIntervals() {
        // The property a scheduling consumer actually depends on. If this ordering
        // breaks, every rating button in every consuming app is lying.
        val engine = FsrsEngine.latestDefault()
        val intervals = listOf(
            FsrsRating.AGAIN, FsrsRating.HARD, FsrsRating.GOOD, FsrsRating.EASY,
        ).map { rating ->
            engine.nextIntervalDays(engine.initialState(rating).stability, 0.9, 36_500)
        }
        assertEquals(intervals, intervals.sorted(), "intervals must not decrease: $intervals")
        assertTrue(intervals.last() > intervals.first())
    }

    @Test
    fun aFullReviewRoundTripsThroughThePublicApi() {
        val engine = FsrsEngine.latestDefault()
        val previous = engine.initialState(FsrsRating.GOOD)

        val output = engine.review(
            FsrsReviewInput(
                previousState = previous,
                rating = FsrsRating.GOOD,
                elapsedDays = 3,
                desiredRetention = 0.9,
                maximumInterval = 36_500,
            ),
        )

        assertTrue(output.retrievability in 0.0..1.0)
        assertTrue(output.nextIntervalDays >= 1)
        val next = requireNotNull(output.nextState)
        assertTrue(next.stability > 0.0)
        assertTrue(next.difficulty in 1.0..10.0)
    }

    @Test
    fun theEngineIsDeterministic() {
        // No clock and no randomness inside, which is what makes a recorded schedule
        // audit meaningful.
        val a = FsrsEngine.latestDefault()
        val b = FsrsEngine.create(FsrsParameters.latestDefault())
        val state = FsrsMemoryState(5.0, 5.0)
        assertEquals(
            a.nextState(state, FsrsRating.GOOD, 7),
            b.nextState(state, FsrsRating.GOOD, 7),
        )
    }

    @Test
    fun invalidInputIsRejectedRatherThanSilentlyCoerced() {
        val engine = FsrsEngine.latestDefault()
        // A negative elapsed count, an out-of-range retention, and a wrong-sized
        // parameter set are all caller errors and must be loud.
        runCatching { engine.retrievability(FsrsMemoryState(5.0, 5.0), -1) }
            .onSuccess { error("negative elapsedDays must be rejected") }
        runCatching { engine.nextIntervalDays(5.0, 1.5, 36_500) }
            .onSuccess { error("a retention above 1 must be rejected") }
        runCatching { FsrsParameters.of(DoubleArray(3)) }
            .onSuccess { error("a 3-value parameter set must be rejected") }
    }
}
