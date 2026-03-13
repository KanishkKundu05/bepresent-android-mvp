package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.LaurelBadge
import com.bepresent.android.ui.onboarding.v2.components.OnboardingButtonAppearance
import com.bepresent.android.ui.onboarding.v2.components.OnboardingContinueButton
import com.bepresent.android.ui.onboarding.v2.components.ReviewCard
import com.bepresent.android.ui.onboarding.v2.components.ReviewData

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserWhatScreen(onContinue: () -> Unit) {
    val reviews = remember {
        listOf(
            ReviewData(
                user = "Sarah Em.",
                title = "Actually works",
                body = "I tried a bunch of screen time apps. BePresent is the first one that helped me cut back and keep my streak going."
            ),
            ReviewData(
                user = "James K.",
                title = "Huge difference",
                body = "My daily screen time dropped fast. Competing with friends on the leaderboard made staying off my phone feel fun."
            ),
            ReviewData(
                user = "Emily R.",
                title = "Worth it",
                body = "I am reading more, sleeping better, and getting my evenings back with family. BePresent made the habit stick."
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { reviews.size })

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding)
    ) {
        val titleTopPadding = maxHeight * 0.12f

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(titleTopPadding))

            Text(
                text = "567,000+ People\nAchieved Their\nGoals with\nBePresent",
                style = OnboardingTypography.title2,
                color = OnboardingTokens.NeutralBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            LaurelBadge {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row {
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = OnboardingTokens.YellowPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "22,139+",
                        style = OnboardingTypography.h1,
                        color = OnboardingTokens.NeutralBlack
                    )
                    Text(
                        text = "5-Star Reviews",
                        style = OnboardingTypography.p2,
                        color = OnboardingTokens.NeutralBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(end = 44.dp),
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(166.dp)
            ) { page ->
                ReviewCard(review = reviews[page])
            }

            Spacer(modifier = Modifier.height(64.dp))
            Spacer(modifier = Modifier.weight(1f))

            OnboardingContinueButton(
                title = "Continue",
                appearance = OnboardingButtonAppearance.Secondary,
                onClick = onContinue
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
