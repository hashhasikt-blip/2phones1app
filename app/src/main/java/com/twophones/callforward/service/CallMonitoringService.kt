package com.twophones.callforward.service

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.util.Log
import com.twophones.callforward.audio.AudioRouting
import com.twophones.callforward.bluetooth.BluetoothManager
import com.twophones.callforward.bluetooth.BluetoothMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallMonitoringService : Service() {

    companion object {
        private const val TAG = "CallMonitoringService"
    }

    private lateinit var audioRouting: AudioRouting
    private lateinit var bluetoothManager: BluetoothManager
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        audioRouting = AudioRouting(getSystemService(AUDIO_SERVICE) as AudioManager)
        bluetoothManager = BluetoothManager(this)
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra("phone_number") ?: ""
        val callType = intent?.getStringExtra("call_type") ?: "unknown"

        Log.d(TAG, "Call monitoring started: $phoneNumber ($callType)")

        serviceScope.launch {
            // Listen for Bluetooth messages during call
            bluetoothManager.receivedMessages.collect { message ->
                message?.let {
                    handleBluetoothMessage(it)
                }
            }
        }

        return START_STICKY
    }

    private fun handleBluetoothMessage(message: BluetoothMessage) {
        when (message.type) {
            "audio_data" -> {
                // Receive audio from Bluetooth device and play it
                try {
                    val audioData = android.util.Base64.decode(message.data, android.util.Base64.DEFAULT)
                    audioRouting.startPlayback(audioData)
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing audio", e)
                }
            }

            "call_ended" -> {
                Log.d(TAG, "Call ended via Bluetooth notification")
                stopSelf()
            }

            "muted" -> {
                Log.d(TAG, "Muted: ${message.data}")
            }

            "speaker_on" -> {
                audioRouting.setAudioDevice(AudioManager.STREAM_SPEAKER)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRouting.cleanup()
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
