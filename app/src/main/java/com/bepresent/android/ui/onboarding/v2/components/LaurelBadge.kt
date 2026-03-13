package com.bepresent.android.ui.onboarding.v2.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bepresent.android.R
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography

/**
 * Laurel wreath badge with left/right laurel images framing centered content.
 */
@Composable
fun LaurelBadge(
    modifier: Modifier = Modifier,
    laurelSize: Dp = 60.dp,
    spacing: Dp = 16.dp,
    expanded: Boolean = false,
    content: @Composable () -> Unit
) {
    if (expanded) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.left_laurel),
                contentDescription = null,
                modifier = Modifier.height(laurelSize)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spacing),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
            Image(
                painter = painterResource(R.drawable.right_laurel),
                contentDescription = null,
                modifier = Modifier.height(laurelSize)
            )
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.left_laurel),
                contentDescription = null,
                modifier = Modifier.height(laurelSize)
            )
            Spacer(modifier = Modifier.width(spacing))
            content()
            Spacer(modifier = Modifier.width(spacing))
            Image(
                painter = painterResource(R.drawable.right_laurel),
                contentDescription = null,
                modifier = Modifier.height(laurelSize)
            )
        }
    }
}

/**
 * Stat display inside a laurel badge (e.g. "15 Hour" + "Weekly Screen Time Reduction").
 */
@Composable
fun LaurelStatBadge(
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    laurelSize: Dp = 60.dp,
    spacing: Dp = 16.dp,
    expanded: Boolean = false
) {
    LaurelBadge(
        modifier = modifier,
        laurelSize = laurelSize,
        spacing = spacing,
        expanded = expanded
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = headline,
                style = OnboardingTypography.h1,
                color = OnboardingTokens.NeutralBlack,
                textAlign = TextAlign.Center
            )
            Text(
                text = body,
                style = OnboardingTypography.subLabel,
                color = OnboardingTokens.NeutralBlack,
                textAlign = TextAlign.Center
            )
        }
    }
}
