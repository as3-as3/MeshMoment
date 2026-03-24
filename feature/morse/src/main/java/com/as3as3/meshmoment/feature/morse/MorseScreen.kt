package com.as3as3.meshmoment.feature.morse

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorseScreen(viewModel: MorseViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val decodedText by viewModel.decodedText.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val currentIntensity by viewModel.currentIntensity.collectAsStateWithLifecycle()

    var textToSend by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Morse Tool") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val analyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { image ->
                                        viewModel.onCameraIntensityChanged(
                                            calculateIntensity(image)
                                        )
                                        image.close()
                                    }
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analyzer
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                LinearProgressIndicator(
                    progress = { currentIntensity / 255f },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                )
            }

            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Decoded Morse:", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = decodedText.ifEmpty { "Waiting for signal..." },
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Button(onClick = { viewModel.clearBuffer() }, modifier = Modifier.align(Alignment.End)) {
                        Text("Clear")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(modifier = Modifier.padding(16.dp)) {
                TextField(
                    value = textToSend,
                    onValueChange = { textToSend = it },
                    label = { Text("Text to Flash") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSending
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.sendTextAsMorse(textToSend) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSending && textToSend.isNotBlank()
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.FlashlightOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Flash Message")
                    }
                }
            }
        }
    }
}

private fun calculateIntensity(image: androidx.camera.core.ImageProxy): Float {
    val buffer = image.planes[0].buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    var sum = 0L
    for (pixel in data) {
        sum += (pixel.toInt() and 0xFF)
    }
    return if (data.isNotEmpty()) sum.toFloat() / data.size else 0f
}
