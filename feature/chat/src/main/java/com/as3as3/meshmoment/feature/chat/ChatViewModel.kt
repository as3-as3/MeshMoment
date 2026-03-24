package com.as3as3.meshmoment.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.as3as3.meshmoment.core.connectivity.MeshManager
import com.as3as3.meshmoment.data.local.MessageEntity
import com.as3as3.meshmoment.data.local.MessageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: MessageRepository,
    private val meshManager: MeshManager
) : ViewModel() {

    val messages: StateFlow<List<MessageEntity>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sendMessage(content: String) {
        viewModelScope.launch {
            // 1. Save to local DB immediately as "sent"
            val message = MessageEntity(
                senderId = "Me",
                content = content,
                isSent = true
            )
            repository.insert(message)
            
            // 2. Broadcast via Mesh
            meshManager.broadcastMessage(content)
        }
    }
}
