package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.OnboardingButtonAppearance
import com.bepresent.android.ui.onboarding.v2.components.OnboardingContinueButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrialReminderSheet(
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isConfirming by remember { mutableStateOf(false) }
    var animateBell by remember { mutableStateOf(false) }

    val bellSwing by rememberInfiniteTransition(label = "trialReminderBell").animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bellSwing"
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (!isConfirming) onDismiss()
        },
        sheetState = sheetState,
        containerColor = OnboardingTokens.NeutralWhite,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeightIn(min = 380.dp)
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Trial reminder bell",
                tint = OnboardingTokens.BrandPrimary,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(48.dp)
                    .rotate(if (animateBell) bellSwing else 0f),
            )

            Text(
                text = "We've got your back",
                style = OnboardingTypography.h2,
                color = OnboardingTokens.NeutralBlack,
                textAlign = TextAlign.Center
            )

            Text(
                text = "You'll get notifications about when your trial is about to end and more.",
                style = OnboardingTypography.p3,
                color = OnboardingTokens.Neutral800,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            OnboardingContinueButton(
                title = "Enable trial reminder",
                appearance = OnboardingButtonAppearance.Primary,
                enabled = !isConfirming,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (isConfirming) return@OnboardingContinueButton
                    isConfirming = true
                    animateBell = true
                    scope.launch {
                        delay(1000)
                        onContinue()
                    }
                }
            )

            OnboardingContinueButton(
                title = "No thanks, I'll remember",
                appearance = OnboardingButtonAppearance.Plain,
                enabled = !isConfirming,
                modifier = Modifier.fillMaxWidth(),
                onClick = onContinue
            )
        }
    }
}
