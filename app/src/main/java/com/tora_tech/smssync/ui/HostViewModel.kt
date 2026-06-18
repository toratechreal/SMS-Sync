package com.tora_tech.smssync.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tora_tech.smssync.SmsSyncApplication
import com.tora_tech.smssync.data.DeviceStatusDto
import com.tora_tech.smssync.data.DeviceStatusRepository
import com.tora_tech.smssync.data.PingChannel
import com.tora_tech.smssync.data.SupabaseProvider
import com.tora_tech.smssync.device.DeviceStatusReporter
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Host-side: streams the status rows of paired devices (so the host sees the client). */
class HostViewModel(app: Application) : AndroidViewModel(app) {

    private val store = (app as SmsSyncApplication).configStore

    private val _statuses = MutableStateFlow<List<DeviceStatusDto>>(emptyList())
    val statuses: StateFlow<List<DeviceStatusDto>> = _statuses.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private var repo: DeviceStatusRepository? = null
    private var client: SupabaseClient? = null

    var deviceId: String = ""
        private set

    init {
        viewModelScope.launch {
            val config = store.current()
            deviceId = config.deviceId
            val c = SupabaseProvider.from(config) ?: return@launch
            client = c
            repo = DeviceStatusRepository(c)
            repo!!.observeAll().catch { }.collect { _statuses.value = it }
        }
    }

    /** Re-report self, ping other devices to report, and pull the latest statuses. */
    fun refresh() = viewModelScope.launch {
        val r = repo ?: return@launch
        _refreshing.value = true
        try {
            val config = store.current()
            runCatching {
                r.report(DeviceStatusReporter(getApplication()).collect(config.deviceId, "host"))
            }
            client?.let { runCatching { PingChannel.ping(it) } }
            runCatching { _statuses.value = r.fetchAll() }
        } finally {
            _refreshing.value = false
        }
    }

    /** Delete a stale/orphaned device status row. */
    fun removeDevice(id: String) = viewModelScope.launch {
        val r = repo ?: return@launch
        runCatching { r.remove(id) }
        runCatching { _statuses.value = r.fetchAll() }
    }
}
