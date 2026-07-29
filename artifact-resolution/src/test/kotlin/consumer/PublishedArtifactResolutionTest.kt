package consumer

import dev.bee.fsrs.Fsrs7AlgorithmInfo
import dev.bee.fsrs.Fsrs7Engine
import dev.bee.fsrs.Fsrs7Parameters
import dev.bee.fsrs.Fsrs7ReviewInput
import dev.bee.fsrs.FsrsAlgorithmInfo
import dev.bee.fsrs.FsrsEngine
import dev.bee.fsrs.FsrsParameters
import dev.bee.fsrs.FsrsRating
import dev.bee.fsrs.FsrsReviewInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Resolves `dev.bee:bee-fsrs` purely by Maven coordinate — no composite build, no
 * `dependencySubstitution`. This is the thing the in-repo consumer-smoke build cannot
 * demonstrate: that the *artifact* is fetchable and usable, not merely that the API is
 * self-sufficient.
 */
class PublishedArtifactResolutionTest {

    @Test
    fun theArtifactResolvesAndReportsItsIdentity() {
        assertEquals("FSRS-6.x 21-parameter snapshot", FsrsAlgorithmInfo.ALGORITHM_LABEL)
        assertEquals(21, FsrsParameters.PARAMETER_COUNT)
        assertEquals("v6.3.1", FsrsAlgorithmInfo.UPSTREAM_RELEASE)
    }

    @Test
    fun theFsrs6DefaultParametersAreUnchangedByTheFsrs7Addition() {
        // FSRS-7 was added alongside FSRS-6 rather than replacing it, so the point of
        // this assertion is that adding it did not disturb the older engine's
        // defaults. A consumer with stored FSRS-6 rows must keep reproducing them.
        val values = FsrsParameters.latestDefaultValues()
        assertEquals(21, values.size)
        assertEquals(0.212, values[0], 0.0)
        assertEquals(1.2931, values[1], 0.0)
    }

    @Test
    fun theFsrs7EngineAlsoResolvesFromThePublishedArtifact() {
        // Published in the same artifact, so a resolution failure specific to the
        // FSRS-7 classes — a missing file in the jar, say — is caught here rather
        // than by a consumer.
        assertEquals("FSRS-7 35-parameter snapshot", Fsrs7AlgorithmInfo.ALGORITHM_LABEL)
        assertEquals(35, Fsrs7Parameters.PARAMETER_COUNT)

        val engine = Fsrs7Engine.latestDefault()
        val output = engine.review(
            Fsrs7ReviewInput(
                previousState = engine.initialState(FsrsRating.GOOD),
                rating = FsrsRating.GOOD,
                elapsedDays = 3.0,
                desiredRetention = 0.9,
                maximumIntervalDays = 36_500.0,
            ),
        )
        assertTrue(output.nextIntervalDays > 0.0, "interval was ${output.nextIntervalDays}")
        assertTrue(output.retrievability in 0.0..1.0)
    }

    @Test
    fun aReviewSchedulesAFutureInterval() {
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
        assertTrue(output.nextIntervalDays >= 1, "interval was ${output.nextIntervalDays}")
        assertTrue(output.retrievability in 0.0..1.0)
        assertTrue(requireNotNull(output.nextState).stability > 0.0)
    }
}
