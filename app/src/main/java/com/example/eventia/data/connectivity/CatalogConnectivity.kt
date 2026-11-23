package com.example.eventia.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

fun interface CatalogConnectivity {
    fun reconnects(): Flow<Unit>
}

class AndroidCatalogConnectivity @Inject constructor(
    @ApplicationContext context: Context
) : CatalogConnectivity {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    override fun reconnects(): Flow<Unit> = online.drop(1).filter { it }.map { Unit }

    val online: Flow<Boolean> = callbackFlow {
        val initialStateLock = Any()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                synchronized(initialStateLock) {
                    trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                }
            }

            override fun onLost(network: Network) {
                synchronized(initialStateLock) { trySend(false) }
            }
        }
        synchronized(initialStateLock) {
            manager.registerDefaultNetworkCallback(callback)
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
            trySend(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
        }
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
