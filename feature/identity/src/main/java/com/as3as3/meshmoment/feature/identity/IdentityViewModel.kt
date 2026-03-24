package com.as3as3.meshmoment.feature.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.as3as3.meshmoment.core.security.IdentityManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

class IdentityViewModel(private val identityManager: IdentityManager) : ViewModel() {

    val currentIdentity: StateFlow<String> = identityManager.currentIdentity
        .map { it.toString() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Loading..."
        )

    fun rotateIdentity() {
        identityManager.rotateNow()
    }
}
