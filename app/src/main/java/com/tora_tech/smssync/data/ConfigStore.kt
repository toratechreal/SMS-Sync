package com.tora_tech.smssync.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smssync_config")

/** Immutable snapshot of everything we persist locally. */
data class AppConfig(
    val mode: AppMode?,
    val supabaseUrl: String?,
    val supabaseKey: String?,
    val deviceId: String,
    val backfillDone: Boolean,
    val backfillCursor: Long,
    val syncMode: SyncMode,
    val keepAliveDismissed: Boolean,
    val lastNotifiedTs: Long,
    val contactsSyncEnabled: Boolean,
    val contactsPromptShown: Boolean,
) {
    val isPaired: Boolean get() = !supabaseUrl.isNullOrBlank() && !supabaseKey.isNullOrBlank()
}

/** Reads/writes local configuration backed by Preferences DataStore. */
class ConfigStore(private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val SUPABASE_URL = stringPreferencesKey("supabase_url")
        val SUPABASE_KEY = stringPreferencesKey("supabase_key")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val BACKFILL_DONE = booleanPreferencesKey("backfill_done")
        val BACKFILL_CURSOR = longPreferencesKey("backfill_cursor")
        val SYNC_MODE = stringPreferencesKey("sync_mode")
        val KEEP_ALIVE_DISMISSED = booleanPreferencesKey("keep_alive_dismissed")
        val LAST_NOTIFIED_TS = longPreferencesKey("last_notified_ts")
        val CONTACTS_SYNC_ENABLED = booleanPreferencesKey("contacts_sync_enabled")
        val CONTACTS_PROMPT_SHOWN = booleanPreferencesKey("contacts_prompt_shown")
    }

    val configFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        AppConfig(
            mode = prefs[Keys.MODE]?.let { runCatching { AppMode.valueOf(it) }.getOrNull() },
            supabaseUrl = prefs[Keys.SUPABASE_URL],
            supabaseKey = prefs[Keys.SUPABASE_KEY],
            deviceId = prefs[Keys.DEVICE_ID] ?: "",
            backfillDone = prefs[Keys.BACKFILL_DONE] ?: false,
            backfillCursor = prefs[Keys.BACKFILL_CURSOR] ?: 0L,
            syncMode = prefs[Keys.SYNC_MODE]?.let { runCatching { SyncMode.valueOf(it) }.getOrNull() }
                ?: SyncMode.REALTIME,
            keepAliveDismissed = prefs[Keys.KEEP_ALIVE_DISMISSED] ?: false,
            lastNotifiedTs = prefs[Keys.LAST_NOTIFIED_TS] ?: 0L,
            contactsSyncEnabled = prefs[Keys.CONTACTS_SYNC_ENABLED] ?: false,
            contactsPromptShown = prefs[Keys.CONTACTS_PROMPT_SHOWN] ?: false,
        )
    }

    /** Reads the current snapshot, generating + persisting a device id on first access. */
    suspend fun current(): AppConfig {
        ensureDeviceId()
        return configFlow.first()
    }

    private suspend fun ensureDeviceId() {
        val prefs = context.dataStore.data.first()
        if (prefs[Keys.DEVICE_ID].isNullOrBlank()) {
            context.dataStore.edit { it[Keys.DEVICE_ID] = UUID.randomUUID().toString() }
        }
    }

    suspend fun setMode(mode: AppMode) {
        ensureDeviceId()
        context.dataStore.edit { it[Keys.MODE] = mode.name }
    }

    suspend fun setPairing(url: String, key: String) {
        context.dataStore.edit {
            it[Keys.SUPABASE_URL] = url
            it[Keys.SUPABASE_KEY] = key
        }
    }

    suspend fun setBackfillDone(done: Boolean) {
        context.dataStore.edit { it[Keys.BACKFILL_DONE] = done }
    }

    suspend fun setBackfillCursor(cursor: Long) {
        context.dataStore.edit { it[Keys.BACKFILL_CURSOR] = cursor }
    }

    suspend fun setSyncMode(mode: SyncMode) {
        context.dataStore.edit { it[Keys.SYNC_MODE] = mode.name }
    }

    suspend fun setKeepAliveDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_ALIVE_DISMISSED] = dismissed }
    }

    suspend fun setLastNotifiedTs(ts: Long) {
        context.dataStore.edit { it[Keys.LAST_NOTIFIED_TS] = ts }
    }

    suspend fun setContactsSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CONTACTS_SYNC_ENABLED] = enabled }
    }

    suspend fun setContactsPromptShown(shown: Boolean) {
        context.dataStore.edit { it[Keys.CONTACTS_PROMPT_SHOWN] = shown }
    }

}
