package ar.com.patova.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PatovaApi {

    @POST("v1/validate")
    suspend fun validate(@Body request: ValidateRequest): ValidateResponse

    @POST("v1/report")
    suspend fun report(@Body request: ReportRequest): ReportResponse

    @POST("v1/feedback")
    suspend fun feedback(@Body request: FeedbackRequest): FeedbackResponse

    @GET("v1/config/{device_id}")
    suspend fun getConfig(@Path("device_id") deviceId: String): DeviceConfigResponse

    @PUT("v1/config/{device_id}")
    suspend fun updateConfig(
        @Path("device_id") deviceId: String,
        @Body request: DeviceConfigRequest
    ): DeviceConfigResponse

    @POST("v1/payments/create-preference")
    suspend fun createPreference(@Body request: CreatePreferenceRequest): CreatePreferenceResponse

    @GET("v1/subscriptions/me")
    suspend fun getSubscriptionMe(@Query("user_id") userId: String): SubscriptionMeResponse

    @POST("v1/behavior/sync")
    suspend fun sync(@Body request: SyncRequest): SyncResponse

    @GET("v1/stats")
    suspend fun getStats(): StatsResponse
}
