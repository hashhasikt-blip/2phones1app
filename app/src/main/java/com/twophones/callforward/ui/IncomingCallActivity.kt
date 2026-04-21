package com.twophones.callforward.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.twophones.callforward.R
import com.twophones.callforward.databinding.ActivityIncomingCallBinding

class IncomingCallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "IncomingCallActivity"
        const val EXTRA_PHONE_NUMBER = "phone_number"
    }

    private lateinit var binding: ActivityIncomingCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
        
        binding.tvCallerName.text = phoneNumber
        
        binding.btnAccept.setOnClickListener {
            acceptCall(phoneNumber)
        }

        binding.btnReject.setOnClickListener {
            rejectCall()
        }
    }

    private fun acceptCall(phoneNumber: String) {
        Log.d(TAG, "Call accepted: $phoneNumber")
        
        // Send call state to main app via intent
        val intent = Intent("com.twophones.callforward.CALL_ACCEPTED").apply {
            putExtra("phone_number", phoneNumber)
        }
        sendBroadcast(intent)
        
        finish()
    }

    private fun rejectCall() {
        Log.d(TAG, "Call rejected")
        
        val intent = Intent("com.twophones.callforward.CALL_REJECTED")
        sendBroadcast(intent)
        
        finish()
    }
}
