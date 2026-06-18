package com.tora_tech.smssync.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tora_tech.smssync.SmsSyncApplication
import com.tora_tech.smssync.client.ClientSyncService
import com.tora_tech.smssync.contacts.ContactsUploader
import com.tora_tech.smssync.data.AppConfig
import com.tora_tech.smssync.data.AppMode
import com.tora_tech.smssync.data.SyncMode
import com.tora_tech.smssync.device.ServiceWatchdogWorker
import com.tora_tech.smssync.host.HostSyncService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Top-level state: which mode we're in, pairing status, and mode/pairing actions. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val store = (app as SmsSyncApplication).configStore

    val config: StateFlow<AppConfig?> =
        store.configFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setMode(mode: AppMode) = viewModelScope.launch { store.setMode(mode) }

    /** Host owner saved their Supabase project: persist it and start syncing. */
    fun saveHostPairing(url: String, key: String) = viewModelScope.launch {
        store.setPairing(url.trim(), key.trim())
        applyHostServices()
    }

    /** Client scanned a QR: persist the project coordinates and start client services. */
    fun saveClientPairing(url: String, key: String) = viewModelScope.launch {
        store.setPairing(url.trim(), key.trim())
        applyClientServices()
    }

    fun startHostServices() = viewModelScope.launch { applyHostServices() }

    fun startClientServices() = viewModelScope.launch { applyClientServices() }

    fun dismissKeepAlive() = viewModelScope.launch { store.setKeepAliveDismissed(true) }

    /** Enable/disable contact-name sync; records that we've shown the prompt. */
    fun setContactsSyncEnabled(enabled: Boolean) = viewModelScope.launch {
        store.setContactsSyncEnabled(enabled)
        store.setContactsPromptShown(true)
        if (enabled) {
            val app = getApplication<Application>() as SmsSyncApplication
            runCatching { ContactsUploader.syncOnce(app, app) }
        }
    }

    fun markContactsPromptShown() = viewModelScope.launch { store.setContactsPromptShown(true) }

    /** Switch sync mode and (re)apply the right services for the current role. */
    fun setSyncMode(mode: SyncMode) = viewModelScope.launch {
        store.setSyncMode(mode)
        when (store.current().mode) {
            AppMode.HOST -> applyHostServices()
            AppMode.CLIENT -> applyClientServices()
            null -> {}
        }
    }

    private suspend fun applyHostServices() {
        val ctx = getApplication<Application>()
        ServiceWatchdogWorker.schedule(ctx)
        if (store.current().syncMode == SyncMode.REALTIME) HostSyncService.start(ctx)
        else HostSyncService.stop(ctx)
    }

    private suspend fun applyClientServices() {
        val ctx = getApplication<Application>()
        ServiceWatchdogWorker.schedule(ctx)
        if (store.current().syncMode == SyncMode.REALTIME) ClientSyncService.start(ctx)
        else ClientSyncService.stop(ctx)
    }
}
