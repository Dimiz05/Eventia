package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.eventia.R
import com.example.eventia.repositories.AccountField
import com.example.eventia.ui.theme.EventiaTheme

@Composable
fun SessionStartup(showError: Boolean, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.Center) {
        if (showError) {
            ScreenMessage(
                title = stringResource(R.string.auth_startup_error),
                isError = true,
                actionLabel = stringResource(R.string.retry),
                onAction = onRetry
            )
        } else {
            LoadingScreen(stringResource(R.string.auth_loading))
        }
    }
}

@Composable
fun SessionForm(
    form: SessionUiState.Form,
    onSubmit: (String, String, String) -> Unit,
    onSwitch: () -> Unit,
    onEdit: (AccountField) -> Unit,
    onGuest: () -> Unit
) {
    var name by rememberSaveable(form.registering) { mutableStateOf("") }
    var email by rememberSaveable(form.registering) { mutableStateOf("") }
    var password by remember(form.registering) { mutableStateOf("") }
    val action = if (form.registering) R.string.auth_register else R.string.auth_sign_in
    val switch = if (form.registering) R.string.auth_sign_in else R.string.auth_register
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.padding(top = 24.dp).size(64.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("E", style = MaterialTheme.typography.headlineLarge)
            }
        }
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(action), style = MaterialTheme.typography.headlineMedium)
        if (form.registering) {
            SessionField(name, R.string.auth_name, form.errors[AccountField.Name], !form.submitting) {
                name = it
                onEdit(AccountField.Name)
            }
        }
        SessionField(email, R.string.auth_email, form.errors[AccountField.Email], !form.submitting, KeyboardType.Email) {
            email = it
            onEdit(AccountField.Email)
        }
        SessionField(
            password, R.string.auth_password, form.errors[AccountField.Password], !form.submitting, KeyboardType.Password
        ) {
            password = it
            onEdit(AccountField.Password)
        }
        form.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { onSubmit(name, email, password) },
            enabled = !form.submitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(if (form.submitting) R.string.auth_submitting else action))
        }
        TextButton(onClick = onSwitch, enabled = !form.submitting) { Text(stringResource(switch)) }
        if (!form.registering) {
            TextButton(onClick = onGuest, enabled = !form.submitting) {
                Text(stringResource(R.string.auth_continue_guest))
            }
        }
    }
}

@Composable
private fun SessionField(
    value: String,
    label: Int,
    error: Int?,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(stringResource(label)) },
        isError = error != null,
        supportingText = error?.let { message -> { Text(stringResource(message)) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (keyboardType == KeyboardType.Password) PasswordVisualTransformation()
        else VisualTransformation.None
    )
}

@Composable
fun AccountScreen(
    session: SessionUiState.Active?,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            session == null -> Button(onClick = onSignIn) { Text(stringResource(R.string.auth_sign_in)) }
            session.loading || session.signingOut -> CircularProgressIndicator()
            session.account != null -> {
                Text(stringResource(R.string.auth_email), style = MaterialTheme.typography.labelLarge)
                Text(session.account.email)
            }
            else -> {
                Text(stringResource(R.string.auth_account_error))
                Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
        }
        if (session != null) {
            if (session.signOutFailed) {
                Text(stringResource(R.string.auth_sign_out_error), color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = onSignOut, enabled = !session.signingOut) {
                Text(stringResource(R.string.auth_sign_out))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionFormPreview() {
    EventiaTheme {
        SessionForm(SessionUiState.Form(), { _, _, _ -> }, {}, {}, {})
    }
}
