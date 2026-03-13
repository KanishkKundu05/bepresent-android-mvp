package com.bepresent.android.ui.onboarding.v2.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import com.bepresent.android.R
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.LaurelBadge
import com.bepresent.android.ui.onboarding.v2.components.OnboardingButtonAppearance
import com.bepresent.android.ui.onboarding.v2.components.OnboardingContinueButton
import com.google.android.play.core.review.ReviewManagerFactory

@Composable
fun RatingScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val reviewManager = remember { ReviewManagerFactory.create(context) }
    var showRatingPrompt by remember { mutableStateOf(false) }
    var selectedStars by remember { mutableIntStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun continueAfterPrompt() {
        showRatingPrompt = false
        isSubmitting = false
        selectedStars = 0
        onContinue()
    }

    fun launchInAppReview() {
        val activity = context as? Activity ?: run {
            continueAfterPrompt()
            return
        }

        isSubmitting = true
        reviewManager.requestReviewFlow()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    continueAfterPrompt()
                    return@addOnCompleteListener
                }

                reviewManager.launchReviewFlow(activity, task.result)
                    .addOnCompleteListener {
                        continueAfterPrompt()
                    }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 25.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            Text(
                text = "Enjoying\nthe present?",
                style = OnboardingTypography.h1,
                color = OnboardingTokens.NeutralBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Founders photo
            Image(
                painter = painterResource(R.drawable.jack_and_charles),
                contentDescription = "Jack and Charles",
                modifier = Modifier
                    .width(235.dp)
                    .shadow(
                        elevation = 35.dp,
                        shape = RoundedCornerShape(8.dp),
                        ambientColor = OnboardingTokens.NeutralBlack.copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .rotate(-1f),
                contentScale = ContentScale.FillWidth
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "We're brothers who couldn't stop scrolling, so we built BePresent. For us, and for you.",
                style = OnboardingTypography.p1,
                color = OnboardingTokens.NeutralBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Laurel with stars
            LaurelBadge {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.five_stars),
                        contentDescription = "5 stars",
                        modifier = Modifier.height(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"Thank you for freeing me\nfrom this device.\"",
                        style = OnboardingTypography.subLabel,
                        color = OnboardingTokens.NeutralBlack,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(70.dp))
        }

        // Continue button
        OnboardingContinueButton(
            title = "Continue",
            appearance = OnboardingButtonAppearance.Secondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            onClick = {
                showRatingPrompt = true
            }
        )

        if (showRatingPrompt) {
            AlertDialog(
                onDismissRequest = { continueAfterPrompt() },
                title = {
                    Text(
                        text = "Enjoying the present?",
                        style = OnboardingTypography.h2,
                        color = OnboardingTokens.NeutralBlack
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tap a star to rate BePresent on Google Play.",
                            style = OnboardingTypography.p2,
                            color = OnboardingTokens.NeutralBlack,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (star in 1..5) {
                                IconButton(
                                    onClick = { selectedStars = star },
                                    enabled = !isSubmitting
                                ) {
                                    Icon(
                                        imageVector = if (star <= selectedStars) {
                                            Icons.Filled.Star
                                        } else {
                                            Icons.Outlined.StarOutline
                                        },
                                        contentDescription = "$star star${if (star == 1) "" else "s"}",
                                        tint = OnboardingTokens.YellowPrimary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { launchInAppReview() },
                        enabled = selectedStars > 0 && !isSubmitting
                    ) {
                        Text("Rate now")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { continueAfterPrompt() },
                        enabled = !isSubmitting
                    ) {
                        Text("Not now")
                    }
                }
            )
        }
    }
}
