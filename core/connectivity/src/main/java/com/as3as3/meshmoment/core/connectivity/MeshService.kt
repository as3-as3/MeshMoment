package com.as3as3.meshmoment.core.connectivity

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.as3as3.meshmoment.core.security.CryptoManager
import com.as3as3.meshmoment.core.security.IdentityManager

class MeshService : LifecycleService() {

    private lateinit var meshManager: MeshManager
    private val binder = MeshBinder()

    inner class MeshBinder : Binder() {
        fun getMeshManager(): MeshManager = meshManager
    }

    override fun onCreate() {
        super.onCreate()
        val identityManager = IdentityManager()
        val cryptoManager = CryptoManager()
        meshManager = MeshManager(this, identityManager, cryptoManager)
        
        createNotificationChannel()
        
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(1, notification, type)
        } else {
            startForeground(1, notification)
        }
        
        meshManager.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        meshManager.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "mesh_service",
                "Mesh Network Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "mesh_service")
            .setContentTitle("MeshMoment Active")
            .setContentText("Maintaining mesh connectivity and PTT...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
