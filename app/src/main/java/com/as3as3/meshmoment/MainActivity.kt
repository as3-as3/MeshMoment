package com.as3as3.meshmoment

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.as3as3.meshmoment.core.connectivity.MeshManager
import com.as3as3.meshmoment.core.connectivity.MeshService
import com.as3as3.meshmoment.data.local.AppDatabase
import com.as3as3.meshmoment.data.local.MessageEntity
import com.as3as3.meshmoment.data.local.MessageRepository
import com.as3as3.meshmoment.feature.chat.ChatScreen
import com.as3as3.meshmoment.feature.chat.ChatViewModel
import com.as3as3.meshmoment.feature.chat.ChatViewModelFactory
import com.as3as3.meshmoment.feature.identity.IdentityScreen
import com.as3as3.meshmoment.feature.identity.IdentityViewModel
import com.as3as3.meshmoment.feature.identity.IdentityViewModelFactory
import com.as3as3.meshmoment.feature.morse.MorseScreen
import com.as3as3.meshmoment.feature.morse.MorseViewModel
import com.as3as3.meshmoment.feature.morse.MorseViewModelFactory
import com.as3as3.meshmoment.feature.radar.RadarScreen
import com.as3as3.meshmoment.feature.radar.RadarViewModel
import com.as3as3.meshmoment.feature.radar.RadarViewModelFactory
import com.as3as3.meshmoment.ui.theme.MeshMomentTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val meshManager = mutableStateOf<MeshManager?>(null)
    private lateinit var repository: MessageRepository
    private var isBound = false
    private var serviceStartedAndBinding = false
    private var isPttActive = mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MeshService.MeshBinder
            val manager = binder.getMeshManager()
            meshManager.value = manager
            isBound = true
            
            lifecycleScope.launch {
                manager.receivedMessages.collectLatest { meshMessage ->
                    repository.insert(
                        MessageEntity(
                            senderId = meshMessage.senderId,
                            content = meshMessage.content ?: "",
                            isSent = false,
                            timestamp = meshMessage.timestamp
                        )
                    )
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            meshManager.value = null
            isBound = false
            serviceStartedAndBinding = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "mesh-moment-db"
        ).fallbackToDestructiveMigration().build()
        repository = MessageRepository(db.messageDao())
        
        if (arePermissionsGranted()) {
            startMeshService()
        }

        setContent {
            MeshMomentTheme {
                val navController = rememberNavController()
                val currentMeshManager by remember { meshManager }
                val pttActive by remember { isPttActive }

                val permissionsToRequest = remember {
                    val list = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        list.add(Manifest.permission.BLUETOOTH_SCAN)
                        list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                        list.add(Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        list.add(Manifest.permission.BLUETOOTH)
                        list.add(Manifest.permission.BLUETOOTH_ADMIN)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    list.toTypedArray()
                }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    if (permissions.values.all { it }) {
                        startMeshService()
                    }
                }

                LaunchedEffect(Unit) {
                    if (!arePermissionsGranted()) {
                        launcher.launch(permissionsToRequest)
                    }
                }

                if (currentMeshManager == null) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Connecting to Mesh...")
                            }
                        }
                    }
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentRoute = navBackStackEntry?.destination?.route

                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Radar, contentDescription = "Radar") },
                                    label = { Text("Radar") },
                                    selected = currentRoute == "radar",
                                    onClick = { navController.navigate("radar") { launchSingleTop = true } }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                                    label = { Text("Chat") },
                                    selected = currentRoute == "chat",
                                    onClick = { navController.navigate("chat") { launchSingleTop = true } }
                                )
                                NavigationBarItem(
                                    icon = { 
                                        Box {
                                            Icon(Icons.Default.Mic, contentDescription = "PTT")
                                            if (pttActive) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    },
                                    label = { Text("PTT") },
                                    selected = false,
                                    onClick = { /* Intercepted by hardware buttons */ }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Person, contentDescription = "Identity") },
                                    label = { Text("Identity") },
                                    selected = currentRoute == "identity",
                                    onClick = { navController.navigate("identity") { launchSingleTop = true } }
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(navController, "radar", Modifier.padding(innerPadding)) {
                            composable("radar") {
                                RadarScreen(viewModel(factory = RadarViewModelFactory(currentMeshManager!!)))
                            }
                            composable("chat") {
                                ChatScreen(viewModel(factory = ChatViewModelFactory(repository, currentMeshManager!!)))
                            }
                            composable("identity") {
                                IdentityScreen(viewModel(factory = IdentityViewModelFactory(currentMeshManager!!.identityManager)))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (!isPttActive.value) {
                isPttActive.value = true
                meshManager.value?.startPtt()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            isPttActive.value = false
            meshManager.value?.stopPtt()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun arePermissionsGranted(): Boolean {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.addAll(listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT))
        }
        return permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun startMeshService() {
        if (serviceStartedAndBinding) return
        serviceStartedAndBinding = true
        Intent(this, MeshService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) unbindService(connection)
    }
}
