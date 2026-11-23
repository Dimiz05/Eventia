package com.example.eventia.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.eventia.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest

internal enum class ConnectionNotice { Offline, Restored }

@OptIn(ExperimentalCoroutinesApi::class)
internal fun Flow<Boolean>.connectionNotices(): Flow<ConnectionNotice?> =
    flow {
        var wasOffline = false
        distinctUntilChanged().transformLatest { online ->
            val restored = online && wasOffline
            wasOffline = !online
            when {
                !online -> emit(ConnectionNotice.Offline)
                restored -> {
                    emit(ConnectionNotice.Restored)
                    delay(3_000)
                    emit(null)
                }
                else -> emit(null)
            }
        }.collect { emit(it) }
    }

@Composable
internal fun ConnectionBanner(notice: ConnectionNotice) {
    val colors = MaterialTheme.colorScheme
    val offline = notice == ConnectionNotice.Offline
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = if (offline) colors.surfaceVariant else colors.primaryContainer,
        contentColor = if (offline) colors.onSurfaceVariant else colors.onPrimaryContainer
    ) {
        Text(
            text = stringResource(
                if (offline) R.string.connection_offline else R.string.catalog_connection_restored
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
