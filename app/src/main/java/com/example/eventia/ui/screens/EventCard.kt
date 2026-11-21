package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
fun EventCard(event: Event, onEventClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(onClick = { onEventClick(event.id) }, modifier = modifier.fillMaxWidth()) {
        EventArtwork(event.imageUrl)
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text(event.category.name, style = MaterialTheme.typography.labelLarge)
            Text(eventLocalDateTime(event))
            Text(event.venue.name)
            Text(event.summary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.available_spots, event.availableSpots))
        }
    }
}

@Composable
internal fun EventArtwork(imageUrl: String?) {
    val substitute = painterResource(R.drawable.ic_event_placeholder)
    AsyncImage(
        model = imageUrl?.takeIf { it.isNotBlank() },
        contentDescription = null,
        placeholder = substitute,
        fallback = substitute,
        error = substitute,
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
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
