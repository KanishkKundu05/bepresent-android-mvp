package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bepresent.android.R
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.SlideToConfirmButton
import kotlinx.coroutines.delay

private val CHALLENGE_BULLETS = listOf(
    "Build a consistent screen time habit",
    "Track your daily progress with streaks",
    "Compete on the leaderboard",
    "Find your ideal app blocking schedule"
)

@Composable
fun SevenDayChallengeScreen(
    yearsOnPhone: Int,
    onAccepted: () -> Unit
) {
    var keyframe by remember { mutableIntStateOf(0) }
    var visibleBullets by remember { mutableIntStateOf(0) }
    var showSlider by remember { mutableStateOf(false) }

    val heroScale by animateFloatAsState(
        targetValue = when {
            keyframe < 2 -> 0.1f
            keyframe == 2 -> 1.07f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 120f),
        label = "hero_scale"
    )

    // Keyframe animation sequence (~2s total)
    LaunchedEffect(Unit) {
        delay(200)
        keyframe = 1  // show hero
        delay(250)
        keyframe = 2  // scale hero up
        delay(300)
        keyframe = 3  // settle hero
        delay(400)
        keyframe = 4  // show subtitle

        // Stagger bullet appearances
        for (i in 1..CHALLENGE_BULLETS.size) {
            delay(200)
            visibleBullets = i
        }

        delay(300)
        showSlider = true
    }

    val hoursSaved = yearsOnPhone * 2 // rough estimate of hours per day savings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Hero section
        AnimatedVisibility(
            visible = keyframe >= 1,
            enter = fadeIn()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(heroScale)
            ) {
                Text(
                    text = "\uD83C\uDFC6",
                    style = OnboardingTypography.extraLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "7 Day Challenge",
                    style = OnboardingTypography.h1,
                    color = OnboardingTokens.BrandPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Subtitle + bullets
        AnimatedVisibility(
            visible = keyframe >= 4,
            enter = fadeIn()
        ) {
            Column(
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = "This week, BePresent will help you:",
                    style = OnboardingTypography.p2,
                    color = OnboardingTokens.Neutral900
                )

                Spacer(modifier = Modifier.height(25.dp))

                CHALLENGE_BULLETS.forEachIndexed { index, message ->
                    AnimatedVisibility(
                        visible = index < visibleBullets,
                        enter = fadeIn() + slideInVertically { 12 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Image(
                                painter = painterResource(R.drawable.blue_check),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = message,
                                style = OnboardingTypography.p3,
                                color = OnboardingTokens.Neutral900
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom section: hours saved badge + slider
        AnimatedVisibility(
            visible = showSlider,
            enter = fadeIn()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 60.dp)
            ) {
                // Hours saved badge
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(OnboardingTokens.GreenFill)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = OnboardingTokens.GreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Win back ${hoursSaved / 2}+ hours every day",
                        style = OnboardingTypography.label,
                        color = OnboardingTokens.Neutral900
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SlideToConfirmButton(
                    title = "Slide to accept challenge",
                    completedTitle = "Challenge accepted!",
                    onComplete = onAccepted
                )
            }
        }
    }
}
