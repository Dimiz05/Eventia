package com.example.eventia.ui.screens.venue

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.eventia.R
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.screens.map.MapPoint

@Composable
internal fun VenueLocation(
    venue: Venue,
    additionalAction: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val point = MapPoint.from(venue.latitude, venue.longitude)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            when {
                venue.address.isNotBlank() -> venue.address
                point != null -> stringResource(
                    R.string.venue_location_coordinates,
                    point.latitude.toString(),
                    point.longitude.toString()
                )
                else -> stringResource(R.string.venue_address_unavailable)
            }
        )
        if (point != null || additionalAction != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (point != null) {
                    OutlinedButton(onClick = {
                        val coordinates = "${point.latitude},${point.longitude}"
                        val query = Uri.encode("$coordinates(${venue.name})")
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$coordinates?q=$query"))
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(
                                context, R.string.venue_directions_unavailable, Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Text(stringResource(R.string.venue_directions))
                    }
                }
                additionalAction?.invoke()
            }
        }
    }
}
