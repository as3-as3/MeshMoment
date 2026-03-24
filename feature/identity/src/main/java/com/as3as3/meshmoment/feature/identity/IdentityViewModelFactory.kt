package com.as3as3.meshmoment.feature.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.as3as3.meshmoment.core.security.IdentityManager

class IdentityViewModelFactory(private val identityManager: IdentityManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IdentityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IdentityViewModel(identityManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
