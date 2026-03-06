package com.bepresent.android.ui.homev2.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepresent.android.ui.homev2.HomeV2Tokens

enum class FullButtonAppearance {
    Primary,
    Gray,
    Plain,
    DangerShadow
}

@Composable
fun FullButton(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    appearance: FullButtonAppearance = FullButtonAppearance.Primary,
    enabled: Boolean = true,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    onClick: () -> Unit
) {
    val backgroundColor = when (appearance) {
        FullButtonAppearance.Primary -> HomeV2Tokens.BrandPrimary
        FullButtonAppearance.Gray -> HomeV2Tokens.Neutral200
        FullButtonAppearance.Plain -> Color.Transparent
        FullButtonAppearance.DangerShadow -> HomeV2Tokens.DangerPrimary
    }

    val contentColor = when (appearance) {
        FullButtonAppearance.Primary -> Color.White
        FullButtonAppearance.Gray -> HomeV2Tokens.NeutralBlack
        FullButtonAppearance.Plain -> HomeV2Tokens.BrandPrimary
        FullButtonAppearance.DangerShadow -> Color.White
    }

    val dropShadowColor = when (appearance) {
        FullButtonAppearance.DangerShadow -> HomeV2Tokens.DangerShadow
        FullButtonAppearance.Primary -> HomeV2Tokens.BrandPrimary.copy(alpha = 0.4f)
        else -> null
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Drop shadow capsule
        if (dropShadowColor != null) {
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .offset(y = 4.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = dropShadowColor,
                    disabledContainerColor = dropShadowColor.copy(alpha = 0.3f)
                ),
                enabled = false,
                contentPadding = PaddingValues(0.dp)
            ) {}
        }

        // Main capsule button
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = contentColor,
                disabledContainerColor = backgroundColor.copy(alpha = 0.5f),
                disabledContentColor = contentColor.copy(alpha = 0.5f)
            ),
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = fontSize
                )
            }
        }
    }
}
