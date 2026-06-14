package ar.com.patova.domain.heuristics

import ar.com.patova.data.local.CallEventDao
import ar.com.patova.data.local.CallEventEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

class LocalHeuristicsEngineTest {

    private val fakeCallEventDao = object : CallEventDao {
        override fun getAllFlow() = kotlinx.coroutines.flow.flowOf(emptyList<CallEventEntity>())
        override suspend fun getEventsSince(sinceMillis: Long) = emptyList<CallEventEntity>()
        override suspend fun insert(entity: CallEventEntity) {}
        override suspend fun getById(id: String) = null
        override suspend fun markFeedbackSynced(id: String) {}
        override suspend fun getRecentCallsByHash(phoneHash: String, sinceMillis: Long) = emptyList<CallEventEntity>()
    }

    private lateinit var engine: LocalHeuristicsEngine

    @Before
    fun setup() {
        engine = LocalHeuristicsEngine(fakeCallEventDao)
    }

    @Test
    fun testSafeNumberNoFlags() {
        val result = engine.evaluate("+541112345678", emptyList())
        assertTrue(result.flags.isEmpty())
        assertFalse(result.blockingRecommended)
        assertTrue(result.score < 0.50)
    }

    @Test
    fun testTelemarketingPrefixMatch() {
        val result = engine.evaluate("+54115701123456", emptyList())
        assertTrue(HeuristicFlag.TELEMARKETING_PREFIX_MATCH in result.flags)
        assertTrue(result.score >= 0.45)
    }

    @Test
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
    fun testTemporaryNumberAllSameDigits() {
        val result = engine.evaluate("+54112222222", emptyList())
        assertTrue(HeuristicFlag.TEMPORARY_NUMBER_PATTERN in result.flags)
        assertTrue(result.score >= 0.60)
        assertTrue(result.blockingRecommended)
    }

    @Test
    fun testNormalSubscriberNoTemporaryFlag() {
        val result = engine.evaluate("+541112345678", emptyList())
        assertFalse(HeuristicFlag.TEMPORARY_NUMBER_PATTERN in result.flags)
    }

    @Test
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
        assertTrue("Average latency ${avgMs}ms exceeded 50ms threshold", avgMs < 50.0)
    }

    @Test
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
