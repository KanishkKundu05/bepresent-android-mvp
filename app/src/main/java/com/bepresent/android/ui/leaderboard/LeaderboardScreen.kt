package com.bepresent.android.ui.leaderboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bepresent.android.R
import com.bepresent.android.ui.theme.PresentBlue
import com.bepresent.android.ui.theme.SuccessGreen
import com.bepresent.android.ui.theme.TierBronze
import com.bepresent.android.ui.theme.TierDiamond
import com.bepresent.android.ui.theme.TierGold
import com.bepresent.android.ui.theme.TierPlatinum
import com.bepresent.android.ui.theme.TierSilver
import kotlinx.coroutines.delay
import kotlin.math.abs

// ── Main entry point ──

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    when {
        state.showIntro -> LeaderboardIntroContent(
            onDismiss = { viewModel.markIntroShown() }
        )
        state.showResults && state.resultsInfo != null -> LeaderboardResultsContent(
            resultsInfo = state.resultsInfo!!,
            currentTier = state.currentTier,
            onContinue = { viewModel.dismissResults() }
        )
        else -> TieredLeaderboardContent(state = state)
    }
}

// ── Main leaderboard view ──

@Composable
private fun TieredLeaderboardContent(state: LeaderboardUiState) {
    val listState = rememberLazyListState()

    // Build flat item list with zone markers
    val displayItems = remember(state.entries, state.maxPromotionRank, state.minDemotionRank, state.username) {
        buildDisplayItems(state.entries, state.maxPromotionRank, state.minDemotionRank, state.username)
    }

    // Auto-scroll to user's position
    LaunchedEffect(state.userRank, displayItems) {
        if (state.userRank > 5 && displayItems.isNotEmpty()) {
            val targetIndex = displayItems.indexOfFirst {
                it is DisplayItem.Entry && it.entry.username == state.username
            }
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // Trophy progression row
        TierTrophiesRow(currentTier = state.currentTier)

        Spacer(Modifier.height(16.dp))

        // Tier title
        Text(
            text = "${state.currentTier.displayName} Tier",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = getTierColor(state.currentTier)
        )

        Spacer(Modifier.height(4.dp))

        // Promotion/demotion info
        val promoRank = state.maxPromotionRank
        if (promoRank > 0) {
            Text(
                text = "Top $promoRank advance to the next tier",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            val bottomCount = 30 - (state.minDemotionRank - 1)
            Text(
                text = "Bottom $bottomCount get demoted",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(Modifier.height(4.dp))

        // Days left
        Text(
            text = "${state.daysLeft} ${if (state.daysLeft == 1) "day" else "days"} left",
            style = MaterialTheme.typography.bodyMedium,
            color = PresentBlue
        )

        Spacer(Modifier.height(12.dp))

        HorizontalDivider()

        // Scrollable entries
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(displayItems, key = { it.key }) { item ->
                when (item) {
                    is DisplayItem.Entry -> LeaderboardEntryRow(
                        entry = item.entry,
                        rank = item.rank,
                        isCurrentUser = item.isCurrentUser,
                        rankColor = item.rankColor
                    )
                    is DisplayItem.ZoneMarker -> ZoneMarkerRow(
                        text = item.text,
                        color = item.color,
                        isUp = item.isUp
                    )
                }
            }
        }
    }
}

// ── Display item model ──

private sealed class DisplayItem(val key: String) {
    data class Entry(
        val entry: TieredLeaderboardEntry,
        val rank: Int,
        val isCurrentUser: Boolean,
        val rankColor: Color
    ) : DisplayItem("entry_$rank")

    data class ZoneMarker(
        val text: String,
        val color: Color,
        val isUp: Boolean
    ) : DisplayItem("zone_$text")
}

private fun buildDisplayItems(
    entries: List<TieredLeaderboardEntry>,
    maxPromotionRank: Int,
    minDemotionRank: Int,
    username: String
): List<DisplayItem> = buildList {
    entries.forEachIndexed { index, entry ->
        val rank = index + 1

        // Demotion zone marker appears before demotion-rank entry
        if (rank == minDemotionRank) {
            add(DisplayItem.ZoneMarker("Demotion Zone", Color.Red, isUp = false))
        }

        val rankColor = when {
            rank <= maxPromotionRank && maxPromotionRank > 0 -> SuccessGreen
            rank >= minDemotionRank && minDemotionRank <= 30 -> Color.Red
            else -> Color.Black
        }

        add(DisplayItem.Entry(entry, rank, isCurrentUser = entry.username == username, rankColor = rankColor))

        // Promotion zone marker appears after promotion-rank entry
        if (rank == maxPromotionRank) {
            add(DisplayItem.ZoneMarker("Promotion Zone", SuccessGreen, isUp = true))
        }
    }
}

// ── Entry row ──

@Composable
private fun LeaderboardEntryRow(
    entry: TieredLeaderboardEntry,
    rank: Int,
    isCurrentUser: Boolean,
    rankColor: Color
) {
    val bgColor = if (isCurrentUser) PresentBlue.copy(alpha = 0.1f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        val rankText = when (rank) {
            1 -> "\uD83E\uDD47" // 🥇
            2 -> "\uD83E\uDD48" // 🥈
            3 -> "\uD83E\uDD49" // 🥉
            else -> "$rank"
        }
        Text(
            text = rankText,
            style = if (rank <= 3) MaterialTheme.typography.headlineLarge
            else MaterialTheme.typography.bodyMedium,
            color = rankColor,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.width(12.dp))

        // Avatar circle
        val avatarColor = mapStringToColor(entry.username)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.username.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.width(16.dp))

        // Username
        Text(
            text = entry.username,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrentUser) PresentBlue else Color.Black,
            modifier = Modifier.weight(1f)
        )

        // Points
        Text(
            text = "${entry.points} XP",
            style = MaterialTheme.typography.bodyMedium,
            color = if (entry.points == 0) Color.Gray else Color.Black
        )
    }
}

// ── Zone marker ──

@Composable
private fun ZoneMarkerRow(text: String, color: Color, isUp: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isUp) "▲" else "▼",
            color = color,
            fontSize = 14.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = color
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isUp) "▲" else "▼",
            color = color,
            fontSize = 14.sp
        )
    }
}

