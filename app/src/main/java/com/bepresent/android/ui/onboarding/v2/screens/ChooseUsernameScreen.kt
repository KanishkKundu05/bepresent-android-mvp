package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.OnboardingContinueButton
import com.bepresent.android.ui.onboarding.v2.components.OnboardingButtonAppearance

@Composable
fun ChooseUsernameScreen(
    username: String,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onUsernameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var validationError by remember { mutableStateOf<String?>(null) }

    val isValid = username.length >= 3 && username.matches(Regex("^[a-zA-Z0-9_]+$"))

    // Server error alert dialog
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Choose a Username",
            style = OnboardingTypography.h2,
            color = OnboardingTokens.NeutralBlack
        )

        Spacer(modifier = Modifier.height(40.dp))

        TextField(
            value = username,
            onValueChange = { newValue ->
                onUsernameChanged(newValue.lowercase().take(15))
                validationError = when {
                    newValue.length < 3 -> "Username must be at least 3 characters"
                    !newValue.matches(Regex("^[a-zA-Z0-9_]*$")) -> "Only letters, numbers, and underscores"
                    else -> null
                }
            },
            placeholder = {
                Text(
                    text = "Your username",
                    style = OnboardingTypography.p2,
                    color = OnboardingTokens.Neutral800,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            textStyle = OnboardingTypography.p2.copy(textAlign = TextAlign.Center),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = OnboardingTokens.BrandPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (isValid && !isLoading) onConfirm()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        Divider(color = OnboardingTokens.Neutral800, thickness = 1.dp)

        if (validationError != null && username.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = validationError!!,
                style = OnboardingTypography.caption,
                color = OnboardingTokens.RedPrimary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        OnboardingContinueButton(
            title = "Continue",
            appearance = OnboardingButtonAppearance.Secondary,
            enabled = isValid && !isLoading,
            isLoading = isLoading,
            onClick = {
                keyboardController?.hide()
                onConfirm()
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Request focus on first composition
    androidx.compose.runtime.LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
