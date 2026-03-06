package com.bepresent.android.ui.homev2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepresent.android.ui.homev2.HomeV2Tokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeHeaderRow(
    streak: Int,
    isStreakFrozen: Boolean,
    weeklyXp: Int,
    onProfileClick: () -> Unit,
    onDevClick: () -> Unit = {},
    onStreakClick: () -> Unit = {},
    onXpClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile icon button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(HomeV2Tokens.NeutralWhite.copy(alpha = 0.3f))
                .combinedClickable(onClick = onProfileClick, onLongClick = onDevClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                modifier = Modifier.size(22.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Streak pill
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isStreakFrozen) HomeV2Tokens.Brand100 else HomeV2Tokens.OrangeFill)
                .clickable(onClick = onStreakClick)
                .padding(vertical = 8.dp, horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isStreakFrozen) HomeV2Tokens.BrandPrimary else HomeV2Tokens.OrangePrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$streak",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (isStreakFrozen) HomeV2Tokens.BrandPrimary else HomeV2Tokens.OrangePrimary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // XP pill
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(HomeV2Tokens.YellowFill)
                .clickable(onClick = onXpClick)
                .padding(vertical = 8.dp, horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = HomeV2Tokens.YellowPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$weeklyXp XP",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = HomeV2Tokens.YellowPrimary
            )
        }
    }
}
