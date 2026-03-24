package com.as3as3.meshmoment.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.as3as3.meshmoment.core.connectivity.MeshManager
import com.as3as3.meshmoment.data.local.MessageRepository

class ChatViewModelFactory(
    private val repository: MessageRepository,
    private val meshManager: MeshManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, meshManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
