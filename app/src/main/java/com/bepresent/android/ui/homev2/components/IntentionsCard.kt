package com.bepresent.android.ui.homev2.components

import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.bepresent.android.data.db.AppIntention
import com.bepresent.android.ui.homev2.HomeV2Tokens

@Composable
fun IntentionsCard(
    intentions: List<AppIntention>,
    onReload: () -> Unit,
    onAdd: () -> Unit,
    onIntentionClick: (AppIntention) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "App Intentions",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeV2Tokens.NeutralBlack
            )

            IconButton(onClick = onReload, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    modifier = Modifier.size(18.dp),
                    tint = HomeV2Tokens.BrandPrimary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onAdd,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = HomeV2Tokens.BrandPrimary,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add intention",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Intention list
        intentions.forEach { intention ->
            IntentionListItem(
                intention = intention,
                onClick = { onIntentionClick(intention) }
            )
            Divider(
                color = HomeV2Tokens.Neutral200,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dashed "Add App Intention" footer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAdd),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = HomeV2Tokens.NeutralWhite),
            border = BorderStroke(
                width = 1.dp,
                color = HomeV2Tokens.Neutral200
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = HomeV2Tokens.BrandPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add App Intention",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = HomeV2Tokens.BrandPrimary
                )
            }
        }
    }
}

@Composable
private fun IntentionListItem(
    intention: AppIntention,
    onClick: () -> Unit
) {
    val isOverLimit = intention.totalOpensToday >= intention.allowedOpensPerDay
    val opensColor = if (isOverLimit) Color(0xFFEF2424) else Color(0xFF32BC00)

    val context = LocalContext.current
    val appIcon = remember(intention.packageName) {
        try {
            context.packageManager.getApplicationIcon(intention.packageName)
                .toBitmap(64, 64).asImageBitmap()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (appIcon != null) {
            Image(
                bitmap = appIcon,
                contentDescription = intention.appName,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "${intention.totalOpensToday}/${intention.allowedOpensPerDay}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = opensColor,
            modifier = Modifier.weight(1f)
        )
        if (intention.streak > 0) {
            Text(
                text = "\uD83D\uDD25 ${intention.streak}",
                fontSize = 12.sp,
                color = HomeV2Tokens.OrangePrimary
            )
        }
    }
}
