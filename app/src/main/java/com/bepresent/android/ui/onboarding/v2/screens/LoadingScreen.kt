package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import kotlinx.coroutines.delay

private const val LOADING_DURATION_MS = 3000L
private const val LOADING_HAPTIC_STEPS = 10
private val SUBHEADLINES = listOf(
    "Analyzing your screen time habits...",
    "Building your personalized plan...",
    "Preparing your journey..."
)

@Composable
fun LoadingScreen(onComplete: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var targetProgress by remember { mutableFloatStateOf(0f) }
    var currentSubheadlineIndex by remember { mutableIntStateOf(0) }
    var isTransitioning by remember { mutableStateOf(false) }
    var lastHapticStep by remember { mutableIntStateOf(0) }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = LOADING_DURATION_MS.toInt()),
        label = "loading_progress"
    )

    // Start loading animation
    LaunchedEffect(Unit) {
        targetProgress = 1f
        delay(LOADING_DURATION_MS)
        onComplete()
    }

    // Cycle subheadlines
    LaunchedEffect(Unit) {
        val interval = LOADING_DURATION_MS / SUBHEADLINES.size
        for (i in 1 until SUBHEADLINES.size) {
            delay(interval)
            isTransitioning = true
            delay(150)
            currentSubheadlineIndex = i
            isTransitioning = false
        }
    }

    LaunchedEffect(animatedProgress) {
        val currentStep = (animatedProgress * LOADING_HAPTIC_STEPS).toInt()
        if (currentStep > lastHapticStep) {
            lastHapticStep = currentStep
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Circular progress
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(190.dp)
        ) {
            val trackColor = OnboardingTokens.Brand200
            val progressColor = OnboardingTokens.BrandPrimary

            Canvas(modifier = Modifier.size(170.dp)) {
                val strokeWidth = 14.9.dp.toPx()
                val arcDiameter = size.minDimension - strokeWidth
                val topLeft = Offset(
                    (size.width - arcDiameter) / 2f,
                    (size.height - arcDiameter) / 2f
                )
                val arcSize = Size(arcDiameter, arcDiameter)

                // Track
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Percentage text
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = OnboardingTypography.title2,
                color = OnboardingTokens.BrandPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Calculating your results",
            style = OnboardingTypography.h2,
            color = OnboardingTokens.NeutralBlack,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = SUBHEADLINES[currentSubheadlineIndex],
            style = OnboardingTypography.p3,
            color = OnboardingTokens.NeutralBlack.copy(
                alpha = if (isTransitioning) 0f else 1f
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.height(60.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}
