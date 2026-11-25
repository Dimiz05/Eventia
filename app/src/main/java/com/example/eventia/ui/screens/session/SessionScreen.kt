package com.example.eventia.ui.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.eventia.R
import com.example.eventia.ui.components.LoadingScreen
import com.example.eventia.ui.components.ScreenMessage

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
fun AccountScreen(
    session: SessionUiState.Active?,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().padding(24.dp)
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            session == null -> Button(onClick = onSignIn) { Text(stringResource(R.string.auth_sign_in)) }
            session.signingOut -> CircularProgressIndicator()
            session.account != null -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    session.account.name?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
                    Text(stringResource(R.string.auth_email), style = MaterialTheme.typography.labelLarge)
                    Text(session.account.email)
                }
                if (session.loading) CircularProgressIndicator()
                if (session.refreshFailed) {
                    Text(stringResource(R.string.auth_account_error), color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
            }
            session.loading -> CircularProgressIndicator()
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
