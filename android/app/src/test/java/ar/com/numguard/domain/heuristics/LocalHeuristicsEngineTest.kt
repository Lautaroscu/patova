package ar.com.numguard.domain.heuristics

import ar.com.numguard.data.local.CachedValidationDao
import ar.com.numguard.data.local.CallEventDao
import ar.com.numguard.data.local.CallEventEntity
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.system.measureTimeMillis

@ExtendWith(MockKExtension::class)
class LocalHeuristicsEngineTest {

    @MockK
    lateinit var callEventDao: CallEventDao

    @MockK
    lateinit var cachedValidationDao: CachedValidationDao

    private lateinit var engine: LocalHeuristicsEngine

    @BeforeEach
    fun setup() {
        engine = LocalHeuristicsEngine(callEventDao)
    }

    @Test
    @DisplayName("Safe number: no flags, no block")
    fun testSafeNumberNoFlags() {
        val result = engine.evaluate("+541112345678", emptyList())
        assertTrue(result.flags.isEmpty())
        assertFalse(result.blockingRecommended)
        assertTrue(result.score < 0.50)
    }

    @Test
    @DisplayName("Prefix match triggers TELEMARKETING_PREFIX_MATCH flag")
    fun testTelemarketingPrefixMatch() {
        val result = engine.evaluate("+54115701123456", emptyList())
        assertTrue(HeuristicFlag.TELEMARKETING_PREFIX_MATCH in result.flags)
        assertTrue(result.score >= 0.45)
    }

    @Test
    @DisplayName("Burst of unknown calls triggers HIGH_CALL_FREQUENCY_BURST")
    fun testCallBurstDetected() {
        val now = System.currentTimeMillis()
        val recentCalls = (1..5).map { i ->
            CallEventEntity(
                id = "ev-$i",
                numberHash = "+54111234567$i",
                numberMasked = "+5411****$i",
                verdict = "UNKNOWN",
                spamScore = null,
                reason = null,
                occurredAtMillis = now - 10_000L,
                actionTaken = "ALLOWED",
                syncedFeedback = false
            )
        }
        val result = engine.evaluate("+5411123457000", recentCalls)
        assertTrue(HeuristicFlag.HIGH_CALL_FREQUENCY_BURST in result.flags)
        assertTrue(result.blockingRecommended)
    }

    @Test
    @DisplayName("No burst when calls are old")
    fun testNoBurstWithOldCalls() {
        val now = System.currentTimeMillis()
        val recentCalls = (1..5).map { i ->
            CallEventEntity(
                id = "ev-$i",
                numberHash = "+54111234567$i",
                numberMasked = "+5411****$i",
                verdict = "UNKNOWN",
                spamScore = null,
                reason = null,
                occurredAtMillis = now - 120_000L,
                actionTaken = "ALLOWED",
                syncedFeedback = false
            )
        }
        val result = engine.evaluate("+5411123457000", recentCalls)
        assertFalse(HeuristicFlag.HIGH_CALL_FREQUENCY_BURST in result.flags)
    }

    @Test
    @DisplayName("Burst not triggered when calls have known verdicts")
    fun testBurstIgnoresKnownCalls() {
        val now = System.currentTimeMillis()
        val recentCalls = (1..5).map { i ->
            CallEventEntity(
                id = "ev-$i",
                numberHash = "+54111234567$i",
                numberMasked = "+5411****$i",
                verdict = "ALLOW",
                spamScore = null,
                reason = null,
                occurredAtMillis = now - 5_000L,
                actionTaken = "ALLOWED",
                syncedFeedback = false
            )
        }
        val result = engine.evaluate("+5411123457000", recentCalls)
        assertFalse(HeuristicFlag.HIGH_CALL_FREQUENCY_BURST in result.flags)
    }

    @Test
    @DisplayName("Sequential number pattern detected")
    fun testSequentialPatternDetected() {
        val recentCalls = listOf(
            callEvent("+541112345671", verdict = "UNKNOWN"),
            callEvent("+541112345672", verdict = "UNKNOWN"),
            callEvent("+541112345673", verdict = "UNKNOWN"),
        )
        val result = engine.evaluate("+541112345670", recentCalls)
        assertTrue(HeuristicFlag.SEQUENTIAL_NUMBER_PATTERN in result.flags)
        assertTrue(result.blockingRecommended)
        assertTrue(result.score >= 0.70)
    }

    @Test
    @DisplayName("Non-sequential numbers do NOT trigger sequential flag")
    fun testNonSequentialDoesNotTrigger() {
        val recentCalls = listOf(
            callEvent("+541112345671", verdict = "UNKNOWN"),
            callEvent("+541112399999", verdict = "UNKNOWN"),
            callEvent("+541112340000", verdict = "UNKNOWN"),
        )
        val result = engine.evaluate("+541112345670", recentCalls)
        assertFalse(HeuristicFlag.SEQUENTIAL_NUMBER_PATTERN in result.flags)
    }

    @Test
    @DisplayName("Temporary number pattern: all same digits in subscriber")
    fun testTemporaryNumberAllSameDigits() {
        val result = engine.evaluate("+54112222222", emptyList())
        assertTrue(HeuristicFlag.TEMPORARY_NUMBER_PATTERN in result.flags)
        assertTrue(result.score >= 0.60)
        assertTrue(result.blockingRecommended)
    }

    @Test
    @DisplayName("Normal subscriber digits do NOT trigger temporary flag")
    fun testNormalSubscriberNoTemporaryFlag() {
        val result = engine.evaluate("+541112345678", emptyList())
        assertFalse(HeuristicFlag.TEMPORARY_NUMBER_PATTERN in result.flags)
    }

    @Test
    @DisplayName("Combined flags produce correct score")
    fun testCombinedFlagsScore() {
        val recentCalls = listOf(
            callEvent("+5411525212345", verdict = "UNKNOWN"),
            callEvent("+5411525212346", verdict = "UNKNOWN"),
        )
        val result = engine.evaluate("+5411525212345", recentCalls)
        assertTrue(result.score >= 0.70)
        assertTrue(result.blockingRecommended)
    }

    @Test
    @DisplayName("Offline latency under 50ms")
    fun testLatencyUnder50ms() {
        val elapsed = measureTimeMillis {
            repeat(100) {
                engine.evaluate(
                    "+54115701123456",
                    listOf(
                        callEvent("+54115701123457", verdict = "UNKNOWN"),
                        callEvent("+54115701123458", verdict = "UNKNOWN"),
                        callEvent("+54115701123459", verdict = "UNKNOWN"),
                    )
                )
            }
        }
        val avgMs = elapsed / 100.0
        assertTrue(avgMs < 50.0, "Average latency ${avgMs}ms exceeded 50ms threshold")
    }

    @Test
    @DisplayName("Empty call history still evaluates correctly")
    fun testEmptyHistoryProducesValidResult() {
        val result = engine.evaluate("+5411525212345", emptyList())
        assertTrue(result.score >= 0.0)
        assertFalse(result.blockingRecommended)
    }

    private fun callEvent(
        numberHash: String,
        verdict: String = "UNKNOWN",
        occurredAtMillis: Long = System.currentTimeMillis() - 5_000L
    ) = CallEventEntity(
        id = "ev-${numberHash.takeLast(4)}",
        numberHash = numberHash,
        numberMasked = "+5411****" + numberHash.takeLast(2),
        verdict = verdict,
        spamScore = null,
        reason = null,
        occurredAtMillis = occurredAtMillis,
        actionTaken = if (verdict == "BLOCK") "BLOCKED" else "ALLOWED",
        syncedFeedback = false
    )
}
