package com.bepresent.android.ui.homev2.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DriftingClouds(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // Cloud 1: y=40, scale=0.8, opacity=0.4, duration=45s, delay=0s
        CloudItem(
            yOffset = 40.dp,
            cloudWidth = (150 * 0.8).dp,
            opacity = 0.4f,
            durationMs = 45_000,
            delayMs = 0
        )
        // Cloud 2: y=150, scale=1.2, opacity=0.3, duration=55s, delay=10s, height=50dp
        CloudItem(
            yOffset = 150.dp,
            cloudWidth = (150 * 1.2).dp,
            cloudHeight = 50.dp,
            opacity = 0.3f,
            durationMs = 55_000,
            delayMs = 10_000
        )
        // Cloud 3: y=100, scale=1.6, opacity=0.25, duration=65s, delay=22s, height=40dp
        CloudItem(
            yOffset = 100.dp,
            cloudWidth = (150 * 1.6).dp,
            cloudHeight = 40.dp,
            opacity = 0.25f,
            durationMs = 65_000,
            delayMs = 22_000
        )
        // Cloud 4: y=220, scale=0.6, opacity=0.35, duration=38s, delay=35s
        CloudItem(
            yOffset = 220.dp,
            cloudWidth = (150 * 0.6).dp,
            opacity = 0.35f,
            durationMs = 38_000,
            delayMs = 35_000
        )
    }
}

@Composable
private fun CloudItem(
    yOffset: Dp,
    cloudWidth: Dp,
    cloudHeight: Dp? = null,
    opacity: Float,
    durationMs: Int,
    delayMs: Int
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    val startX = -250f
    val endX = screenWidthDp

    val infiniteTransition = rememberInfiniteTransition(label = "cloud")
    val xOffset by infiniteTransition.animateFloat(
        initialValue = startX,
        targetValue = endX,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMs,
                delayMillis = delayMs,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloudX"
    )

    val iconModifier = if (cloudHeight != null) {
        Modifier.width(cloudWidth).height(cloudHeight)
    } else {
        Modifier.width(cloudWidth)
    }

    Icon(
        imageVector = Icons.Default.Cloud,
        contentDescription = null,
        modifier = Modifier
            .offset(x = xOffset.dp, y = yOffset)
            .then(iconModifier),
        tint = Color.White.copy(alpha = opacity)
    )
}
