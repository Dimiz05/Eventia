package com.example.eventia.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.eventia.R
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.screens.map.MapPoint
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

@Composable
internal fun DetailTopBar(onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(
            Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp).padding(start = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), stringResource(R.string.navigate_back))
            }
        }
    }
}

@Composable
internal fun DetailArtwork(imageUrl: String?, @DrawableRes fallbackIcon: Int, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.background(Brush.linearGradient(listOf(colors.primaryContainer, colors.surfaceContainerHighest))),
        contentAlignment = Alignment.Center
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = colors.surface) {
            Icon(
                painterResource(fallbackIcon), null,
                Modifier.padding(16.dp).size(28.dp), tint = colors.primary
            )
        }
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
internal fun DetailLocationMap(venue: Venue, point: MapPoint?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val available = remember(context, inspection) {
        !inspection && !context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        ).metaData?.getString("com.google.android.geo.API_KEY").isNullOrBlank() &&
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }
    val shape = RoundedCornerShape(12.dp)
    if (point != null && available) {
        key(point) {
            val location = LatLng(point.latitude, point.longitude)
            val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(location, 16f) }
            GoogleMap(
                modifier = modifier.clip(shape),
                cameraPositionState = camera,
                googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
                uiSettings = MapUiSettings(mapToolbarEnabled = false, zoomControlsEnabled = false)
            ) {
                Marker(state = rememberUpdatedMarkerState(location), title = venue.name)
            }
        }
    } else {
        Surface(modifier, shape = shape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painterResource(R.drawable.ic_map), null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        stringResource(R.string.venue_map_preview_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
internal fun rememberVenueDirections(venue: Venue, point: MapPoint?): () -> Unit {
    val context = LocalContext.current
    return remember(venue.name, point, context) {
        {
            if (point != null) {
                val coordinates = "${point.latitude},${point.longitude}"
                val query = Uri.encode("$coordinates(${venue.name})")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$coordinates?q=$query"))
                try {
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.venue_directions_unavailable, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
