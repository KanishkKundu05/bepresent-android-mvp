package com.bepresent.android.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bepresent.android.data.convex.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPartnerClick: (String) -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val partners by viewModel.partners.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val intentionCountdownEnabled by viewModel.intentionCountdownEnabled.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Settings
            Text("Settings", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Intention Countdown",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Show a countdown before you can open a blocked app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = intentionCountdownEnabled,
                        onCheckedChange = { viewModel.setIntentionCountdownEnabled(it) }
                    )
                }
            }

            when (authState) {
                is AuthState.Unauthenticated -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Sign in to sync stats, view leaderboards, and add accountability partners.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.login() }) {
                                Text("Sign In")
                            }
                        }
                    }
                }

                is AuthState.Loading -> {
                    Text("Signing in...", style = MaterialTheme.typography.bodyMedium)
                }

                is AuthState.Authenticated -> {
                    // Display Name Editor
                    profile?.let { p ->
                        var editName by remember(p.displayName) { mutableStateOf(p.displayName) }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Display Name", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.updateDisplayName(editName) },
                                        enabled = editName.isNotBlank() && editName != p.displayName
                                    ) {
                                        Text("Save")
                                    }
                                }
                            }
                        }

                        // Friend Code
                        p.friendCode?.let { code ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Friend Code", style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            code,
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                    }
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(code))
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                    }
                                }
                            }
                        }
                    }

                    // Pending Partner Requests
                    val pendingRequests = partners.filter { it.isIncoming && it.status == "pending" }
                    if (pendingRequests.isNotEmpty()) {
                        Text("Pending Requests", style = MaterialTheme.typography.titleMedium)
                        pendingRequests.forEach { request ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        request.otherDisplayName,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    TextButton(onClick = {
                                        viewModel.respondToRequest(request.partnershipId, true)
                                    }) {
                                        Text("Accept")
                                    }
                                    TextButton(onClick = {
                                        viewModel.respondToRequest(request.partnershipId, false)
                                    }) {
                                        Text("Reject", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // My Partners
                    val acceptedPartners = partners.filter { it.status == "accepted" }
                    if (acceptedPartners.isNotEmpty()) {
                        Text("My Partners", style = MaterialTheme.typography.titleMedium)
                        acceptedPartners.forEach { partner ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onPartnerClick(partner.otherUserId) }
                            ) {
                                Text(
                                    partner.otherDisplayName,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    // Sync Status
                    if (pendingSyncCount > 0) {
                        Text(
                            "$pendingSyncCount items pending sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Logout
                    OutlinedButton(onClick = { viewModel.logout() }) {
                        Text("Sign Out")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
