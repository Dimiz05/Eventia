package com.example.eventia.ui.screens.session

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import com.example.eventia.R
import com.example.eventia.repositories.AccountField
import com.example.eventia.ui.theme.EventiaTheme

@Composable
fun SessionForm(
    form: SessionUiState.Form,
    onSubmit: (String, String, String) -> Unit,
    onSwitch: () -> Unit,
    onEdit: (AccountField) -> Unit,
    onGuest: () -> Unit
) {
    val scrollState = rememberScrollState()
    key(form.registering) {
        var name by rememberSaveable { mutableStateOf("") }
        var email by rememberSaveable { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmation by remember { mutableStateOf("") }
        var confirmationError by remember { mutableStateOf<Int?>(null) }
        val focusManager = LocalFocusManager.current

        val switchMode = {
            focusManager.clearFocus()
            onSwitch()
        }

        BackHandler(enabled = form.registering && !form.submitting, onBack = switchMode)
        FestivalSessionForm(
            form = form,
            name = name,
            email = email,
            password = password,
            confirmation = confirmation,
            confirmationError = confirmationError,
            scrollState = scrollState,
            onNameChange = {
                name = it
                onEdit(AccountField.Name)
            },
            onEmailChange = {
                email = it
                onEdit(AccountField.Email)
            },
            onPasswordChange = {
                password = it
                confirmationError = null
                onEdit(AccountField.Password)
            },
            onConfirmationChange = {
                confirmation = it
                confirmationError = null
            },
            onSubmit = {
                focusManager.clearFocus()
                if (form.registering && password != confirmation) {
                    confirmationError = R.string.auth_password_mismatch
                } else {
                    onSubmit(name, email, password)
                }
            },
            onSwitch = switchMode,
            onGuest = onGuest
        )
    }
}

@Preview(name = "Sign in", showBackground = true, widthDp = 393, heightDp = 800)
@Preview(name = "Sign in, small", showBackground = true, widthDp = 320, heightDp = 640)
@Preview(name = "Sign in, dark", showBackground = true, widthDp = 393, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SignInPreview() {
    EventiaTheme { SessionForm(SessionUiState.Form(), { _, _, _ -> }, {}, {}, {}) }
}

@Preview(name = "Create account", showBackground = true, widthDp = 393, heightDp = 800)
@Preview(name = "Create account, small", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun RegistrationPreview() {
    EventiaTheme { SessionForm(SessionUiState.Form(registering = true), { _, _, _ -> }, {}, {}, {}) }
}
