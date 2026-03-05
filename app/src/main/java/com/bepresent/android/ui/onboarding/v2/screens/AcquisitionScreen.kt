package com.bepresent.android.ui.onboarding.v2.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bepresent.android.R
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.SurveyListItem
import kotlinx.coroutines.delay

private data class AcquisitionOption(val title: String, @DrawableRes val iconRes: Int?)

@Composable
fun AcquisitionScreen(
    onSelect: (String) -> Unit
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val options = remember {
        val base = listOf(
            AcquisitionOption("TikTok", R.drawable.tiktok_favi),
            AcquisitionOption("Instagram", R.drawable.insta_favi),
            AcquisitionOption("Facebook", R.drawable.facebook_favi),
            AcquisitionOption("Reddit", R.drawable.reddit_favi),
            AcquisitionOption("Recommended by a Friend", R.drawable.user_favi),
            AcquisitionOption("Google Play", R.drawable.google_play_favi),
        ).shuffled()
        base + AcquisitionOption("Other", null)
    }

    // 300ms delay before advancing
    LaunchedEffect(selectedOption) {
        if (selectedOption != null && isProcessing) {
            delay(300)
            onSelect(selectedOption!!)
        }
    }

    // 2s cleanup: reset state if user comes back
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            delay(2000)
            selectedOption = null
            isProcessing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "How did you hear\nabout BePresent?",
            style = OnboardingTypography.h2,
            color = OnboardingTokens.NeutralBlack,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            options.forEach { option ->
                val itemAlpha = when {
                    !isProcessing -> 1f
                    selectedOption == option.title -> 1f
                    else -> 0.5f
                }
                SurveyListItem(
                    title = option.title,
                    iconRes = option.iconRes,
                    isSelected = selectedOption == option.title,
                    enabled = !isProcessing,
                    modifier = Modifier.alpha(itemAlpha),
                    onClick = {
                        selectedOption = option.title
                        isProcessing = true
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}
