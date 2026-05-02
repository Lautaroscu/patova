package ar.com.numguard.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface NumGuardApi {

    @POST("api/v1/validate")
    suspend fun validate(@Body request: ValidateRequest): ValidateResponse

    @POST("v1/report")
    suspend fun report(@Body request: ReportRequest): ReportResponse

    @POST("v1/feedback")
    suspend fun feedback(@Body request: FeedbackRequest): FeedbackResponse
}
