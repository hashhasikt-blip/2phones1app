package com.twophones.callforward.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.twophones.callforward.bluetooth.BluetoothManager
import com.twophones.callforward.bluetooth.BluetoothMessage

class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return

        when (intent?.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        Log.d(TAG, "Incoming call from: $phoneNumber")
                        handleIncomingCall(context, phoneNumber)
                    }
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        Log.d(TAG, "Call offhook: $phoneNumber")
                        handleCallStart(context, phoneNumber)
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        Log.d(TAG, "Call idle")
                        handleCallEnd(context)
                    }
                }
            }

            "android.intent.action.NEW_OUTGOING_CALL" -> {
                val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                Log.d(TAG, "Outgoing call to: $phoneNumber")
                handleOutgoingCall(context, phoneNumber)
            }
        }
    }

    private fun handleIncomingCall(context: Context, phoneNumber: String?) {
        phoneNumber ?: return

        // Send call info via Bluetooth
        val btManager = BluetoothManager(context)
        if (btManager.isConnected()) {
            val message = BluetoothMessage(
                type = "incoming_call",
                data = phoneNumber
            )
            btManager.sendMessage(message)
            Log.d(TAG, "Sent incoming call notification via Bluetooth")
        }

        // Start call monitoring service
        val serviceIntent = Intent(context, CallMonitoringService::class.java).apply {
            putExtra("phone_number", phoneNumber)
            putExtra("call_type", "incoming")
        }
        context.startService(serviceIntent)
    }

    private fun handleOutgoingCall(context: Context, phoneNumber: String?) {
        phoneNumber ?: return

        val serviceIntent = Intent(context, CallMonitoringService::class.java).apply {
            putExtra("phone_number", phoneNumber)
            putExtra("call_type", "outgoing")
        }
        context.startService(serviceIntent)
    }

    private fun handleCallStart(context: Context, phoneNumber: String?) {
        Log.d(TAG, "Call started")
    }

    private fun handleCallEnd(context: Context) {
        Log.d(TAG, "Call ended")
        
        // Stop the call monitoring service
        context.stopService(Intent(context, CallMonitoringService::class.java))
    }
}
