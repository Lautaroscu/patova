package ar.com.patova.domain

interface DeviceIdProvider {
    fun getDeviceIdHash(): String
}
