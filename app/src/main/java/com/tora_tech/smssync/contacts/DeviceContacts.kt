package com.tora_tech.smssync.contacts

import android.content.Context
import android.provider.ContactsContract
import com.tora_tech.smssync.data.normalizePhone

/** A device contact entry for the new-chat picker. */
data class DeviceContact(val name: String, val number: String)

/** Reads device contacts into a map of normalized phone number -> display name. */
object DeviceContacts {

    /** Reads contacts as a list (name + display number), de-duped by number, sorted by name. */
    fun readList(context: Context): List<DeviceContact> {
        val seen = HashSet<String>()
        val out = ArrayList<DeviceContact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        )
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC",
            )?.use { cursor ->
                val iNumber = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val iName = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val number = cursor.getString(iNumber) ?: continue
                    val name = cursor.getString(iName) ?: continue
                    val key = normalizePhone(number)
                    if (name.isNotBlank() && seen.add(key)) {
                        out.add(DeviceContact(name = name, number = number))
                    }
                }
            }
        }
        return out
    }

    fun read(context: Context): Map<String, String> {
        val map = HashMap<String, String>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        )
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null, null,
            )?.use { cursor ->
                val iNumber = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val iName = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val number = cursor.getString(iNumber) ?: continue
                    val name = cursor.getString(iName) ?: continue
                    val key = normalizePhone(number)
                    if (key.isNotBlank() && name.isNotBlank()) {
                        map.putIfAbsent(key, name)
                    }
                }
            }
        }
        return map
    }
}
