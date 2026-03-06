package com.bepresent.android.ui.homev2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepresent.android.ui.homev2.HomeV2Tokens

data class BlockedTimeState(
    val hours: String = "00",
    val minutes: String = "00",
    val seconds: String = "00",
    val dailyRecordHours: Int = 0,
    val dailyRecordMinutes: Int = 0,
    val dailyRecordSeconds: Int = 0,
    val sessionModeLabel: String = "All apps",
    val sessionDurationLabel: String = "1h"
)

@Composable
fun BlockedTimeCard(
    state: BlockedTimeState,
    onSessionModeClick: () -> Unit,
    onSessionGoalClick: () -> Unit,
    onBlockNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Time Blocked today",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeV2Tokens.NeutralBlack
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Daily record chip
        Text(
            text = "Your daily record: ${state.dailyRecordHours}h ${state.dailyRecordMinutes}m ${state.dailyRecordSeconds}s",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = HomeV2Tokens.BrandPrimary,
            modifier = Modifier
                .clip(CircleShape)
                .background(HomeV2Tokens.Brand100)
                .padding(vertical = 6.dp, horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Timer digits row
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            TimerDigitColumn(value = state.hours, label = "Hours")
            TimerSeparator()
            TimerDigitColumn(value = state.minutes, label = "Minutes")
            TimerSeparator()
            TimerDigitColumn(value = state.seconds, label = "Seconds")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Session mode + duration capsules
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // Session mode capsule
            CapsuleEntry(
                icon = Icons.Default.Shield,
                label = state.sessionModeLabel,
                onClick = onSessionModeClick,
                modifier = Modifier.weight(1.68f)
            )
            // Session duration capsule
            CapsuleEntry(
                icon = Icons.Default.Timer,
                label = state.sessionDurationLabel,
                onClick = onSessionGoalClick,
                modifier = Modifier.weight(0.72f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Block Now CTA
        FullButton(
            title = "Block Now",
            icon = Icons.Default.PlayArrow,
            appearance = FullButtonAppearance.Primary,
            fontSize = 20.sp,
            onClick = onBlockNowClick
        )
    }
}

@Composable
private fun TimerDigitColumn(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = HomeV2Tokens.TimerDigitStyle,
            color = HomeV2Tokens.NeutralBlack,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = HomeV2Tokens.TimerLabelStyle,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimerSeparator() {
    Text(
        text = ":",
        style = HomeV2Tokens.TimerDigitStyle.copy(
            color = HomeV2Tokens.NeutralBlack.copy(alpha = 0.3f)
        ),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun CapsuleEntry(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(HomeV2Tokens.NeutralBlack.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = HomeV2Tokens.BrandPrimary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = HomeV2Tokens.NeutralBlack,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.Gray
        )
    }
}
