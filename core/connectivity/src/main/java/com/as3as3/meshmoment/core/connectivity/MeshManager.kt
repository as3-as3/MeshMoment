package com.as3as3.meshmoment.core.connectivity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.as3as3.meshmoment.core.security.CryptoManager
import com.as3as3.meshmoment.core.security.IdentityManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class MeshManager(
    private val context: Context,
    val identityManager: IdentityManager,
    private val cryptoManager: CryptoManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val advertiser = BleAdvertiserManager(context)
    private val scanner = BleScannerManager(context)
    private var gattServer: BleGattServerManager? = null
    private val audioManager = AudioStreamManager()
    private val wifiTransport = WifiAwareTransport(context)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val messageAdapter = moshi.adapter(MeshMessage::class.java)

    private val _receivedMessages = MutableSharedFlow<MeshMessage>()
    val receivedMessages: SharedFlow<MeshMessage> = _receivedMessages.asSharedFlow()

    private val _activeRoom = MutableStateFlow<LocalRoom?>(null)
    val activeRoom = _activeRoom

    private val meshKey = "mesh-moment-shared-secret-key-2025"
    private val processedMessageIds = mutableSetOf<String>()

    companion object {
        val APP_SERVICE_UUID: UUID = UUID.fromString("f052111a-1111-4444-8888-999999999999")
        val MESSAGE_CHAR_UUID: UUID = UUID.fromString("f052111a-1111-4444-8888-999999999998")
    }

    fun start() {
        gattServer = BleGattServerManager(
            context, 
            APP_SERVICE_UUID, 
            MESSAGE_CHAR_UUID, 
            onMessageReceived = { encryptedMessage ->
                scope.launch {
                    handleIncomingRawMessage(encryptedMessage)
                }
            }
        )
        gattServer?.startServer()
        
        scope.launch {
            identityManager.currentIdentity.collectLatest { identity ->
                advertiser.stopAdvertising()
                advertiser.startAdvertising(APP_SERVICE_UUID)
            }
        }
        
        scanner.startScanning(APP_SERVICE_UUID)

        scope.launch {
            audioManager.audioChunks.collect { chunk ->
                broadcastAudioChunk(chunk)
            }
        }
    }

    private suspend fun handleIncomingRawMessage(encryptedData: String) {
        try {
            val decrypted = cryptoManager.decrypt(encryptedData, meshKey)
            val meshMessage = messageAdapter.fromJson(decrypted)
            
            if (meshMessage != null && !processedMessageIds.contains(meshMessage.id)) {
                processedMessageIds.add(meshMessage.id)
                if (processedMessageIds.size > 1000) processedMessageIds.remove(processedMessageIds.first())
                
                when (meshMessage.type) {
                    MessageType.AUDIO -> {
                        if (meshMessage.audioData != null) {
                            audioManager.playAudioChunk(meshMessage.audioData)
                        }
                    }
                    MessageType.ROOM_ADVERTISEMENT -> {
                        // Notify UI about discovered room via BLE
                        _receivedMessages.emit(meshMessage)
                    }
                    MessageType.JOIN_REQUEST -> {
                        // Handle join request (simplified: auto-accept if password matches)
                        // In real app, show dialog to user
                    }
                    else -> {
                        _receivedMessages.emit(meshMessage)
                    }
                }
                
                if (meshMessage.ttl > 0) {
                    relayMessage(meshMessage.copy(ttl = meshMessage.ttl - 1))
                }
            }
        } catch (e: Exception) {
            Log.e("MeshManager", "Failed to process incoming message", e)
        }
    }

    fun createRoom(name: String, password: String) {
        val room = LocalRoom(
            id = UUID.randomUUID().toString().take(8),
            name = name,
            password = password,
            ownerId = identityManager.currentIdentity.value.toString()
        )
        _activeRoom.value = room
        wifiTransport.startPublishing(room)
        
        // Advertise room via BLE Mesh so others can see it
        val advertisement = MeshMessage(
            senderId = room.ownerId,
            roomInfo = room,
            type = MessageType.ROOM_ADVERTISEMENT
        )
        relayMessage(advertisement)
    }

    fun joinRoom(room: LocalRoom) {
        _activeRoom.value = room
        wifiTransport.subscribeToRoom(room.id, room.password)
    }

    private fun relayMessage(message: MeshMessage) {
        val devices = scanner.foundDevices.value
        devices.forEach { result ->
            sendMessageToDevice(result.device, message)
        }
    }

    fun stop() {
        advertiser.stopAdvertising()
        scanner.stopScanning()
        gattServer?.stopServer()
        audioManager.release()
        wifiTransport.stop()
    }
    
    val foundDevices = scanner.foundDevices

    fun broadcastMessage(content: String) {
        val message = MeshMessage(
            senderId = identityManager.currentIdentity.value.toString(),
            content = content,
            type = MessageType.TEXT
        )
        processedMessageIds.add(message.id)
        relayMessage(message)
    }

    private fun broadcastAudioChunk(audioData: String) {
        val message = MeshMessage(
            senderId = identityManager.currentIdentity.value.toString(),
            audioData = audioData,
            type = MessageType.AUDIO,
            ttl = 1 
        )
        // If Wi-Fi Aware is active, we could send high-quality audio here
        // For now, we still use the BLE Mesh relay
        processedMessageIds.add(message.id)
        relayMessage(message)
    }

    fun startPtt() {
        audioManager.startRecording()
    }

    fun stopPtt() {
        audioManager.stopRecording()
    }

    @SuppressLint("MissingPermission")
    private fun sendMessageToDevice(device: BluetoothDevice, message: MeshMessage) {
        val jsonMessage = messageAdapter.toJson(message)
        val encryptedMessage = try {
            cryptoManager.encrypt(jsonMessage, meshKey)
        } catch (e: Exception) {
            Log.e("MeshManager", "Encryption failed", e)
            return
        }

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(APP_SERVICE_UUID)
                    val characteristic = service?.getCharacteristic(MESSAGE_CHAR_UUID)
                    if (characteristic != null) {
                        characteristic.value = encryptedMessage.toByteArray()
                        gatt.writeCharacteristic(characteristic)
                    }
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                gatt.disconnect()
            }
        }
        device.connectGatt(context, false, gattCallback)
    }
}
