package ar.com.numguard.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface NumGuardApi {

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
}
