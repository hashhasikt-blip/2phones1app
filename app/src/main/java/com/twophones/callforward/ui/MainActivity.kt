package com.twophones.callforward.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.twophones.callforward.R
import com.twophones.callforward.bluetooth.BluetoothManager
import com.twophones.callforward.contact.ContactManager
import com.twophones.callforward.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var contactManager: ContactManager

    private val permissions = mutableListOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                Log.d(TAG, "All permissions granted")
                initializeApp()
            } else {
                Log.d(TAG, "Some permissions denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions()
    }

    private fun requestPermissions() {
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            initializeApp()
        }
    }

    private fun initializeApp() {
        bluetoothManager = BluetoothManager(this)
        contactManager = ContactManager(this)

        setupUI()
        loadContacts()
        setupBluetoothListeners()
    }

    private fun setupUI() {
        binding.btnConnect.setOnClickListener {
            val devices = bluetoothManager.getAvailablePairedDevices()
            if (devices.isNotEmpty()) {
                showDeviceSelectionDialog(devices)
            } else {
                updateStatus("No paired devices found")
            }
        }

        binding.btnDisconnect.setOnClickListener {
            bluetoothManager.disconnect()
            updateStatus(getString(R.string.disconnected))
        }

        binding.btnRefreshContacts.setOnClickListener {
            loadContacts()
        }
    }

    private fun loadContacts() {
        lifecycleScope.launch {
            val contacts = contactManager.loadContacts()
            Log.d(TAG, "Loaded ${contacts.size} contacts")
            // Update UI with contacts
            updateContactList(contacts.joinToString("\n") { "${it.name}: ${it.phoneNumber}" })
        }
    }

    private fun setupBluetoothListeners() {
        lifecycleScope.launch {
            bluetoothManager.connectionStatus.collect { status ->
                val statusText = when (status) {
                    BluetoothManager.ConnectionStatus.DISCONNECTED -> getString(R.string.disconnected)
                    BluetoothManager.ConnectionStatus.CONNECTING -> getString(R.string.connecting)
                    BluetoothManager.ConnectionStatus.CONNECTED -> getString(R.string.connected)
                    BluetoothManager.ConnectionStatus.ERROR -> "Error"
                }
                updateStatus(statusText)
            }
        }

        lifecycleScope.launch {
            bluetoothManager.receivedMessages.collect { message ->
                message?.let {
                    Log.d(TAG, "Message received: ${it.type} - ${it.data}")
                    handleReceivedMessage(it)
                }
            }
        }
    }

    private fun handleReceivedMessage(message: com.twophones.callforward.bluetooth.BluetoothMessage) {
        when (message.type) {
            "incoming_call" -> {
                updateStatus("Incoming call from: ${message.data}")
            }

            "call_state" -> {
                updateStatus("Call state: ${message.data}")
            }

            "contacts" -> {
                val contacts = contactManager.deserializeContactsFromBluetooth(message.data)
                updateContactList(contacts.joinToString("\n") { "${it.name}: ${it.phoneNumber}" })
            }
        }
    }

    private fun showDeviceSelectionDialog(devices: List<android.bluetooth.BluetoothDevice>) {
        val deviceNames = devices.map { it.name ?: it.address }.toTypedArray()
        
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.bluetooth_device_selection))
            .setItems(deviceNames) { _, which ->
                bluetoothManager.connectToDevice(devices[which])
                updateStatus(getString(R.string.connecting))
            }
            .show()
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            binding.tvStatus.text = message
            Log.d(TAG, "Status: $message")
        }
    }

    private fun updateContactList(contactText: String) {
        runOnUiThread {
            binding.tvContacts.text = contactText
        }
    }
}
