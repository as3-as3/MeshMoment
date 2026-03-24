package com.as3as3.meshmoment.core.connectivity

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class MeshMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val content: String? = null,
    val audioData: String? = null, // Base64 encoded audio chunk
    val roomInfo: LocalRoom? = null, // Used for JOIN_RESPONSE or advertising a room
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 3
)

enum class MessageType {
    TEXT, AUDIO, JOIN_REQUEST, JOIN_RESPONSE, ROOM_ADVERTISEMENT
}
