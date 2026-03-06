package com.bepresent.android.ui.intention

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepresent.android.data.db.AppIntention
import com.bepresent.android.ui.homev2.HomeV2Tokens
import com.bepresent.android.ui.homev2.components.FullButton
import com.bepresent.android.ui.homev2.components.FullButtonAppearance
import com.bepresent.android.ui.picker.AppPickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentionConfigSheet(
    existingIntention: AppIntention? = null,
    excludePackages: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onSave: (packageName: String, appName: String, allowedOpensPerDay: Int, timePerOpenMinutes: Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedPackage by remember { mutableStateOf(existingIntention?.packageName) }
    var selectedAppName by remember { mutableStateOf(existingIntention?.appName) }
    var allowedOpens by remember { mutableIntStateOf(existingIntention?.allowedOpensPerDay ?: 10) }
    var timePerOpen by remember { mutableIntStateOf(existingIntention?.timePerOpenMinutes ?: 5) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HomeV2Tokens.NeutralWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = "Set an App Intention",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HomeV2Tokens.NeutralBlack
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Line 1: I'll open {CHOOSE APP}
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "I'll open",
                    fontSize = 17.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(HomeV2Tokens.Brand100)
                        .clickable { showAppPicker = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedAppName ?: "Choose App",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeV2Tokens.BrandPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = HomeV2Tokens.BrandPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Line 2: no more than {10} times a day
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "no more than",
                    fontSize = 17.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(10.dp))
                InlineStepper(
                    value = "$allowedOpens",
                    onDecrement = { if (allowedOpens > 1) allowedOpens-- },
                    onIncrement = { if (allowedOpens < 20) allowedOpens++ }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "times a day",
                    fontSize = 17.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Line 3: for {5 min} each time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "for",
                    fontSize = 17.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(10.dp))
                InlineStepper(
                    value = "$timePerOpen min",
                    onDecrement = { if (timePerOpen > 1) timePerOpen-- },
                    onIncrement = { if (timePerOpen < 30) timePerOpen++ }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "each time",
                    fontSize = 17.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // CTA button — hero style like "Block Now"
            FullButton(
                title = if (existingIntention != null) "Save Changes" else "Set App Intention",
                appearance = FullButtonAppearance.Primary,
                enabled = selectedPackage != null,
                onClick = {
                    if (selectedPackage != null && selectedAppName != null) {
                        onSave(selectedPackage!!, selectedAppName!!, allowedOpens, timePerOpen)
                    }
                }
            )

            // Delete button for edit mode
            if (onDelete != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Delete Intention",
                        color = HomeV2Tokens.DangerPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Nested App Picker sheet
    if (showAppPicker) {
        AppPickerSheet(
            multiSelect = false,
            excludePackages = excludePackages,
            onDismiss = { showAppPicker = false },
            onAppsSelected = { apps ->
                if (apps.isNotEmpty()) {
                    selectedPackage = apps.first().packageName
                    selectedAppName = apps.first().label
                }
                showAppPicker = false
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "Delete Intention",
                    fontWeight = FontWeight.Bold,
                    color = HomeV2Tokens.NeutralBlack
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this intention? This action cannot be undone.",
                    color = HomeV2Tokens.NeutralBlack
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                }) {
                    Text(
                        text = "Delete",
                        color = HomeV2Tokens.DangerPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(
                        text = "Cancel",
                        color = HomeV2Tokens.NeutralBlack
                    )
                }
            },
            containerColor = HomeV2Tokens.NeutralWhite
        )
    }
}

@Composable
private fun InlineStepper(
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(HomeV2Tokens.Brand100)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(HomeV2Tokens.NeutralWhite)
                .clickable(onClick = onDecrement),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease",
                modifier = Modifier.size(16.dp),
                tint = HomeV2Tokens.BrandPrimary
            )
        }
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = HomeV2Tokens.BrandPrimary,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .widthIn(min = 28.dp),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(HomeV2Tokens.NeutralWhite)
                .clickable(onClick = onIncrement),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase",
                modifier = Modifier.size(16.dp),
                tint = HomeV2Tokens.BrandPrimary
            )
        }
    }
}
