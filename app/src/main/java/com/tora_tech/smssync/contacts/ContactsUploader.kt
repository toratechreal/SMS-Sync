package com.tora_tech.smssync.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.tora_tech.smssync.SmsSyncApplication
import com.tora_tech.smssync.data.ContactDto
import com.tora_tech.smssync.data.ContactsRepository
import com.tora_tech.smssync.data.MessagesRepository
import com.tora_tech.smssync.data.SupabaseProvider
import com.tora_tech.smssync.data.normalizePhone

/**
 * Uploads contact names for numbers the user has actually texted: reads the device's
 * contacts, intersects them with the addresses present in `messages`, and upserts the
 * matches into `contacts`. Only runs when the setting is on and READ_CONTACTS is granted.
 */
object ContactsUploader {

    suspend fun syncOnce(context: Context, app: SmsSyncApplication) {
        val config = app.configStore.current()
        if (!config.contactsSyncEnabled || !config.isPaired) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val client = SupabaseProvider.from(config) ?: return
        val deviceContacts = DeviceContacts.read(context)
        if (deviceContacts.isEmpty()) return

        val textedNumbers = runCatching { MessagesRepository(client).fetchAll() }
            .getOrNull().orEmpty()
            .map { normalizePhone(it.address) }
            .toSet()

        val matched = textedNumbers.mapNotNull { phone ->
            deviceContacts[phone]?.let { name ->
                ContactDto(phone = phone, name = name, sourceDeviceId = config.deviceId)
            }
        }
        runCatching { ContactsRepository(client).upsertAll(matched) }
    }
}
