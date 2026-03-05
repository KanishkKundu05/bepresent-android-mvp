package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import com.bepresent.android.BuildConfig
import com.bepresent.android.R
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.OnboardingContinueButton
import com.bepresent.android.ui.onboarding.v2.components.OnboardingButtonAppearance
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet

private val PAYWALL_FEATURES = listOf(
    "7-day screen time challenge",
    "Smart app blocking",
    "Daily progress tracking",
    "Friends leaderboard"
)

@Composable
fun PaywallScreen(
    onSubscribed: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val paymentSheet = rememberPaymentSheet { result ->
        viewModel.handlePaymentResult(result)
    }

    // When we get a clientSecret, present PaymentSheet
    LaunchedEffect(uiState.clientSecret) {
        val secret = uiState.clientSecret ?: return@LaunchedEffect
        try {
            paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret = secret,
                configuration = PaymentSheet.Configuration.Builder("BePresent")
                    .googlePay(
                        PaymentSheet.GooglePayConfiguration(
                            environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                            countryCode = "US",
                            currencyCode = "USD"
                        )
                    )
                    .build()
            )
        } catch (e: Exception) {
            viewModel.onPaymentSheetError(e.message ?: "Failed to open payment sheet")
        }
    }

    // On subscription success, advance
    LaunchedEffect(uiState.subscriptionSuccess) {
        if (uiState.subscriptionSuccess) {
            onSubscribed()
        }
    }

    // No animation — show everything immediately
    val keyframe = 5
    val visibleFeatures = PAYWALL_FEATURES.size

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
                modifier = Modifier
            ) {
                Text(
                    text = "\uD83D\uDD13", // 🔓
                    style = OnboardingTypography.extraLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unlock BePresent",
                    style = OnboardingTypography.h1,
                    color = OnboardingTokens.BrandPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Features section
        AnimatedVisibility(
            visible = keyframe >= 4,
            enter = fadeIn()
        ) {
            Column(
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = "Everything you need to take control:",
                    style = OnboardingTypography.p2,
                    color = OnboardingTokens.Neutral900
                )

                Spacer(modifier = Modifier.height(25.dp))

                PAYWALL_FEATURES.forEachIndexed { index, feature ->
                    AnimatedVisibility(
                        visible = index < visibleFeatures,
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
                                text = feature,
                                style = OnboardingTypography.p3,
                                color = OnboardingTokens.Neutral900
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Price + CTA section
        AnimatedVisibility(
            visible = keyframe >= 5,
            enter = fadeIn()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                // Price display
                Text(
                    text = "$59.99/year",
                    style = OnboardingTypography.h1,
                    color = OnboardingTokens.NeutralBlack,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "That's just $5.00/month",
                    style = OnboardingTypography.label2,
                    color = OnboardingTokens.Neutral800,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error message
                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        style = OnboardingTypography.subLabel,
                        color = OnboardingTokens.RedPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Subscribe button
                OnboardingContinueButton(
                    title = "Subscribe Now",
                    appearance = OnboardingButtonAppearance.Primary,
                    isLoading = uiState.isLoading,
                    onClick = { viewModel.startSubscription() },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Terms footer
                Text(
                    text = "Cancel anytime. Recurring billing. By subscribing you agree to our Terms of Service.",
                    style = OnboardingTypography.caption2,
                    color = OnboardingTokens.Neutral800,
                    textAlign = TextAlign.Center
                )

                // Dev skip for testing
                if (BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Skip for now (dev only)",
                        style = OnboardingTypography.caption2,
                        color = OnboardingTokens.Neutral800.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { viewModel.skipPaywall() }
                    )
                }
            }
        }
    }
}
