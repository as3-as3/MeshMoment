package com.as3as3.meshmoment.feature.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.as3as3.meshmoment.core.connectivity.MeshManager

class RadarViewModelFactory(private val meshManager: MeshManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RadarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RadarViewModel(meshManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
