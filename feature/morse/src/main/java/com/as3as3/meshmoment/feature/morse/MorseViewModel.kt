package com.as3as3.meshmoment.feature.morse

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MorseViewModel(context: Context) : ViewModel() {
    private val flashSender = MorseFlashSender(context)
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _decodedText = MutableStateFlow("")
    val decodedText: StateFlow<String> = _decodedText.asStateFlow()

    private val _currentIntensity = MutableStateFlow(0f)
    val currentIntensity: StateFlow<Float> = _currentIntensity.asStateFlow()

    private var lastPeakTime = 0L
    private val threshold = 50f // Threshold for flash detection
    private var currentMorseBuffer = StringBuilder()

    fun sendTextAsMorse(text: String) {
        viewModelScope.launch {
            _isSending.value = true
            val morse = MorseConverter.textToMorse(text)
            flashSender.sendMorse(morse)
            _isSending.value = false
        }
    }

    fun onCameraIntensityChanged(intensity: Float) {
        _currentIntensity.value = intensity
        val now = System.currentTimeMillis()
        
        if (intensity > threshold) {
            // High intensity (flash on)
            if (lastPeakTime == 0L) lastPeakTime = now
        } else {
            // Low intensity (flash off)
            if (lastPeakTime != 0L) {
                val duration = now - lastPeakTime
                if (duration in 50..400) {
                    currentMorseBuffer.append(".")
                } else if (duration > 400) {
                    currentMorseBuffer.append("-")
                }
                lastPeakTime = 0L
                updateDecodedText()
            }
        }
    }

    private fun updateDecodedText() {
        // Simplified: Clear after pause or specific logic
        // For now, just show the raw buffer mapping
        val text = MorseConverter.morseToText(currentMorseBuffer.toString())
        _decodedText.value = text
    }
    
    fun clearBuffer() {
        currentMorseBuffer.clear()
        _decodedText.value = ""
    }
}
