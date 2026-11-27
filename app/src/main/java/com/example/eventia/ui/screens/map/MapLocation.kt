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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

@Composable
internal fun rememberMapLocation(onPosition: (MapPoint) -> Unit): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var permitted by remember(context) { mutableStateOf(context.canLocate()) }
    var locating by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permitted = context.canLocate() }

    val center: () -> Unit = {
        if (!locating) {
            locating = true
            scope.launch {
                try {
                    val manager = context.getSystemService(LocationManager::class.java)
                    if (!manager.isLocationEnabled) {
                        Toast.makeText(context, R.string.map_location_disabled, Toast.LENGTH_SHORT).show()
                    } else {
                        val point = context.currentMapPoint(manager)
                        permitted = context.canLocate()
                        if (point == null) {
                            Toast.makeText(context, R.string.map_location_unavailable, Toast.LENGTH_SHORT).show()
                        } else {
                            onPosition(point)
                        }
                    }
                } finally {
                    locating = false
                }
            }
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permitted = context.canLocate()
        if (permitted) center()
        else Toast.makeText(context, R.string.map_location_denied, Toast.LENGTH_SHORT).show()
    }
    return permitted to {
        permitted = context.canLocate()
        if (permitted) center()
        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}

private fun Context.hasLocationPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.canLocate(): Boolean =
    hasLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
        hasLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)

private suspend fun Context.currentMapPoint(manager: LocationManager): MapPoint? {
    val precise = hasLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    val providers = if (precise) {
        listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    } else {
        listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
    }
    val enabled = manager.getProviders(true)
    val provider = providers.firstOrNull(enabled::contains) ?: return null
    val location = withTimeoutOrNull(8_000L) {
        suspendCancellableCoroutine<Location?> { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                manager.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(this@currentMapPoint)) {
                    continuation.resume(it)
                }
            } catch (_: SecurityException) {
                continuation.resume(null)
            } catch (_: IllegalArgumentException) {
                continuation.resume(null)
            }
        }
    }
    return MapPoint.from(location?.latitude, location?.longitude)
}
