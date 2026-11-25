package com.example.eventia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.eventia.R
import com.example.eventia.domain.model.Event
import java.util.Date
import android.text.format.DateFormat

@Composable
fun EventCard(
    event: Event,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    favoriteAction: (@Composable () -> Unit)? = null
) {
    OutlinedCard(
        onClick = { onEventClick(event.id) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EventArtwork(event.imageUrl, Modifier.size(88.dp).clip(RoundedCornerShape(10.dp)))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EventCategoryLabel(event.category.name)
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                Text(eventLocalDateTime(event), style = MaterialTheme.typography.bodyMedium)
                Text(event.venue.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    event.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(R.string.available_spots, event.availableSpots),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            favoriteAction?.invoke()
        }
    }
}

@Composable
internal fun EventCategoryLabel(name: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
internal fun EventArtwork(
    imageUrl: String?,
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
) {
    val substitute = painterResource(R.drawable.ic_event_placeholder)
    AsyncImage(
        model = imageUrl?.takeIf { it.isNotBlank() },
        contentDescription = null,
        placeholder = substitute,
        fallback = substitute,
        error = substitute,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

@Composable
internal fun eventLocalDateTime(event: Event): String {
    val context = LocalContext.current
    val start = Date.from(event.startsAt.toInstant())
    return stringResource(
        R.string.event_date_time,
        DateFormat.getMediumDateFormat(context).format(start),
        DateFormat.getTimeFormat(context).format(start)
    )
}
