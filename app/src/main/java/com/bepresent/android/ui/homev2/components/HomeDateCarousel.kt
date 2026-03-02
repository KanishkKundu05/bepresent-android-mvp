package com.bepresent.android.ui.homev2.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
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
private val arcYOffset = listOf(28, 10, 0, 0, 0, 10, 28)
private val arcRotation = listOf(-14f, -10f, -5f, 0f, 5f, 10f, 14f)

@Composable
fun HomeDateCarousel(
    days: List<DayUiModel>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.requiredWidth(520.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            days.forEachIndexed { index, day ->
                CalendarDayCell(
                    day = day,
                    modifier = Modifier
                        .offset(y = arcYOffset.getOrElse(index) { 0 }.dp)
                        .rotate(arcRotation.getOrElse(index) { 0f })
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: DayUiModel,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        day.isCurrentDay -> HomeV2Tokens.NeutralWhite
        day.isEnabled -> HomeV2Tokens.NeutralWhite.copy(alpha = 0.25f)
        else -> HomeV2Tokens.NeutralWhite.copy(alpha = 0.15f)
    }

    val textColor = when {
        day.isCurrentDay -> HomeV2Tokens.NeutralBlack
        day.isEnabled -> HomeV2Tokens.NeutralWhite
        else -> HomeV2Tokens.NeutralWhite.copy(alpha = 0.5f)
    }

    Column(
        modifier = modifier
            .clip(CircleShape)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.weekDay,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = day.number,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Status indicator
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                // Past day failed (screentime below goal) → red cross
                day.isFailed && day.isEnabled -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
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
                // Past day completed → green check
                day.isChecked && day.isEnabled -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HomeV2Tokens.GreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            modifier = Modifier.size(22.dp),
                            tint = Color.White
                        )
                    }
                }
                // Future dates → greyed out tick
                !day.isEnabled -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HomeV2Tokens.NeutralWhite.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Future",
                            modifier = Modifier.size(22.dp),
                            tint = HomeV2Tokens.NeutralWhite.copy(alpha = 0.35f)
                        )
                    }
                }
                // Current day / enabled but not checked → empty circle
                else -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (day.isCurrentDay) HomeV2Tokens.Neutral200
                                else HomeV2Tokens.NeutralWhite.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}
