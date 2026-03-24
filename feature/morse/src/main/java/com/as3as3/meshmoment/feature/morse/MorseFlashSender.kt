package com.as3as3.meshmoment.feature.morse

import android.content.Context
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.delay

class MorseFlashSender(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null

    init {
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendMorse(morse: String, unitTimeMs: Long = 200L) {
        val id = cameraId ?: return
        
        for (char in morse) {
            when (char) {
                '.' -> {
                    setFlash(id, true)
                    delay(unitTimeMs)
                    setFlash(id, false)
                }
                '-' -> {
                    setFlash(id, true)
                    delay(unitTimeMs * 3)
                    setFlash(id, false)
                }
                ' ' -> {
                    delay(unitTimeMs * 3) // Gap between letters
                }
                '/' -> {
                    delay(unitTimeMs * 7) // Gap between words
                }
            }
            delay(unitTimeMs) // Gap between symbols
        }
    }

    private fun setFlash(cameraId: String, enabled: Boolean) {
        try {
            cameraManager.setTorchMode(cameraId, enabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
