package com.as3as3.meshmoment.feature.morse

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class MorseCameraAnalyzer(
    private val onIntensityChanged: (Float) -> Unit
) : ImageAnalysis.Analyzer {

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        
        // Calculate average brightness (Y channel in YUV_420_888)
        var sum = 0L
        for (pixel in data) {
            sum += (pixel.toInt() and 0xFF)
        }
        val avg = if (data.isNotEmpty()) sum.toFloat() / data.size else 0f
        
        onIntensityChanged(avg)
        image.close()
    }
}
