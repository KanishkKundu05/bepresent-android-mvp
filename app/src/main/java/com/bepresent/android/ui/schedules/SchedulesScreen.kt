package com.bepresent.android.ui.schedules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepresent.android.data.db.ScheduledSession
import com.bepresent.android.ui.homev2.BackgroundV2
import com.bepresent.android.ui.homev2.CardV2
import com.bepresent.android.ui.homev2.HomeV2Tokens

@Composable
fun SchedulesScreen(viewModel: SchedulesViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        BackgroundV2()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = statusBarTop + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Schedules",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = HomeV2Tokens.NeutralWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Auto-block apps at set times every day",
                    fontSize = 14.sp,
                    color = HomeV2Tokens.NeutralWhite.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(sessions, key = { it.id }) { session ->
                ScheduleCard(
                    session = session,
                    onToggle = { enabled -> viewModel.toggleSchedule(session.id, enabled) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ScheduleCard(
    session: ScheduledSession,
    onToggle: (Boolean) -> Unit
) {
    CardV2 {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.name,
                        style = HomeV2Tokens.CardTitleStyle,
                        color = HomeV2Tokens.NeutralBlack
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatTime(session.startHour, session.startMinute)} – ${formatTime(session.endHour, session.endMinute)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = HomeV2Tokens.BrandPrimary
                    )
                }

                Switch(
                    checked = session.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = HomeV2Tokens.BrandPrimary,
                        checkedThumbColor = HomeV2Tokens.NeutralWhite
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (session.name == "Morning Ninja")
                    "Block distracting apps every morning so you can start your day focused."
                else
                    "Automatically blocks selected apps during this time window.",
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return String.format("%d:%02d %s", displayHour, minute, amPm)
}
