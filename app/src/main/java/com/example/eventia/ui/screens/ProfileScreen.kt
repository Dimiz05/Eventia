package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.domain.model.Event
import com.example.eventia.ui.theme.EventiaTheme

@Composable
internal fun ProfileRoute(
    session: SessionUiState.Active?,
    onRetryAccount: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onEventClick: (Long) -> Unit,
    onSessionExpired: () -> Unit
) {
    if (session == null || session.signingOut) {
        AccountScreen(session, onRetryAccount, onSignIn, onSignOut)
        return
    }

    val viewModel: ProfileViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(state.sessionRejected) {
        if (state.sessionRejected) onSessionExpired()
    }
    ProfileScreen(session, state, onRetryAccount, onSignIn, onSignOut, viewModel::refresh, onEventClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreen(
    session: SessionUiState.Active,
    state: ProfileUiState,
    onRetryAccount: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onRetryAttendances: () -> Unit,
    onEventClick: (Long) -> Unit
) {
    var showUpcoming by rememberSaveable { mutableStateOf(true) }
    val events = if (showUpcoming) state.attendances?.upcoming else state.attendances?.past

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { AccountScreen(session, onRetryAccount, onSignIn, onSignOut, Modifier.fillMaxWidth()) }
        item {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.profile_attendances),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = showUpcoming,
                        onClick = { showUpcoming = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text(stringResource(R.string.profile_upcoming)) }
                    SegmentedButton(
                        selected = !showUpcoming,
                        onClick = { showUpcoming = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text(stringResource(R.string.profile_past)) }
                }
            }
        }
        if (state.loading) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.profile_attendances_loading))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
        if (state.failed) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.profile_attendances_error),
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(onClick = onRetryAttendances) { Text(stringResource(R.string.retry)) }
                }
            }
        }
        if (events != null) {
            if (events.isEmpty() && !state.loading && !state.failed) {
                item {
                    val message = if (showUpcoming) R.string.profile_upcoming_empty else R.string.profile_past_empty
                    Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(events, key = Event::id) { event -> EventCard(event, onEventClick) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileEmptyPreview() {
    EventiaTheme {
        ProfileScreen(
            session = SessionUiState.Active(),
            state = ProfileUiState(AttendanceGroups(emptyList(), emptyList()), loading = false),
            onRetryAccount = {},
            onSignIn = {},
            onSignOut = {},
            onRetryAttendances = {},
            onEventClick = {}
        )
    }
}
