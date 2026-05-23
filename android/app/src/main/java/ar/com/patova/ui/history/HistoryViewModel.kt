package ar.com.patova.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ar.com.patova.data.local.CallEventDao
import ar.com.patova.data.local.CallEventEntity
import ar.com.patova.domain.SubmitFeedbackUseCase
import ar.com.patova.domain.SubmitReportUseCase
import ar.com.patova.sync.PendingReportsWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    application: Application,
    private val callEventDao: CallEventDao,
    private val submitReportUseCase: SubmitReportUseCase,
    private val submitFeedbackUseCase: SubmitFeedbackUseCase
) : AndroidViewModel(application) {

    val events: StateFlow<List<CallEventEntity>> = callEventDao.getAllFlow()
        .let { flow ->
            val result = MutableStateFlow<List<CallEventEntity>>(emptyList())
            viewModelScope.launch {
                flow.collect { result.value = it }
            }
            result.asStateFlow()
        }

    fun submitReport(
        event: CallEventEntity,
        reportType: String,
        description: String?
    ) {
        viewModelScope.launch {
            val sent = submitReportUseCase.invoke(
                numberHash = event.numberHash,
                numberMasked = event.numberMasked,
                reportType = reportType,
                description = description,
                callEventId = event.id
            )
            if (!sent) enqueueSyncWork()
        }
    }

    fun submitFeedback(
        event: CallEventEntity,
        feedbackType: String
    ) {
        viewModelScope.launch {
            val sent = submitFeedbackUseCase.invoke(
                numberHash = event.numberHash,
                numberMasked = event.numberMasked,
                originalVerdict = event.verdict,
                feedbackType = feedbackType,
                callEventId = event.id
            )
            if (!sent) enqueueSyncWork()
        }
    }

    private fun enqueueSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<PendingReportsWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10L,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(getApplication())
            .enqueueUniqueWork(
                "pending_reports_sync",
                ExistingWorkPolicy.REPLACE,
                request
            )
    }
}
