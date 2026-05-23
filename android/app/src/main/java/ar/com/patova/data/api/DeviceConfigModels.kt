package ar.com.patova.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceConfigRequest(
    @SerialName("block_non_contacts") val blockNonContacts: Boolean,
    @SerialName("allowed_prefixes") val allowedPrefixes: List<String>,
    @SerialName("blocked_prefixes") val blockedPrefixes: List<String>
)

@Serializable
data class DeviceConfigResponse(
    @SerialName("device_id") val deviceId: String,
    @SerialName("block_non_contacts") val blockNonContacts: Boolean,
    @SerialName("allowed_prefixes") val allowedPrefixes: List<String>,
    @SerialName("blocked_prefixes") val blockedPrefixes: List<String>
)
