package com.as3as3.meshmoment.data.local

import kotlinx.coroutines.flow.Flow

class MessageRepository(private val messageDao: MessageDao) {
    val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()

    suspend fun insert(message: MessageEntity) {
        messageDao.insertMessage(message)
    }

    suspend fun clearAll() {
        messageDao.deleteAllMessages()
    }
}
