package consumer

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
    fun theDefaultParametersAreTheFsrs6Values() {
        // Not FSRS-7's, which start 0.041, 2.4175, 4.1283, 11.9709.
        val values = FsrsParameters.latestDefaultValues()
        assertEquals(21, values.size)
        assertEquals(0.212, values[0], 0.0)
        assertEquals(1.2931, values[1], 0.0)
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
