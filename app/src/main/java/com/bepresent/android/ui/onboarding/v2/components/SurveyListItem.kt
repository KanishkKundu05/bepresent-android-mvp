package com.bepresent.android.ui.onboarding.v2.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography

@Composable
fun SurveyListItem(
    title: String,
    emoji: String? = null,
    @DrawableRes iconRes: Int? = null,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)

    Box(modifier = modifier.fillMaxWidth()) {
        // Drop shadow (offset down 3dp) — only when not selected
        if (!isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = 3.dp)
                    .clip(shape)
                    .background(OnboardingTokens.Neutral300)
                    .padding(16.dp)
            ) {
                // Invisible content for sizing
                Text(text = title, style = OnboardingTypography.p3, color = OnboardingTokens.Neutral300)
            }
        }

        // Main content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    if (isSelected) OnboardingTokens.Brand100
                    else OnboardingTokens.Neutral100
                )
                .then(
                    if (isSelected) Modifier.border(2.dp, OnboardingTokens.Brand300, shape)
                    else Modifier
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(25.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else if (emoji != null) {
                Text(text = emoji, style = OnboardingTypography.p1)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = title,
                style = OnboardingTypography.p3,
                color = OnboardingTokens.NeutralBlack
            )
        }
    }
}
