package com.as3as3.meshmoment.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.Timer
import java.util.TimerTask

class IdentityManager {

    private val _currentIdentity = MutableStateFlow(generateNewIdentity())
    val currentIdentity: StateFlow<UUID> = _currentIdentity.asStateFlow()

    private var timer: Timer? = null

    init {
        startRotation()
    }

    private fun startRotation() {
        timer?.cancel()
        timer = Timer()
        // Rotate every 15 minutes (15 * 60 * 1000 ms)
        val rotationInterval = 15L * 60L * 1000L
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                _currentIdentity.value = generateNewIdentity()
            }
        }, rotationInterval, rotationInterval)
    }

    private fun generateNewIdentity(): UUID {
        return UUID.randomUUID()
    }

    fun rotateNow() {
        _currentIdentity.value = generateNewIdentity()
        startRotation() // Reset timer
    }
    
    fun stop() {
        timer?.cancel()
        timer = null
    }
}