// ── Tier trophies row ──

@Composable
private fun TierTrophiesRow(currentTier: Tier) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 35.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tier.entries.forEach { tier ->
            TierTrophyIcon(
                userTier = currentTier,
                trophyTier = tier
            )
        }
    }
}

@Composable
private fun TierTrophyIcon(userTier: Tier, trophyTier: Tier) {
    val isUnlocked = userTier.ordinal >= trophyTier.ordinal
    val isCurrent = userTier == trophyTier
    val size: Dp = if (isCurrent) 48.dp else 32.dp
    val color = if (isUnlocked) getTierColor(trophyTier) else Color.Gray.copy(alpha = 0.25f)

    val modifier = if (isCurrent) {
        Modifier.shadow(4.dp, shape = CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f))
    } else {
        Modifier
    }

    TrophyIcon(
        modifier = modifier,
        size = size,
        color = color
    )
}

@Composable
private fun TrophyIcon(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = TierBronze
) {
    Icon(
        painter = painterResource(R.drawable.ic_trophy),
        contentDescription = "Trophy",
        tint = color,
        modifier = modifier.size(size)
    )
}

// ── Results screen ──

@Composable
private fun LeaderboardResultsContent(
    resultsInfo: LeaderboardResultsInfo,
    currentTier: Tier,
    onContinue: () -> Unit
) {
    val mainText = when (resultsInfo.tierMovement) {
        TierMovement.PROMOTED -> "Congrats! \uD83C\uDF89"
        TierMovement.STAYED -> "Good Job!"
        TierMovement.DEMOTED -> "\uD83D\uDE2D\uD83D\uDE2D"
    }

    val bodyText = when (resultsInfo.tierMovement) {
        TierMovement.PROMOTED -> "You finished #${resultsInfo.rank} and advanced to the ${resultsInfo.newTier.displayName} Tier!"
        TierMovement.STAYED -> "You finished #${resultsInfo.rank} and stayed in the ${resultsInfo.newTier.displayName} Tier!"
        TierMovement.DEMOTED -> "You finished #${resultsInfo.rank} and were demoted to the ${resultsInfo.newTier.displayName} Tier!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PresentBlue.copy(alpha = 0.15f))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        // Trophy with shadow
        TrophyIcon(
            size = 120.dp,
            color = getTierColor(resultsInfo.newTier),
            modifier = Modifier.shadow(3.dp, shape = CircleShape)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = mainText,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = bodyText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PresentBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Intro screen ──

@Composable
private fun LeaderboardIntroContent(onDismiss: () -> Unit) {
    var showHeader by remember { mutableStateOf(false) }
    var showList by remember { mutableStateOf(false) }

    val dummyEntries = remember {
        listOf(
            "Amelia Flores" to 100, "Jasper Rossi" to 95,
            "Carla Rhiel Madsen" to 90, "Ashlynn Botosh" to 85,
            "Roger Vetrovs" to 80, "Kianna Donin" to 75,
            "Gretchen Yost" to 70, "Marilyn Torff" to 65,
            "Tatiana Carder" to 60, "Kayden Bator" to 55,
            "Corey Aminoff" to 50, "Miracle Herwitz" to 45
        )
    }

    LaunchedEffect(Unit) {
        delay(200)
        showHeader = true
        delay(1300)
        showList = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // Header: gradient background + trophy + text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFB653).copy(alpha = 0.3f),
                            Color.White
                        ),
                        radius = 600f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = showHeader,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TrophyIcon(
                        size = 120.dp,
                        color = TierBronze,
                        modifier = Modifier.shadow(
                            elevation = 20.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFFF97316).copy(alpha = 0.3f)
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Welcome to Leagues",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "You just landed in Bronze league, keep\nearning XP to rise the ranks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.weight(0.5f))

        // Animated leaderboard list
        AnimatedVisibility(
            visible = showList,
            enter = fadeIn(animationSpec = tween(500))
        ) {
            Column {
                // Dummy entries
                dummyEntries.forEach { (name, points) ->
                    IntroEntryRow(username = name, points = points)
                }

                // Highlighted user entry
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .background(
                            Color(0xFFE5F4FF),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PresentBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "M",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "Me",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PresentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "110 XP",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Nice! button
        if (showList) {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PresentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Nice!", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun IntroEntryRow(username: String, points: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(mapStringToColor(username)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = username.take(1).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = username,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$points XP",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

// ── Utilities ──

fun getTierColor(tier: Tier): Color = when (tier) {
    Tier.BRONZE -> TierBronze
    Tier.SILVER -> TierSilver
    Tier.GOLD -> TierGold
    Tier.PLATINUM -> TierPlatinum
    Tier.DIAMOND -> TierDiamond
}

fun mapStringToColor(input: String): Color {
    val colors = listOf(
        Color(0xFFEF4444), // Red
        Color(0xFF3B82F6), // Blue
        Color(0xFF22C55E), // Green
        Color(0xFFEAB308), // Yellow
        Color(0xFFEC4899), // Pink
        Color(0xFFF97316), // Orange
        Color(0xFF8B5CF6), // Purple
        Color(0xFF14B8A6), // Teal
        Color(0xFF6366F1)  // Indigo
    )
    if (input.length < 2) return colors[0]
    val number = input[1].code
    val index = abs(number % colors.size)
    return colors[index]
}
