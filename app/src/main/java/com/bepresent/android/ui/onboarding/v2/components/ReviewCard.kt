package com.bepresent.android.ui.onboarding.v2.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography

data class ReviewData(
    val user: String,
    val title: String,
    val body: String,
    val stars: Int = 5
)

@Composable
fun ReviewCard(
    review: ReviewData,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .shadow(
                elevation = 35.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = OnboardingTokens.NeutralBlack.copy(alpha = 0.25f),
                spotColor = OnboardingTokens.NeutralBlack.copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = OnboardingTokens.NeutralWhite
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Stars + reviewer name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row {
                    repeat(review.stars) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = OnboardingTokens.YellowPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = review.user,
                    style = OnboardingTypography.subLabel,
                    color = OnboardingTokens.Neutral800
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = review.title,
                style = OnboardingTypography.label,
                color = OnboardingTokens.NeutralBlack,
                minLines = 1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = review.body,
                style = OnboardingTypography.label2,
                color = OnboardingTokens.Neutral900,
                minLines = 3,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
