package ar.com.numguard.domain

import ar.com.numguard.data.local.CallEventDao
import ar.com.numguard.data.local.CallEventEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveCallEventUseCase @Inject constructor(
    private val callEventDao: CallEventDao
) {
    suspend fun invoke(
        numberHash: String,
        numberMasked: String,
        verdict: String,
        spamScore: Int?,
        reason: String?,
        actionTaken: String
    ) {
        callEventDao.insert(
            CallEventEntity(
                id = UUID.randomUUID().toString(),
                numberHash = numberHash,
                numberMasked = numberMasked,
                verdict = verdict,
                spamScore = spamScore,
                reason = reason,
                occurredAtMillis = System.currentTimeMillis(),
                actionTaken = actionTaken,
                syncedFeedback = false
            )
        )
    }
}
