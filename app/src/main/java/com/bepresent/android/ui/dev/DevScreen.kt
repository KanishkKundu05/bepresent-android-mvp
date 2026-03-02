package com.bepresent.android.ui.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bepresent.android.features.blocking.BlockedAppActivity
import com.bepresent.android.data.db.AppIntention
import com.bepresent.android.ui.picker.AppPickerSheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DevScreen(
    onBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
    viewModel: DevViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAppPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dev Tools") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Permissions ---
            SectionHeader("Permissions")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PermissionRow("Usage Stats", state.permissions.usageStats)
                    PermissionRow("Notifications", state.permissions.notifications)
                    PermissionRow("Battery Opt", state.permissions.batteryOptimization)
                    PermissionRow("Overlay", state.permissions.overlay)
                    PermissionRow("Accessibility", state.permissions.accessibility)
                }
            }

            // --- App Intentions ---
            SectionHeader("App Intentions (${state.intentions.size})")
            if (state.intentions.isEmpty()) {
                Text("No intentions set", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.intentions.forEach { intention ->
                IntentionCard(
                    intention = intention,
                    onOpen = { viewModel.openApp(intention.id) },
                    onReblock = { viewModel.reblockApp(intention.id) },
                    onResetDaily = { viewModel.resetDaily(intention.id) },
                    onDelete = { viewModel.deleteIntention(intention) }
                )
            }
            Button(onClick = { showAppPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Create Test Intention")
            }

            // --- Monitoring ---
            SectionHeader("Monitoring Service")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.startMonitoring() }) { Text("Start") }
                OutlinedButton(onClick = { viewModel.stopMonitoring() }) { Text("Stop") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.launchTestShield(BlockedAppActivity.SHIELD_SESSION) }
                ) { Text("Test Session Shield") }
                OutlinedButton(
                    onClick = { viewModel.launchTestShield(BlockedAppActivity.SHIELD_INTENTION) }
                ) { Text("Test Intention Shield") }
            }
            state.foregroundApp?.let {
                Text("Foreground: $it", style = MaterialTheme.typography.bodySmall)
            }
            if (state.blockedPackages.isNotEmpty()) {
                Text("Blocked: ${state.blockedPackages.joinToString(", ") { it.substringAfterLast('.') }}",
                    style = MaterialTheme.typography.bodySmall)
            }
            state.activeSession?.let { session ->
                Text("Active session: ${session.name} (${session.state})", style = MaterialTheme.typography.bodySmall)
            }

            // --- DataStore ---
            SectionHeader("DataStore")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DataRow("Total XP", state.totalXp.toString())
                    DataRow("Total Coins", state.totalCoins.toString())
                    DataRow("Streak Freeze", if (state.streakFreezeAvailable) "Available" else "Used")
                    DataRow("Active Session ID", state.activeSessionId ?: "none")
                }
            }

            // --- Onboarding ---
            SectionHeader("Onboarding")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DataRow("Status", if (state.onboardingCompleted) "Completed" else "Not completed")
                }
            }
            Button(
                onClick = {
                    viewModel.resetOnboarding()
                    onNavigateToOnboarding()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset & Show Onboarding")
            }

            // --- Onboarding Tester ---
            SectionHeader("Onboarding Tester")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Jump to any screen:", style = MaterialTheme.typography.bodySmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        viewModel.onboardingScreenNames.forEachIndexed { index, name ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.launchOnboardingAtScreen(index)
                                    onNavigateToOnboarding()
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // --- Runtime logs ---
            SectionHeader("Runtime Logs (${state.runtimeLogs.size})")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.clearRuntimeLogs() }) {
                    Text("Clear Logs")
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (state.runtimeLogs.isEmpty()) {
                        Text(
                            "No runtime logs yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.runtimeLogs.takeLast(40).forEach { line ->
                            Text(
                                line,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showAppPicker) {
        val existingPackages = state.intentions.map { it.packageName }.toSet()
        AppPickerSheet(
            multiSelect = false,
            excludePackages = existingPackages,
            onDismiss = { showAppPicker = false },
            onAppsSelected = { apps ->
                showAppPicker = false
                apps.firstOrNull()?.let {
                    viewModel.createIntention(it.packageName, it.label)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntentionCard(
    intention: AppIntention,
    onOpen: () -> Unit,
    onReblock: () -> Unit,
    onResetDaily: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (intention.currentlyOpen)
                MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(intention.appName, style = MaterialTheme.typography.titleSmall)
            Text(
                "Opens: ${intention.totalOpensToday}/${intention.allowedOpensPerDay} | " +
                "Streak: ${intention.streak} | " +
                "Open: ${intention.currentlyOpen} | " +
                "Timer: ${intention.timePerOpenMinutes}m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!intention.currentlyOpen) {
                    Button(onClick = onOpen, modifier = Modifier.height(32.dp)) {
                        Text("Open", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    OutlinedButton(onClick = onReblock, modifier = Modifier.height(32.dp)) {
                        Text("Reblock", style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedButton(onClick = onResetDaily, modifier = Modifier.height(32.dp)) {
                    Text("Reset Daily", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}
