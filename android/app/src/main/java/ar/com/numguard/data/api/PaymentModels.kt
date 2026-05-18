package ar.com.numguard.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePreferenceRequest(
    @SerialName("plan_id")
    val planId: String,
    val email: String,
    @SerialName("user_id")
    val userId: String
)

@Serializable
data class CreatePreferenceResponse(
    @SerialName("user_id")
    val userId: String,
    @SerialName("preference_id")
    val preferenceId: String,
    @SerialName("init_point")
    val initPoint: String
)

@Serializable
data class SubscriptionDetail(
    val id: String,
    val status: String,
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    val provider: String = "MERCADO_PAGO",
    @SerialName("renewal_enabled")
    val renewalEnabled: Boolean = false
)

@Serializable
data class SubscriptionMeResponse(
    @SerialName("user_id")
    val userId: String,
    @SerialName("is_premium")
    val isPremium: Boolean,
    val subscription: SubscriptionDetail? = null
)
