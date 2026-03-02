package com.bepresent.android.ui.screentime

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.bepresent.android.data.usage.AppUsageInfo
import com.bepresent.android.ui.components.formatDuration
import com.bepresent.android.ui.homev2.BackgroundV2
import com.bepresent.android.ui.homev2.CardV2
import com.bepresent.android.ui.homev2.HomeV2Tokens

private const val MAX_SCREEN_TIME_MS = 8 * 60 * 60 * 1000L // 8 hours

@Composable
fun ScreenTimeScreen(viewModel: ScreenTimeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        BackgroundV2()

        if (!uiState.hasPermission) {
            PermissionPrompt(
                onGrantClick = { viewModel.refresh() },
                modifier = Modifier.padding(top = statusBarTop)
            )
        } else {
            ScreenTimeContent(
                totalScreenTimeMs = uiState.totalScreenTimeMs,
                perAppUsage = uiState.perAppUsage,
                modifier = Modifier.padding(top = statusBarTop)
            )
        }
    }
}

@Composable
private fun PermissionPrompt(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Usage Access Required",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Grant usage access so BePresent can show which apps you use and for how long.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f),
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                context.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = HomeV2Tokens.BrandPrimary
            )
        ) {
            Text("Open Settings", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ScreenTimeContent(
    totalScreenTimeMs: Long,
    perAppUsage: List<AppUsageInfo>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pm = context.packageManager

    // Filter to launchable apps only
    val launchableApps = remember(perAppUsage) {
        perAppUsage.filter { app ->
            pm.getLaunchIntentForPackage(app.packageName) != null
        }
    }

    val maxAppTimeMs = remember(launchableApps) {
        launchableApps.firstOrNull()?.totalTimeMs ?: 1L
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Text(
            text = "Screen Time",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
        )

        // Total time ring
        CardV2(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = (totalScreenTimeMs.toFloat() / MAX_SCREEN_TIME_MS).coerceIn(0f, 1f),
                        modifier = Modifier.size(140.dp),
                        strokeWidth = 12.dp,
                        color = HomeV2Tokens.BrandPrimary,
                        trackColor = HomeV2Tokens.Neutral200,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatDuration(totalScreenTimeMs),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeV2Tokens.NeutralBlack
                        )
                        Text(
                            text = "today",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App list
        CardV2(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 8.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    text = "Apps",
                    style = HomeV2Tokens.CardTitleStyle,
                    color = HomeV2Tokens.NeutralBlack
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Scrollable app list (outside CardV2 for proper scrolling)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(launchableApps) { app ->
                AppUsageRow(
                    app = app,
                    maxTimeMs = maxAppTimeMs,
                    pm = pm
                )
            }
            // Bottom spacing for nav bar
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun AppUsageRow(
    app: AppUsageInfo,
    maxTimeMs: Long,
    pm: PackageManager
) {
    val appLabel = remember(app.packageName) {
        try {
            pm.getApplicationLabel(
                pm.getApplicationInfo(app.packageName, 0)
            ).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            app.packageName.substringAfterLast(".")
        }
    }
    val appIcon = remember(app.packageName) {
        try {
            pm.getApplicationIcon(app.packageName).toBitmap(64, 64).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        if (appIcon != null) {
            Image(
                bitmap = appIcon,
                contentDescription = appLabel,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(HomeV2Tokens.Neutral200),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appLabel.take(1),
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name + bar + time
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = HomeV2Tokens.NeutralBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatDuration(app.totalTimeMs),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Usage bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(HomeV2Tokens.Neutral200)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            (app.totalTimeMs.toFloat() / maxTimeMs).coerceIn(0f, 1f)
                        )
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(HomeV2Tokens.BrandPrimary)
                )
            }
        }
    }
}
