package com.bepresent.android.ui.homev2.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepresent.android.R
import com.bepresent.android.ui.homev2.HomeV2Tokens

enum class ActiveSessionSubState {
    Active,
    BreakRunning,
    Completed
}

data class ActiveSessionUiState(
    val subState: ActiveSessionSubState = ActiveSessionSubState.Active,
    val sessionName: String = "",
    val modeLabel: String = "Block List",
    val timeRemainingString: String = "00:00",
    val progress: Float = 0f,
    val points: Int = 0,
    val beastMode: Boolean = false,
    val breakTimeRemainingString: String = "00:00",
    val breakProgress: Float = 0f
)

@Composable
fun ActiveSessionCard(
    state: ActiveSessionUiState,
    onTakeBreak: () -> Unit,
    onEndBreak: () -> Unit,
    onGiveUp: () -> Unit,
    onBeastModeInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Cloud background
        Image(
            painter = painterResource(id = R.drawable.cloud_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Drifting clouds (not shown during completion)
        if (state.subState != ActiveSessionSubState.Completed) {
            DriftingClouds()
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Session icon — brick with optional burst
            Box(contentAlignment = Alignment.Center) {
                // Burst behind icon on completion
                if (state.subState == ActiveSessionSubState.Completed) {
                    val infiniteTransition = rememberInfiniteTransition(label = "burst")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(30_000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "burstRotation"
                    )
                    Image(
                        painter = painterResource(id = R.drawable.session_burst),
                        contentDescription = null,
                        modifier = Modifier
                            .size(200.dp)
                            .graphicsLayer {
                                rotationZ = rotation
                                alpha = 0.6f
                            }
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.session_brick),
                    contentDescription = "be present",
                    modifier = Modifier.size(126.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = when (state.subState) {
                    ActiveSessionSubState.Completed -> "Session Complete!"
                    ActiveSessionSubState.BreakRunning -> "On a Break"
                    ActiveSessionSubState.Active -> state.sessionName
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HomeV2Tokens.NeutralBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Allow List / Block List capsule button
            if (state.subState == ActiveSessionSubState.Active) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                        .border(1.dp, Color.White, CircleShape)
                        .clickable { /* TODO: open app selection sheet */ }
                        .height(45.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.modeLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = HomeV2Tokens.NeutralBlack
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = HomeV2Tokens.NeutralBlack
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Break running state: "Apps Temporarily Unlocked" chip
            if (state.subState == ActiveSessionSubState.BreakRunning) {
                Text(
                    text = "Apps Temporarily Unlocked",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = HomeV2Tokens.GreenPrimary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(HomeV2Tokens.GreenFill.copy(alpha = 0.7f))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Main timer
            Text(
                text = state.timeRemainingString,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = HomeV2Tokens.NeutralBlack,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress capsule with XP
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(HomeV2Tokens.YellowFill)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomProgressBar(
                    progress = state.progress,
                    backgroundColor = HomeV2Tokens.YellowPrimary.copy(alpha = 0.2f),
                    filledColor = HomeV2Tokens.YellowPrimary,
                    height = 8.dp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = HomeV2Tokens.YellowPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+ ${state.points} XP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeV2Tokens.YellowPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Break controls
            when (state.subState) {
                ActiveSessionSubState.Active -> {
                    // "Take A Break" button
                    if (!state.beastMode) {
                        FullButton(
                            title = "Take A Break",
                            icon = Icons.Default.Pause,
                            appearance = FullButtonAppearance.Gray,
                            onClick = onTakeBreak
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                ActiveSessionSubState.BreakRunning -> {
                    // Break timer + progress
                    Text(
                        text = state.breakTimeRemainingString,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeV2Tokens.BrandPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomProgressBar(
                        progress = state.breakProgress,
                        backgroundColor = HomeV2Tokens.BrandPrimary.copy(alpha = 0.2f),
                        filledColor = HomeV2Tokens.BrandPrimary,
                        height = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FullButton(
                        title = "End Break Now",
                        appearance = FullButtonAppearance.Gray,
                        onClick = onEndBreak
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                ActiveSessionSubState.Completed -> {
                    // No break controls needed
                }
            }

            // Give Up / Beast Mode row
            if (state.subState != ActiveSessionSubState.Completed) {
                if (state.beastMode) {
                    FullButton(
                        title = "Beast Mode - No Giving Up",
                        appearance = FullButtonAppearance.DangerShadow,
                        onClick = onBeastModeInfo
                    )
                } else {
                    FullButton(
                        title = "Give Up",
                        icon = Icons.Default.Stop,
                        appearance = FullButtonAppearance.DangerShadow,
                        onClick = onGiveUp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
