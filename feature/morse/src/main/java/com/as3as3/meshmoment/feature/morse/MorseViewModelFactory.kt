package com.as3as3.meshmoment.feature.morse

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MorseViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MorseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MorseViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
