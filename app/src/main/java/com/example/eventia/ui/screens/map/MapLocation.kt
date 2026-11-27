package com.example.eventia.ui.screens.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.eventia.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import java.util.Locale

internal data class MapUserLocation(val point: MapPoint, val precise: Boolean)

internal fun MapUserLocation.distanceTo(point: MapPoint): Float {
    val result = FloatArray(1)
    Location.distanceBetween(this.point.latitude, this.point.longitude, point.latitude, point.longitude, result)
    return result[0]
}

internal fun formatMapDistance(meters: Float): String =
    if (meters < 1000f) "${meters.roundToInt().coerceAtMost(999)} m"
    else String.format(Locale.forLanguageTag("es-BO"), "%.1f km", meters / 1000f)

@Composable
internal fun rememberMapLocation(
    onAccessChanged: (Boolean?) -> Unit,
    onPosition: (MapUserLocation) -> Unit
): Pair<Boolean?, () -> Unit> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember(context) { context.getSystemService(LocationManager::class.java) }
    var precision by remember(context) { mutableStateOf(context.locationPrecision()) }
    var request by remember { mutableStateOf<Job?>(null) }
    val refreshAccess = {
        precision = context.locationPrecision()
        onAccessChanged(precision.takeIf { manager.isLocationEnabled })
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshAccess() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { request?.cancel() }

    val center: () -> Unit = {
        if (request?.isActive != true) {
            request = scope.launch {
                val requestedPrecision = precision
                if (!manager.isLocationEnabled) {
                    Toast.makeText(context, R.string.map_location_disabled, Toast.LENGTH_SHORT).show()
                } else if (requestedPrecision != null) {
                    val point = context.currentMapPoint(manager, requestedPrecision)
                    refreshAccess()
                    if (point != null && precision == requestedPrecision && manager.isLocationEnabled) {
                        onPosition(MapUserLocation(point, requestedPrecision))
                    } else {
                        onAccessChanged(null)
                        val message = if (manager.isLocationEnabled) R.string.map_location_unavailable
                        else R.string.map_location_disabled
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshAccess()
        if (precision != null) center()
        else Toast.makeText(context, R.string.map_location_denied, Toast.LENGTH_SHORT).show()
    }
    return precision to {
        refreshAccess()
        if (precision != null) center()
        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}

private fun Context.hasLocationPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.locationPrecision(): Boolean? = when {
    hasLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION) -> true
    hasLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION) -> false
    else -> null
}

private suspend fun Context.currentMapPoint(manager: LocationManager, precise: Boolean): MapPoint? {
    val providers = if (precise) {
        listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    } else {
        listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
    }
    val enabled = providers.filter(manager.getProviders(true)::contains)
    if (enabled.isEmpty()) return null
    return withTimeoutOrNull(8_000L) {
        suspendCancellableCoroutine { continuation ->
            val requests = enabled.map { it to CancellationSignal() }
            val cancelRequests = { requests.forEach { (_, signal) -> signal.cancel() } }
            continuation.invokeOnCancellation { cancelRequests() }
            var remaining = enabled.size
            val onLocation: (Location?) -> Unit = { location ->
                val point = MapPoint.from(location?.latitude, location?.longitude)
                remaining--
                if (continuation.isActive && (point != null || remaining == 0)) {
                    continuation.resume(point)
                    cancelRequests()
                }
            }
            val executor = ContextCompat.getMainExecutor(this@currentMapPoint)
            for ((provider, signal) in requests) {
                try {
                    manager.getCurrentLocation(provider, signal, executor, onLocation)
                } catch (_: SecurityException) {
                    onLocation(null)
                } catch (_: IllegalArgumentException) {
                    onLocation(null)
                }
            }
        }
    }
}
