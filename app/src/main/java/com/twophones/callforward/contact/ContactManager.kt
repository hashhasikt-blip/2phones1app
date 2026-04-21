package com.twophones.callforward.contact

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.twophones.callforward.bluetooth.BluetoothMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.Serializable

data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String
) : Serializable

class ContactManager(private val context: Context) {

    companion object {
        private const val TAG = "ContactManager"
    }

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    fun loadContacts(): List<Contact> {
        return try {
            val contacts = mutableListOf<Contact>()
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
                ),
                null,
                null,
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))

                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id.toString()),
                        null
                    )

                    phoneCursor?.use { pc ->
                        if (pc.moveToFirst()) {
                            val phoneNumber = pc.getString(
                                pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            )
                            contacts.add(Contact(id, name, phoneNumber))
                        }
                    }
                }
            }

            _contacts.value = contacts
            Log.d(TAG, "Loaded ${contacts.size} contacts")
            contacts
        } catch (e: Exception) {
            Log.e(TAG, "Error loading contacts", e)
            emptyList()
        }
    }

    fun serializeContactsForBluetooth(): BluetoothMessage {
        val contactsJson = _contacts.value.joinToString(";") { contact ->
            "${contact.id},${contact.name},${contact.phoneNumber}"
        }
        return BluetoothMessage(
            type = "contacts",
            data = contactsJson
        )
    }

    fun deserializeContactsFromBluetooth(data: String): List<Contact> {
        return try {
            data.split(";").mapNotNull { contactStr ->
                val parts = contactStr.split(",")
                if (parts.size == 3) {
                    Contact(
                        id = parts[0].toLong(),
                        name = parts[1],
                        phoneNumber = parts[2]
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing contacts", e)
            emptyList()
        }
    }

    fun getContactByPhoneNumber(phoneNumber: String): Contact? {
        return _contacts.value.find { it.phoneNumber.replace(Regex("[^0-9]"), "") == phoneNumber.replace(Regex("[^0-9]"), "") }
    }
}
