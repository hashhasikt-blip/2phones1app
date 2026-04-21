package com.twophones.callforward.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.*

data class BluetoothMessage(
    val type: String,  // "call", "contact", "audio", "status"
    val data: String,
    val timestamp: Long = System.currentTimeMillis()
)

@SuppressLint("MissingPermission")
class BluetoothManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothManager"
        private val UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        private const val BUFFER_SIZE = 1024
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _receivedMessages = MutableStateFlow<BluetoothMessage?>(null)
    val receivedMessages: StateFlow<BluetoothMessage?> = _receivedMessages

    private val _availableDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val availableDevices: StateFlow<List<BluetoothDevice>> = _availableDevices

    enum class ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    init {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported on this device")
        }
    }

    fun getAvailablePairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun startDiscovery() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }
            bluetoothAdapter?.startDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discovery", e)
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        Thread {
            try {
                _connectionStatus.value = ConnectionStatus.CONNECTING
                
                bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_SPP)
                bluetoothSocket?.connect()
                
                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream
                
                _connectionStatus.value = ConnectionStatus.CONNECTED
                Log.d(TAG, "Connected to ${device.name}")
                
                startListeningForMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Connection error", e)
                _connectionStatus.value = ConnectionStatus.ERROR
                disconnect()
            }
        }.start()
    }

    private fun startListeningForMessages() {
        Thread {
            val buffer = ByteArray(BUFFER_SIZE)
            var bytes: Int
            
            try {
                while (bluetoothSocket?.isConnected == true) {
                    bytes = inputStream?.read(buffer) ?: -1
                    
                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes)
                        Log.d(TAG, "Received: $message")
                        parseAndHandleMessage(message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading from socket", e)
            }
        }.start()
    }

    private fun parseAndHandleMessage(messageStr: String) {
        try {
            val parts = messageStr.split("|")
            if (parts.size >= 2) {
                val btMessage = BluetoothMessage(
                    type = parts[0],
                    data = parts[1]
                )
                _receivedMessages.value = btMessage
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }

    fun sendMessage(message: BluetoothMessage) {
        try {
            val messageStr = "${message.type}|${message.data}"
            outputStream?.write(messageStr.toByteArray())
            outputStream?.flush()
            Log.d(TAG, "Sent: $messageStr")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
        }
    }

    fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            Log.d(TAG, "Disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }

    fun isConnected(): Boolean = bluetoothSocket?.isConnected == true
}
