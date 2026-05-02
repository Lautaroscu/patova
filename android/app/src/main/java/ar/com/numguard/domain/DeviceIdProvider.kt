package ar.com.numguard.domain

interface DeviceIdProvider {
    fun getDeviceIdHash(): String
}
