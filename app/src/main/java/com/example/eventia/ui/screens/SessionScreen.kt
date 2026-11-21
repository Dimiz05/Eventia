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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.auth_startup_error))
                Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
        } else CircularProgressIndicator()
    }
}

@Composable
fun SessionForm(
    form: SessionUiState.Form,
    onSubmit: (String, String, String) -> Unit,
    onSwitch: () -> Unit,
    onEdit: (AccountField) -> Unit
) {
    var name by rememberSaveable(form.registering) { mutableStateOf("") }
    var email by rememberSaveable(form.registering) { mutableStateOf("") }
    var password by remember(form.registering) { mutableStateOf("") }
    val action = if (form.registering) R.string.auth_register else R.string.auth_sign_in
    val switch = if (form.registering) R.string.auth_sign_in else R.string.auth_register
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
fun AccountScreen(session: SessionUiState.Active, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            session.loading -> CircularProgressIndicator()
            session.account != null -> {
                Text(stringResource(R.string.auth_email), style = MaterialTheme.typography.labelLarge)
                Text(session.account.email)
            }
            else -> {
                Text(stringResource(R.string.auth_account_error))
                Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionFormPreview() {
    EventiaTheme {
        SessionForm(SessionUiState.Form(), { _, _, _ -> }, {}, {})
    }
}
