package com.bepresent.android.ui.homev2.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepresent.android.R
import com.bepresent.android.ui.homev2.HomeV2Tokens

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SessionCountdownCard(
    count: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Cloud background
        Image(
            painter = painterResource(id = R.drawable.cloud_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Title
            Text(
                text = "Blocking apps in",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeV2Tokens.NeutralBlack.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated countdown number
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    (scaleIn(
                        initialScale = 0.5f,
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300)))
                        .togetherWith(fadeOut(animationSpec = tween(150)))
                },
                label = "countdown"
            ) { targetCount ->
                Text(
                    text = "$targetCount",
                    style = HomeV2Tokens.CountdownNumberStyle,
                    color = HomeV2Tokens.NeutralBlack,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Cancel CTA
            FullButton(
                title = "Cancel",
                icon = Icons.Default.Close,
                appearance = FullButtonAppearance.Plain,
                onClick = onCancel
            )
        }
    }
}
