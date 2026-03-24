package com.as3as3.meshmoment.core.connectivity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.aware.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiAwareTransport(private val context: Context) {
    private val systemWifiAwareManager = context.getSystemService(Context.WIFI_AWARE_SERVICE) as? android.net.wifi.aware.WifiAwareManager
    private var awareSession: WifiAwareSession? = null
    private var currentDiscoverySession: DiscoverySession? = null
    
    private val _isAvailable = MutableStateFlow(false)
    val isAvailable = _isAvailable.asStateFlow()

    private val _networkActive = MutableStateFlow(false)
    val networkActive = _networkActive.asStateFlow()

    init {
        checkAvailability()
    }

    private fun checkAvailability() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
            _isAvailable.value = systemWifiAwareManager?.isAvailable ?: false
        }
    }

    fun startPublishing(room: LocalRoom) {
        if (!hasPermissions() || systemWifiAwareManager == null) return
        
        systemWifiAwareManager.attach(object : AttachCallback() {
            override fun onAttached(session: WifiAwareSession) {
                awareSession = session
                val config = PublishConfig.Builder()
                    .setServiceName("MeshMoment_${room.id}")
                    .build()

                session.publish(config, object : DiscoverySessionCallback() {
                    override fun onPublishStarted(publishSession: PublishDiscoverySession) {
                        currentDiscoverySession = publishSession
                        Log.d("WifiAware", "Room published: ${room.name}")
                    }

                    override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                        val msgStr = String(message)
                        Log.d("WifiAware", "Message from peer: $msgStr")
                        if (msgStr == "JOIN") {
                            requestDataPath(peerHandle, room.password, isServer = true)
                        }
                    }
                }, Handler(Looper.getMainLooper()))
            }
            override fun onAttachFailed() {
                Log.e("WifiAware", "Attach failed")
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun subscribeToRoom(roomId: String, password: String) {
        if (!hasPermissions() || systemWifiAwareManager == null) return

        systemWifiAwareManager.attach(object : AttachCallback() {
            override fun onAttached(session: WifiAwareSession) {
                awareSession = session
                val config = SubscribeConfig.Builder()
                    .setServiceName("MeshMoment_$roomId")
                    .build()

                session.subscribe(config, object : DiscoverySessionCallback() {
                    override fun onSubscribeStarted(subscribeSession: SubscribeDiscoverySession) {
                        currentDiscoverySession = subscribeSession
                    }

                    override fun onServiceDiscovered(peerHandle: PeerHandle, serviceSpecificInfo: ByteArray?, matchFilter: List<ByteArray>?) {
                        Log.d("WifiAware", "Service discovered, sending join request")
                        currentDiscoverySession?.sendMessage(peerHandle, 0, "JOIN".toByteArray())
                        requestDataPath(peerHandle, password, isServer = false)
                    }
                }, Handler(Looper.getMainLooper()))
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun requestDataPath(peerHandle: PeerHandle, password: String, isServer: Boolean) {
        val session = currentDiscoverySession ?: return
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val networkSpecifier = WifiAwareNetworkSpecifier.Builder(session, peerHandle)
            .setPskPassphrase(password)
            .build()

        val myNetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            .setNetworkSpecifier(networkSpecifier)
            .build()

        connectivityManager.requestNetwork(myNetworkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("WifiAware", "Data path available!")
                _networkActive.value = true
            }

            override fun onLost(network: Network) {
                _networkActive.value = false
            }
        })
    }

    private fun hasPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun stop() {
        currentDiscoverySession?.close()
        awareSession?.close()
        currentDiscoverySession = null
        awareSession = null
        _networkActive.value = false
    }
}
