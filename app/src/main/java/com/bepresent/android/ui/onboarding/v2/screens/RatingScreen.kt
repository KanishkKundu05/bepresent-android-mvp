package com.bepresent.android.ui.onboarding.v2.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.bepresent.android.R
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.LaurelBadge
import com.bepresent.android.ui.onboarding.v2.components.OnboardingContinueButton
import com.bepresent.android.ui.onboarding.v2.components.OnboardingButtonAppearance
import com.google.android.play.core.review.ReviewManagerFactory

@Composable
fun RatingScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val reviewManager = remember { ReviewManagerFactory.create(context) }

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
                text = "Help Us Grow\nWith a Rating!",
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
                // Request in-app review
                val request = reviewManager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        val activity = context as? Activity
                        if (activity != null) {
                            reviewManager.launchReviewFlow(activity, reviewInfo)
                        }
                    }
                }
                onContinue()
            }
        )
    }
}
