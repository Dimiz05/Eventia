package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.eventia.R
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

@Composable
internal fun CachedDataNotice(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
internal fun cachedCatalogNotice(downloadedAt: Instant): String {
    val now by produceState(Instant.now(), downloadedAt) {
        while (true) {
            value = Instant.now()
            delay(30_000)
        }
    }
    val minutes = Duration.between(downloadedAt, now).toMinutes().coerceAtLeast(0)
    val days = (minutes / 1_440).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.catalog_offline_recent)
        minutes < 60 -> stringResource(R.string.catalog_offline_minutes, minutes)
        minutes < 1_440 -> stringResource(R.string.catalog_offline_hours, minutes / 60)
        else -> pluralStringResource(R.plurals.catalog_offline_days, days, days)
    }
}
