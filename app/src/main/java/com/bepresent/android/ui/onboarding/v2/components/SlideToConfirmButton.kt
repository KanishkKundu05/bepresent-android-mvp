package com.bepresent.android.ui.onboarding.v2.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import kotlin.math.roundToInt

private const val COMPLETION_THRESHOLD = 0.85f

@Composable
fun SlideToConfirmButton(
    title: String,
    completedTitle: String,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit
) {
    val density = LocalDensity.current
    val thumbSizeDp = 56.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val horizontalPaddingPx = with(density) { 4.dp.toPx() }

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var isCompleted by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = dragProgress,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "slide_progress"
    )

    val maxDragPx = (trackWidthPx - thumbSizePx - horizontalPaddingPx * 2).coerceAtLeast(0f)
    val thumbOffsetPx = (animatedProgress * maxDragPx).roundToInt()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .clip(CircleShape)
            .background(OnboardingTokens.BrandPrimary.copy(alpha = 0.3f)),
        contentAlignment = Alignment.CenterStart
    ) {
        // Progress fill
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                .height(64.dp)
                .clip(CircleShape)
                .background(OnboardingTokens.BrandPrimary)
        )

        // Text label
        Text(
            text = if (isCompleted) completedTitle else title,
            style = OnboardingTypography.p1,
            color = OnboardingTokens.NeutralWhite.copy(alpha = (1f - animatedProgress).coerceIn(0f, 1f)),
            modifier = Modifier.align(Alignment.Center)
        )

        // Draggable thumb
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .offset { IntOffset(thumbOffsetPx, 0) }
                .size(thumbSizeDp)
                .clip(CircleShape)
                .background(OnboardingTokens.NeutralWhite)
                .pointerInput(isCompleted) {
                    if (isCompleted) return@pointerInput
                    val thumbPx = thumbSizeDp.toPx()
                    val padPx = 4.dp.toPx()
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragProgress >= COMPLETION_THRESHOLD) {
                                dragProgress = 1f
                                isCompleted = true
                                onComplete()
                            } else {
                                dragProgress = 0f
                            }
                        },
                        onDragCancel = {
                            dragProgress = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val maxDrag = (trackWidthPx - thumbPx - padPx * 2)
                                .coerceAtLeast(0f)
                            if (maxDrag > 0) {
                                val delta = dragAmount / maxDrag
                                dragProgress = (dragProgress + delta).coerceIn(0f, 1f)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = OnboardingTokens.BrandPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
