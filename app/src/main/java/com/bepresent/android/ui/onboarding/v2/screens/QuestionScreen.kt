package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bepresent.android.R
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.QuestionOption
import com.bepresent.android.ui.onboarding.v2.QuestionType
import com.bepresent.android.ui.onboarding.v2.components.SurveyListItem

@Composable
fun QuestionScreen(
    title: String,
    emoji: String,
    options: List<QuestionOption>,
    questionType: QuestionType,
    selectedAnswer: String?,
    onSelect: (String) -> Unit
) {
    var isProcessing by remember(title) { mutableStateOf(false) }
    val isAgeQuestion = questionType == QuestionType.Age

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.15f))

        if (isAgeQuestion) {
            Icon(
                imageVector = Icons.Default.Cake,
                contentDescription = "Birthday cake",
                tint = OnboardingTokens.BrandPrimary,
                modifier = Modifier.size(64.dp)
            )
        } else {
            Text(
                text = emoji,
                style = OnboardingTypography.extraLarge,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = title,
            style = OnboardingTypography.h2,
            color = OnboardingTokens.NeutralBlack,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isAgeQuestion) {
                Image(
                    painter = painterResource(R.drawable.session_brick),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(220.dp)
                        .offset(y = 28.dp)
                )
            }

            // Options list
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Adaptive spacing based on option count
                val topSpacing = when {
                    options.size < 3 -> 68.dp
                    options.size < 5 -> 35.dp
                    else -> 10.dp
                }
                Spacer(modifier = Modifier.height(topSpacing))

                options.forEach { option ->
                    SurveyListItem(
                        title = option.title,
                        isSelected = selectedAnswer == option.title,
                        enabled = !isProcessing,
                        onClick = {
                            isProcessing = true
                            onSelect(option.title)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(if (isAgeQuestion) 140.dp else 20.dp))
            }
        }
    }
}
