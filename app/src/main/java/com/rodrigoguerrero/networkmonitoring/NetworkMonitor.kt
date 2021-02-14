package com.rodrigoguerrero.networkmonitoring

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkRequest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class NetworkMonitor(
    private val context: Context
) : LifecycleObserver {

    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private var connectivityManager: ConnectivityManager? = null
    private val validNetworks = HashSet<Network>()

    private lateinit var job: Job
    private lateinit var coroutineScope: CoroutineScope

    private val _networkAvailableStateFlow: MutableStateFlow<NetworkState> =
        MutableStateFlow(NetworkState.Available)
    val networkAvailableStateFlow
        get() = _networkAvailableStateFlow

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun registerNetworkCallback() {
        networkCallback = createNetworkCallback()
        job = Job()
        coroutineScope = CoroutineScope(Dispatchers.Default + job)
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)
        checkCurrentNetworkState()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun unregisterNetworkCallback() {
        validNetworks.clear()
        connectivityManager?.unregisterNetworkCallback(networkCallback)
        job.cancel()
    }

    private fun createNetworkCallback() = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            connectivityManager?.getNetworkCapabilities(network).also {
                if (it?.hasCapability(NET_CAPABILITY_INTERNET) == true) {
                    validNetworks.add(network)
                }
            }
            checkValidNetworks()
        }

        override fun onLost(network: Network) {
            validNetworks.remove(network)
            checkValidNetworks()
        }
    }

    private fun checkCurrentNetworkState() {
        connectivityManager?.allNetworks?.let {
            validNetworks.addAll(it)
        }
        checkValidNetworks()
    }

    private fun checkValidNetworks() {
        coroutineScope.launch {
            _networkAvailableStateFlow.emit(
                if (validNetworks.size > 0)
                    NetworkState.Available
                else
                    NetworkState.Unavailable
            )
        }
    }
}

sealed class NetworkState {
    object Unavailable : NetworkState()
    object Available : NetworkState()
}