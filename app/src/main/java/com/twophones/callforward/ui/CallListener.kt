package com.twophones.callforward.ui

import android.content.Intent
import android.os.Bundle
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.twophones.callforward.bluetooth.BluetoothManager
import com.twophones.callforward.bluetooth.BluetoothMessage

/**
 * Call listener to handle incoming and outgoing calls
 */
sealed class CallEvent {
    data class IncomingCall(val phoneNumber: String) : CallEvent()
    data class OutgoingCall(val phoneNumber: String) : CallEvent()
    object CallEnded : CallEvent()
    object CallStarted : CallEvent()
}

class CallListener(private val bluetoothManager: BluetoothManager) : Call.Callback() {

    companion object {
        private const val TAG = "CallListener"
    }

    override fun onStateChanged(call: Call, state: Int) {
        super.onStateChanged(call, state)
        
        val phoneNumber = call.details.handle?.schemeSpecificPart ?: "Unknown"
        
        when (state) {
            Call.STATE_RINGING -> {
                Log.d(TAG, "Incoming call: $phoneNumber")
                val message = BluetoothMessage(
                    type = "incoming_call",
                    data = phoneNumber
                )
                bluetoothManager.sendMessage(message)
            }
            
            Call.STATE_DIALING -> {
                Log.d(TAG, "Outgoing call: $phoneNumber")
                val message = BluetoothMessage(
                    type = "outgoing_call",
                    data = phoneNumber
                )
                bluetoothManager.sendMessage(message)
            }
            
            Call.STATE_CONNECTED -> {
                Log.d(TAG, "Call connected: $phoneNumber")
                val message = BluetoothMessage(
                    type = "call_connected",
                    data = phoneNumber
                )
                bluetoothManager.sendMessage(message)
            }
            
            Call.STATE_DISCONNECTED -> {
                Log.d(TAG, "Call disconnected")
                val message = BluetoothMessage(
                    type = "call_ended",
                    data = "ended"
                )
                bluetoothManager.sendMessage(message)
            }
        }
    }

    override fun onCallDestroyed(call: Call) {
        super.onCallDestroyed(call)
        Log.d(TAG, "Call destroyed")
    }
}
