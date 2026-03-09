package com.bepresent.android.ui.homev2.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepresent.android.ui.homev2.HomeV2Tokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionModeSheet(
    currentModeIndex: Int,
    selectedAppCount: Int = 0,
    onDismiss: () -> Unit,
    onSetMode: (index: Int) -> Unit,
    onOpenAppList: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedIndex by remember { mutableIntStateOf(currentModeIndex) }

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
            Text(
                text = "Block Mode",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HomeV2Tokens.NeutralBlack
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Segmented picker
            CustomPickerView(
                options = listOf("All Apps", "Specific Apps"),
                selectedIndex = selectedIndex,
                onSelectionChanged = { selectedIndex = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Apps list summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAppList),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        append(if (selectedIndex == 0) "Allowed Apps" else "Blocked Apps")
                        if (selectedAppCount > 0) append(" ($selectedAppCount)")
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = HomeV2Tokens.NeutralBlack,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open app list",
                    modifier = Modifier.size(20.dp),
                    tint = HomeV2Tokens.NeutralBlack.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Set Mode button
            FullButton(
                title = "Set Mode",
                appearance = FullButtonAppearance.Primary,
                onClick = { onSetMode(selectedIndex) }
            )
        }
    }
}
