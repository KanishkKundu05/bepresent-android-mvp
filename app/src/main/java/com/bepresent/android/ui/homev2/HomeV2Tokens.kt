package com.bepresent.android.ui.homev2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * HomeV2 design tokens matching the iOS Swift implementation.
 */
object HomeV2Tokens {
    // Brand colors
    val BrandPrimary = Color(0xFF003BFF)
    val Blue1 = Color(0xFF003BFF)
    val Blue2 = Color(0xFF55B7FF)
    val Blue3 = Color(0xFFABDDFF)

    // Neutrals
    val Neutral100 = Color(0xFFF9F9F9)
    val Neutral200 = Color(0xFFE6E6E6)
    val NeutralWhite = Color(0xFFFFFFFF)
    val NeutralBlack = Color(0xFF000000)

    // Accent fills
    val YellowFill = Color(0xFFFFF9E5)
    val YellowPrimary = Color(0xFFEAB308)
    val OrangeFill = Color(0xFFFFF3E0)
    val OrangePrimary = Color(0xFFF97316)
    val GreenFill = Color(0xFFE8F5E9)
    val GreenPrimary = Color(0xFF22C55E)
    val Brand100 = Color(0xFFE0E7FF)
    val Brand300 = Color(0xFF818CF8)
    val DangerPrimary = Color(0xFFEF4444)
    val DangerShadow = Color(0xFFB91C1C)

    // Card shape
    val CardCornerRadius = 24.dp
    val CardShape = RoundedCornerShape(24.dp)

    // Typography styles (using system font — swap to FFF Acid Grotesk if font file added)
    val TimerDigitStyle = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 54.sp,
        letterSpacing = (-1).sp
    )

    val TimerLabelStyle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        color = Color(0xFF9CA3AF)
    )

    val CardTitleStyle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    )

    val SubLabelStyle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )

    val CountdownNumberStyle = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 96.sp
    )
}

/**
 * cardV2 wrapper: neutral100 background, rounded corners, subtle shadow.
 * Matches iOS cardV2 extension.
 */
@Composable
fun CardV2(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 15.dp,
                shape = HomeV2Tokens.CardShape,
                ambientColor = HomeV2Tokens.NeutralBlack.copy(alpha = 0.15f),
                spotColor = HomeV2Tokens.NeutralBlack.copy(alpha = 0.15f)
            )
            .clip(HomeV2Tokens.CardShape)
            .border(BorderStroke(1.dp, HomeV2Tokens.Neutral200), HomeV2Tokens.CardShape)
            .background(HomeV2Tokens.Neutral100),
        content = content
    )
}

/**
 * backgroundV2: radial gradient masked to a large circle at the top.
 * Matches iOS backgroundV2 extension.
 */
@Composable
fun BackgroundV2(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val colors = listOf(
                    Color(0xFFABDDFF),  // blue3 — lightest center
                    Color(0xFF55B7FF),  // blue2 — mid
                    Color(0xFF003BFF)   // blue1 — darkest edge
                )
                val centerY = -size.height * 0.5f
                val r = size.height * 0.5f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = colors,
                        center = Offset(size.width / 2f, centerY),
                        radius = r
                    ),
                    radius = r,
                    center = Offset(size.width / 2f, centerY)
                )
            }
    )
}
