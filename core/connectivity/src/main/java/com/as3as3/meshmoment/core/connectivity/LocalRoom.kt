package com.as3as3.meshmoment.core.connectivity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LocalRoom(
    val id: String,
    val name: String,
    val password: String,
    val ownerId: String,
    val transportType: TransportType = TransportType.WIFI_AWARE
)

enum class TransportType {
    BLE, WIFI_AWARE
}
