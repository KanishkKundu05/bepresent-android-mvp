package com.bepresent.android.ui.homev2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepresent.android.ui.homev2.HomeV2Tokens

data class DayUiModel(
    val weekDay: String,
    val number: String,
    val isEnabled: Boolean,
    val isChecked: Boolean,
    val isFailed: Boolean = false,
    val isCurrentDay: Boolean
)

// Arc transform arrays matching iOS exactly
private val arcYOffset = listOf(15, -10, -25, -25, -25, -10, 15)
private val arcRotation = listOf(-18f, -15f, -10f, 0f, 10f, 15f, 18f)
private val arcHPadding = listOf(6, 5, 2, 2, 2, 5, 6)

@Composable
fun HomeDateCarousel(
    days: List<DayUiModel>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 30.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.requiredWidth(520.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { index, day ->
                CalendarDayCell(
                    day = day,
                    modifier = Modifier
                        .padding(horizontal = arcHPadding.getOrElse(index) { 0 }.dp)
                        .offset(y = arcYOffset.getOrElse(index) { 0 }.dp)
                        .rotate(arcRotation.getOrElse(index) { 0f })
                )
            }
        }
    }
}

// iOS theme color references
private val Neutral800 = Color(0xFF777777)

@Composable
private fun CalendarDayCell(
    day: DayUiModel,
    modifier: Modifier = Modifier
) {
    val checkSize = if (day.isCurrentDay) 48.dp else 40.dp

    // Text color: current day = brandPrimary (blue), others = white
    val textColor = when {
        day.isCurrentDay -> HomeV2Tokens.BrandPrimary
        day.isEnabled -> HomeV2Tokens.NeutralWhite
        else -> HomeV2Tokens.NeutralWhite.copy(alpha = 0.5f)
    }

    // Cell background matching iOS
    val cellModifier = modifier.clip(CircleShape)
    val bgModifier = when {
        day.isCurrentDay -> cellModifier.background(HomeV2Tokens.NeutralWhite)
        day.isEnabled -> cellModifier.background(HomeV2Tokens.NeutralWhite.copy(alpha = 0.20f))
        else -> cellModifier
            .background(HomeV2Tokens.NeutralWhite.copy(alpha = 0.15f))
            .border(1.5.dp, HomeV2Tokens.NeutralWhite.copy(alpha = 0.15f), CircleShape)
    }

    Column(
        modifier = bgModifier
            .padding(top = 10.dp)
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Weekday label — iOS uses font.tiny (9sp) bold, 0.6 opacity
        Text(
            text = day.weekDay,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        // Day number — iOS uses font.caption (12sp) bold
        Text(
            text = day.number,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Status indicator
        Box(
            modifier = Modifier.size(checkSize),
            contentAlignment = Alignment.Center
        ) {
            when {
                // Past day failed → red cross
                day.isFailed && day.isEnabled -> {
                    Box(
                        modifier = Modifier
                            .size(checkSize)
                            .clip(CircleShape)
                            .background(HomeV2Tokens.DangerPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Below goal",
                            modifier = Modifier.size(22.dp),
                            tint = Color.White
                        )
                    }
                }
                // Past day completed → green check with shadow & gradient
                day.isChecked && day.isEnabled -> {
                    Box(
                        modifier = Modifier
                            .size(checkSize)
                            .shadow(6.dp, CircleShape, ambientColor = HomeV2Tokens.GreenPrimary.copy(alpha = 0.4f), spotColor = HomeV2Tokens.GreenPrimary.copy(alpha = 0.3f))
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        HomeV2Tokens.GreenPrimary,
                                        HomeV2Tokens.GreenPrimary.copy(alpha = 0.8f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                }
                // Future / disabled → stroked circle only (no fill, no checkmark)
                !day.isEnabled -> {
                    Box(
                        modifier = Modifier
                            .size(checkSize)
                            .border(1.5.dp, HomeV2Tokens.NeutralWhite.copy(alpha = 0.15f), CircleShape)
                    )
                }
                // Current day or enabled not checked → grey checkmark
                else -> {
                    Box(
                        modifier = Modifier
                            .size(checkSize)
                            .clip(CircleShape)
                            .background(HomeV2Tokens.Neutral200),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Not completed",
                            modifier = Modifier.size(24.dp),
                            tint = Neutral800.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
