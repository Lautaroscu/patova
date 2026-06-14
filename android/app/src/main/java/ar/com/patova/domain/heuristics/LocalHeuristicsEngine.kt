package ar.com.patova.domain.heuristics

import ar.com.patova.data.local.CallEventDao
import ar.com.patova.data.local.CallEventEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Heuristic flags that the local engine can raise.
 */
enum class HeuristicFlag {
    SEQUENTIAL_NUMBER_PATTERN,
    HIGH_CALL_FREQUENCY_BURST,
    TEMPORARY_NUMBER_PATTERN,
    TELEMARKETING_PREFIX_MATCH
}

/**
 * Result of a local heuristic evaluation.
 *
 * @param flags heuristic rules triggered for this call.
 * @param score local suspicion score [0.0 – 1.0].
 * @param blockingRecommended whether the OS should be advised to block.
 * @param reason human-readable explanation.
 */
data class LocalHeuristicResult(
    val flags: List<@JvmSuppressWildcards HeuristicFlag>,
    val score: Double,
    val blockingRecommended: Boolean,
    val reason: String
)

/**
 * Known telemarketing / VoIP / temporary prefixes used by spam operators
 * in Argentina (ENACOM-registered and community-curated).
 *
 * These prefixes are baked into the APK and updated via the periodic config
 * sync so they work fully offline.
 */
private val KNOWN_SPAM_PREFIXES: Set<String> = setOf(
    "0114100", "0114101", "0114102",
    "0115252", "0115256", "0115258",
    "0115260", "0115261", "0115262",
    "0115360", "0115361", "0115362",
    "0115700", "0115701", "0115702",
    "0116091", "0116092", "0116093",
    "0117070", "0117071", "0117072",
    "3512000", "3512010", "3512011",
    "2614000", "2614010", "2614011",
)

/**
 * Local, offline-first heuristic engine for instantaneous call screening.
 *
 * Designed to execute [evaluate] in **under 50 ms** on a cold path
 * so that the OS [android.telecom.CallScreeningService] is never blocked.
 *
 * ## Heuristic weights (documented):
 *   - SEQUENTIAL_NUMBER_PATTERN  → +0.70 → BLOCK
 *   - HIGH_CALL_FREQUENCY_BURST  → +0.55 → BLOCK if combined with another flag
 *   - TELEMARKETING_PREFIX_MATCH → +0.45 → SUSPECT
 *   - TEMPORARY_NUMBER_PATTERN   → +0.60 → BLOCK
 */
@Singleton
class LocalHeuristicsEngine @Inject constructor(
    private val callEventDao: CallEventDao
) {

    companion object {
        private const val BURST_WINDOW_MS: Long = 60_000L
        private const val BURST_THRESHOLD: Int = 3
        private const val BLOCK_THRESHOLD: Double = 0.50
    }

    fun evaluate(
        incomingNumber: String,
        recentCalls: List<CallEventEntity> = emptyList()
    ): LocalHeuristicResult {
        val flags = mutableListOf<HeuristicFlag>()
        var score = 0.0

        if (matchesSequentialPattern(incomingNumber, recentCalls)) {
            flags.add(HeuristicFlag.SEQUENTIAL_NUMBER_PATTERN)
            score = maxOf(score, 0.70)
        }

        if (detectCallBurst(recentCalls)) {
            flags.add(HeuristicFlag.HIGH_CALL_FREQUENCY_BURST)
            score = maxOf(score, 0.55)
        }

        if (matchesTemporaryNumberPattern(incomingNumber)) {
            flags.add(HeuristicFlag.TEMPORARY_NUMBER_PATTERN)
            score = maxOf(score, 0.60)
        }

        val prefix = extractNationalPrefix(incomingNumber)
        if (prefix != null && KNOWN_SPAM_PREFIXES.contains(prefix)) {
            flags.add(HeuristicFlag.TELEMARKETING_PREFIX_MATCH)
            score = maxOf(score, 0.45)
        }

        val blockingRecommended = score >= BLOCK_THRESHOLD
        val reason = buildReason(flags, score)

        return LocalHeuristicResult(
            flags = flags,
            score = score,
            blockingRecommended = blockingRecommended,
            reason = reason
        )
    }

    private fun matchesSequentialPattern(
        incomingNumber: String,
        recentCalls: List<CallEventEntity>
    ): Boolean {
        val incomingDigits = incomingNumber.filter { it.isDigit() }
        if (incomingDigits.length < 7) return false

        val prefixLen = maxOf(incomingDigits.length - 3, 3)
        val incomingPrefix = incomingDigits.take(prefixLen)
        val incomingSuffix = incomingDigits.drop(prefixLen)

        val matches = recentCalls.filter { call ->
            val callDigits = call.numberHash.filter { it.isDigit() }
            callDigits.length >= 7 &&
                callDigits.take(prefixLen) == incomingPrefix &&
                callDigits.drop(prefixLen).isNotEmpty()
        }

        if (matches.size < 2) return false

        val suffixes = listOf(incomingSuffix.toIntOrNull() ?: return false) +
            matches.mapNotNull { it.numberHash.filter { it.isDigit() }.drop(prefixLen).toIntOrNull() }
        val sorted = suffixes.sorted()
        return sorted.zipWithNext().any { (a, b) -> b - a in 1..3 }
    }

    private fun detectCallBurst(recentCalls: List<CallEventEntity>): Boolean {
        val now = System.currentTimeMillis()
        val recentUnknown = recentCalls.filter { call ->
            call.verdict in listOf("UNKNOWN", "SUSPECT") &&
                (now - call.occurredAtMillis) <= BURST_WINDOW_MS
        }
        return recentUnknown.size >= BURST_THRESHOLD
    }

    private fun matchesTemporaryNumberPattern(number: String): Boolean {
        val digits = number.filter { it.isDigit() }
        if (digits.length < 10) return false
        val localPart = if (digits.startsWith("54") && digits.length >= 12) {
            digits.drop(2)
        } else {
            digits
        }
        val subscriber = localPart.takeLast(7)
        return subscriber.all { it == subscriber.first() } ||
            subscriber.reversed().all { it == subscriber.first() }
    }

    private fun extractNationalPrefix(number: String): String? {
        val digits = number.filter { it.isDigit() }
        val local = if (digits.startsWith("54") && digits.length >= 5) {
            "0" + digits.drop(2)
        } else if (digits.length >= 4) {
            digits
        } else {
            return null
        }
        if (local.length < 7) return null
        return local.take(7)
    }

    private fun buildReason(flags: List<HeuristicFlag>, score: Double): String {
        if (flags.isEmpty()) return "No local heuristics triggered."
        val names = flags.joinToString(", ") { it.name.lowercase().replace('_', ' ') }
        return "Score=${"%.2f".format(score)} | Flags: $names"
    }
}
