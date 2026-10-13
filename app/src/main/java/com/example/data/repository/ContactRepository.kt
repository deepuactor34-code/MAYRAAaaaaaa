package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.domain.model.ContactInfo

class ContactRepository(private val context: Context) {

    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun searchContacts(query: String): List<ContactInfo> {
        if (!hasContactsPermission() || query.isBlank()) {
            return emptyList()
        }

        val contactsList = mutableListOf<ContactInfo>()
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cleanQuery = query.trim().replace("'", "''")
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$cleanQuery%")
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val seenNumbers = mutableSetOf<String>()
                while (it.moveToNext()) {
                    val id = if (idIndex != -1) it.getString(idIndex) else ""
                    val name = if (nameIndex != -1) it.getString(nameIndex) ?: "" else ""
                    val rawNumber = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""
                    val cleanNumber = rawNumber.replace("\\s+".toRegex(), "").replace("-", "")

                    if (cleanNumber.isNotEmpty() && !seenNumbers.contains(cleanNumber)) {
                        seenNumbers.add(cleanNumber)
                        contactsList.add(ContactInfo(id = id, name = name, phoneNumber = cleanNumber))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return contactsList
    }
}
