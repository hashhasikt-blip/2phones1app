package com.twophones.callforward.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.twophones.callforward.bluetooth.BluetoothManager

/**
 * Service to manage Bluetooth connection lifecycle
 */
class BluetoothConnectionService : Service() {

    companion object {
        private const val TAG = "BluetoothConnectionService"
    }

    private lateinit var bluetoothManager: BluetoothManager

    override fun onCreate() {
        super.onCreate()
        bluetoothManager = BluetoothManager(this)
        Log.d(TAG, "Bluetooth connection service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val deviceAddress = intent?.getStringExtra("device_address")

        when (action) {
            "connect" -> {
                deviceAddress?.let {
                    val device = bluetoothManager.getAvailablePairedDevices()
                        .find { d -> d.address == deviceAddress }
                    if (device != null) {
                        bluetoothManager.connectToDevice(device)
                        Log.d(TAG, "Connecting to device: $deviceAddress")
                    }
                }
            }

            "disconnect" -> {
                bluetoothManager.disconnect()
                Log.d(TAG, "Disconnecting from device")
                stopSelf()
            }

            "discover" -> {
                bluetoothManager.startDiscovery()
                Log.d(TAG, "Starting Bluetooth discovery")
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.disconnect()
        Log.d(TAG, "Bluetooth connection service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
