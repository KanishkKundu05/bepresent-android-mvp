package com.bepresent.android.ui.social

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bepresent.android.ui.homev2.BackgroundV2
import com.bepresent.android.ui.homev2.CardV2
import com.bepresent.android.ui.homev2.HomeV2Tokens

@Composable
fun SocialScreen(viewModel: SocialViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val context = LocalContext.current
    var showEmailDialog by remember { mutableStateOf(false) }

    // Contact picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        if (contactUri != null) {
            val result = extractContactInfo(context, contactUri)
            if (result != null) {
                viewModel.addPartner(result.first, result.second)
            }
        }
    }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactPickerLauncher.launch(null)
        }
    }

    val onAddPartnerClick: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            contactPickerLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BackgroundV2()

        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusBarTop)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Accountability Partners",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onAddPartnerClick) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Partner",
                            tint = Color.White
                        )
                    }
                }

                // Subtitle
                Text(
                    text = "They\u2019ll get a text when you break your intentions",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                )

                if (uiState.partners.isEmpty()) {
                    // Empty state
                    CardV2(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = HomeV2Tokens.BrandPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Add an Accountability Partner",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HomeV2Tokens.NeutralBlack
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pick a contact and they\u2019ll be notified via text message when you break your screen time intentions. Stay honest, stay focused.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onAddPartnerClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HomeV2Tokens.BrandPrimary
                                )
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add from Contacts")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Add from Email",
                                fontSize = 14.sp,
                                color = HomeV2Tokens.BrandPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { showEmailDialog = true }
                            )
                        }
                    }
                } else {
                    // Partner list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.partners) { partner ->
                            AccountabilityPartnerRow(
                                partner = partner,
                                onRemove = { viewModel.removePartner(partner.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // Email dialog
        if (showEmailDialog) {
            AddFromEmailDialog(
                onDismiss = { showEmailDialog = false },
                onAdd = { name, email ->
                    viewModel.addPartnerByEmail(name, email)
                    showEmailDialog = false
                }
            )
        }

        // Error display
        uiState.addError?.let { error ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HomeV2Tokens.DangerPrimary)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(error, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun AccountabilityPartnerRow(
    partner: AccountabilityPartner,
    onRemove: () -> Unit
) {
    CardV2 {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle with initial
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(HomeV2Tokens.Brand100),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = partner.contactName.take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HomeV2Tokens.BrandPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = partner.contactName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = HomeV2Tokens.NeutralBlack
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = partner.phoneNumber,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove partner",
                    tint = HomeV2Tokens.DangerPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AddFromEmailDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, email: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add from Email") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name.trim(), email.trim()) },
                enabled = name.isNotBlank() && email.contains("@")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Extract name and phone number from a contact URI returned by the contact picker.
 */
private fun extractContactInfo(context: Context, contactUri: Uri): Pair<String, String>? {
    var contactName = ""
    var contactId = ""

    val cursor: Cursor? = context.contentResolver.query(
        contactUri,
        arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
        ),
        null, null, null
    )
    cursor?.use {
        if (it.moveToFirst()) {
            contactId = it.getString(
                it.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            )
            contactName = it.getString(
                it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            ) ?: ""
        }
    }

    if (contactId.isEmpty()) return null

    val phoneCursor: Cursor? = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
        arrayOf(contactId),
        null
    )
    var phoneNumber = ""
    phoneCursor?.use {
        if (it.moveToFirst()) {
            phoneNumber = it.getString(
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            ) ?: ""
        }
    }

    if (phoneNumber.isEmpty() || contactName.isEmpty()) return null

    // Normalize: remove spaces, dashes, parens
    val normalizedPhone = phoneNumber.replace(Regex("[\\s\\-()]+"), "")

    return Pair(contactName, normalizedPhone)
}
