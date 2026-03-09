package com.bepresent.android.ui.homev2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepresent.android.data.db.AppIntention
import com.bepresent.android.ui.homev2.HomeV2Tokens

// Colors matching iOS streak views
private val OrangeStreakBg = Color(0xFFFFF7ED)
private val BlueFrozenAccent = Color(0xFF3B82F6)
private val GradientTop = Color(0xFFF0F4FF)
private val GradientBottom = Color(0xFFD6E4FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakSheet(
    intentions: List<AppIntention>,
    isStreakFrozen: Boolean,
    onDismiss: () -> Unit,
    onAddIntention: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        if (intentions.isEmpty()) {
            ZeroAppLimitStreakContent(
                onDismiss = onDismiss,
                onAddIntention = onAddIntention
            )
        } else {
            AppLimitStreakContent(
                intentions = intentions,
                isStreakFrozen = isStreakFrozen,
                onDismiss = onDismiss
            )
        }
    }
}

// --- AppLimitStreakContent (has intentions) ---

@Composable
private fun AppLimitStreakContent(
    intentions: List<AppIntention>,
    isStreakFrozen: Boolean,
    onDismiss: () -> Unit
) {
    val sorted = intentions.sortedByDescending { it.streak }
    val highestStreak = sorted.firstOrNull()?.streak ?: 0
    val flameColor = if (isStreakFrozen) BlueFrozenAccent else HomeV2Tokens.OrangePrimary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrangeStreakBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Title area — flame + streak title
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = flameColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "$highestStreak day streak",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = HomeV2Tokens.NeutralBlack
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (sorted.isNotEmpty() && !isStreakFrozen) {
                Text(
                    text = "of keeping your Intention",
                    fontSize = 13.sp,
                    color = HomeV2Tokens.NeutralBlack
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Intentions area header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Your app intention streaks",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable list of intention cells
            LazyColumn {
                items(sorted, key = { it.id }) { intention ->
                    AppLimitStreakCell(
                        intention = intention,
                        isStreakFrozen = isStreakFrozen
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Close button (top-left)
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .padding(start = 16.dp, top = 20.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = HomeV2Tokens.NeutralBlack,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// --- ZeroAppLimitStreakContent (no intentions) ---

@Composable
private fun ZeroAppLimitStreakContent(
    onDismiss: () -> Unit,
    onAddIntention: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientTop, GradientBottom),
                    startY = Float.POSITIVE_INFINITY * 0.51f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(140.dp))

            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = HomeV2Tokens.OrangePrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Build your streak",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = HomeV2Tokens.NeutralBlack
            )

            Text(
                text = "By adding an App Intention",
                fontSize = 20.sp,
                color = HomeV2Tokens.NeutralBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            FullButton(
                title = "+ Add App Intention",
                appearance = FullButtonAppearance.Gray,
                onClick = {
                    onAddIntention()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pick an app, set your daily limit, and maintain your streak!",
                fontSize = 12.sp,
                color = HomeV2Tokens.NeutralBlack,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(100.dp))
        }

        // Close button (top-left)
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .padding(start = 16.dp, top = 20.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = HomeV2Tokens.NeutralBlack,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// --- Individual streak cell ---

@Composable
private fun AppLimitStreakCell(
    intention: AppIntention,
    isStreakFrozen: Boolean
) {
    val flameColor = if (isStreakFrozen) BlueFrozenAccent else HomeV2Tokens.OrangePrimary
    val context = LocalContext.current
    val appIcon = remember(intention.packageName) {
        try {
            context.packageManager.getApplicationIcon(intention.packageName)
                .toBitmap(64, 64).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HomeV2Tokens.NeutralWhite.copy(alpha = 0.5f))
            .border(2.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        if (appIcon != null) {
            Image(
                bitmap = appIcon,
                contentDescription = intention.appName,
                modifier = Modifier
                    .size(35.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(35.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = intention.appName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = HomeV2Tokens.NeutralBlack
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // App name + opens info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = intention.appName,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = HomeV2Tokens.NeutralBlack
            )
            Text(
                text = "Under ${intention.allowedOpensPerDay} opens",
                fontSize = 12.sp,
                color = HomeV2Tokens.NeutralBlack
            )
        }

        // Streak flame + count
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = flameColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${intention.streak}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = HomeV2Tokens.NeutralBlack
            )
        }
    }
}
