package com.as3as3.meshmoment.core.connectivity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.aware.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiAwareManager(private val context: Context) {
    private val systemWifiAwareManager = context.getSystemService(Context.WIFI_AWARE_SERVICE) as? android.net.wifi.aware.WifiAwareManager
    private var awareSession: WifiAwareSession? = null
    private var currentSession: DiscoverySession? = null
    private var peerHandle: PeerHandle? = null

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable = _isAvailable.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<PeerHandle>>(emptyList())
    val connectedPeers = _connectedPeers.asStateFlow()

    init {
        checkAvailability()
    }

    private fun checkAvailability() {
        val hasFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)
        _isAvailable.value = hasFeature && systemWifiAwareManager != null
    }

    fun startRoom(room: LocalRoom) {
        val manager = systemWifiAwareManager ?: return
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("WifiAware", "Location permission missing")
            return
        }

        manager.attach(object : AttachCallback() {
            override fun onAttached(session: WifiAwareSession) {
                awareSession = session
                val config = PublishConfig.Builder()
                    .setServiceName("MeshRoom_${room.id}")
                    .build()

                session.publish(config, object : DiscoverySessionCallback() {
                    override fun onPublishStarted(publishSession: PublishDiscoverySession) {
                        currentSession = publishSession
                        Log.d("WifiAware", "Room published: ${room.name}")
                    }

                    override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                        Log.d("WifiAware", "Join request received from peer")
                        // Data path establishment would go here
                    }
                }, Handler(Looper.getMainLooper()))
            }
            
            override fun onAttachFailed() {
                Log.e("WifiAware", "Attach failed")
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun discoverRooms(onRoomFound: (String) -> Unit) {
        val manager = systemWifiAwareManager ?: return
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("WifiAware", "Location permission missing")
            return
        }

        manager.attach(object : AttachCallback() {
            override fun onAttached(session: WifiAwareSession) {
                awareSession = session
                val config = SubscribeConfig.Builder()
                    .setServiceName("MeshRoom_")
                    .build()

                session.subscribe(config, object : DiscoverySessionCallback() {
                    override fun onSubscribeStarted(subscribeSession: SubscribeDiscoverySession) {
                        currentSession = subscribeSession
                    }

                    override fun onServiceDiscovered(peerHandle: PeerHandle, serviceSpecificInfo: ByteArray?, matchFilter: List<ByteArray>?) {
                        this@WifiAwareManager.peerHandle = peerHandle
                        onRoomFound("Room Found")
                    }
                }, Handler(Looper.getMainLooper()))
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun stop() {
        currentSession?.close()
        awareSession?.close()
        currentSession = null
        awareSession = null
    }
}
