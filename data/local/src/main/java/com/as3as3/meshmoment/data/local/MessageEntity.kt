package com.as3as3.meshmoment.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSent: Boolean
)
