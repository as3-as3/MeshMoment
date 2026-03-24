package com.as3as3.meshmoment.feature.radar

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(viewModel: RadarViewModel) {
    val devices by viewModel.discoveredNodes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mesh Radar") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            VisualRadar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                nodeCount = devices.size
            )

            Text(
                text = "Nearby Nodes (${devices.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(devices) { result ->
                    DeviceItem(
                        name = result.device.name ?: "Anonymous Node",
                        address = result.device.address,
                        rssi = result.rssi,
                        onClick = {
                            viewModel.sendMessage(result, "Ping from Mesh Radar")
                        }
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startScanning()
    }
}

@Composable
fun VisualRadar(modifier: Modifier = Modifier, nodeCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    val radarColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.minDimension / 2 * 0.8f

        // Draw concentric circles
        drawCircle(color = radarColor.copy(alpha = 0.1f), radius = maxRadius, center = center, style = Stroke(2f))
        drawCircle(color = radarColor.copy(alpha = 0.1f), radius = maxRadius * 0.66f, center = center, style = Stroke(2f))
        drawCircle(color = radarColor.copy(alpha = 0.1f), radius = maxRadius * 0.33f, center = center, style = Stroke(2f))

        // Draw sweep line
        val angleRad = Math.toRadians(sweepAngle.toDouble())
        val endX = center.x + maxRadius * cos(angleRad).toFloat()
        val endY = center.y + maxRadius * sin(angleRad).toFloat()
        drawLine(
            color = radarColor,
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 4f
        )

        // Draw "blips" for nodes (randomized positions for visual effect based on count)
        // In a real app, these would map to RSSI and estimated bearing
        repeat(nodeCount) { i ->
            val nodeAngle = (i * 137.5f) % 360f // Golden angle-ish distribution
            val nodeDistance = maxRadius * (0.2f + (i * 0.1f) % 0.8f)
            val nodeRad = Math.toRadians(nodeAngle.toDouble())
            val bx = center.x + nodeDistance * cos(nodeRad).toFloat()
            val by = center.y + nodeDistance * sin(nodeRad).toFloat()
            
            drawCircle(
                color = radarColor.copy(alpha = 0.8f),
                radius = 8f,
                center = Offset(bx, by)
            )
        }
    }
}

@Composable
fun DeviceItem(name: String, address: String, rssi: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(address) },
        trailingContent = { 
            Column(horizontalAlignment = Alignment.End) {
                Text("$rssi dBm")
                LinearProgressIndicator(
                    progress = { (rssi + 100) / 70f }, // Rough normalization
                    modifier = Modifier.width(50.dp).height(4.dp)
                )
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
