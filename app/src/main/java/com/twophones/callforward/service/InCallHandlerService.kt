package com.twophones.callforward.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.twophones.callforward.audio.AudioRouting
import com.twophones.callforward.bluetooth.BluetoothManager
import com.twophones.callforward.bluetooth.BluetoothMessage
import android.media.AudioManager

/**
 * InCall Service to handle call-related operations
 * This is used to intercept and handle in-call operations
 */
class InCallHandlerService : InCallService() {

    companion object {
        private const val TAG = "InCallHandlerService"
    }

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var audioRouting: AudioRouting

    override fun onCreate() {
        super.onCreate()
        bluetoothManager = BluetoothManager(this)
        audioRouting = AudioRouting(getSystemService(AUDIO_SERVICE) as AudioManager)
        Log.d(TAG, "InCall service created")
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call added: ${call.details.handle}")

        // Send call information via Bluetooth
        val phoneNumber = call.details.handle?.schemeSpecificPart ?: "Unknown"
        val message = BluetoothMessage(
            type = "call_state",
            data = "added:$phoneNumber"
        )
        bluetoothManager.sendMessage(message)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed")

        val message = BluetoothMessage(
            type = "call_state",
            data = "removed"
        )
        bluetoothManager.sendMessage(message)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRouting.cleanup()
        Log.d(TAG, "InCall service destroyed")
    }
}
