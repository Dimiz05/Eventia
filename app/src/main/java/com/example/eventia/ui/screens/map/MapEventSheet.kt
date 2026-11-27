package com.example.eventia.ui.screens.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.eventia.R
import com.example.eventia.domain.model.Event
import com.example.eventia.ui.components.EventArtwork
import com.example.eventia.ui.components.eventLocalDateTime
import com.example.eventia.ui.components.rememberVenueDirections
import com.example.eventia.ui.screens.explore.ExploreBadge

@Composable
internal fun MapEventSheet(
    events: List<Event>,
    onDismiss: () -> Unit,
    onEventClick: (Long) -> Unit,
    favoriteAction: (@Composable (Event) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp)
                .width(32.dp).height(4.dp).background(colors.outlineVariant, RoundedCornerShape(50))
        )
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.map_shared_point),
                    Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ExploreBadge(
                    stringResource(R.string.venue_list_count, events.size),
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(painterResource(R.drawable.ic_catalog_close), stringResource(R.string.map_close_event))
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(events, key = { it.id }) { event ->
                MapEventSummary(event, onEventClick = { onEventClick(event.id) }, favoriteAction = favoriteAction)
            }
        }
    }
}

@Composable
private fun MapEventSummary(event: Event, onEventClick: () -> Unit, favoriteAction: (@Composable (Event) -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val point = MapPoint.from(event.venue.latitude, event.venue.longitude)
    val directions = rememberVenueDirections(event.venue, point)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceContainer) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EventArtwork(event.imageUrl, Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    MapEventLine(R.drawable.ic_catalog_clock, eventLocalDateTime(event))
                    MapEventLine(R.drawable.ic_catalog_pin, event.venue.name)
                    ExploreBadge(
                        label = if (event.availableSpots <= 0) stringResource(R.string.catalog_sold_out)
                        else pluralStringResource(
                            R.plurals.catalog_spots_available, event.availableSpots, event.availableSpots
                        ),
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer
                    )
                }
                favoriteAction?.invoke(event)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = directions,
                enabled = point != null,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                border = BorderStroke(1.dp, if (point == null) colors.outlineVariant else colors.primary)
            ) { Text(stringResource(R.string.venue_directions)) }
            Button(
                onClick = onEventClick,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text(stringResource(R.string.map_view_detail)) }
        }
    }
}

@Composable
private fun MapEventLine(icon: Int, label: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(painterResource(icon), null, Modifier.size(15.dp), tint = colors.primary)
        Text(label, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}
