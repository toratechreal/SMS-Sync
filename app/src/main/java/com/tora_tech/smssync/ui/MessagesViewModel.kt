package com.tora_tech.smssync.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tora_tech.smssync.SmsSyncApplication
import com.tora_tech.smssync.data.ContactsRepository
import com.tora_tech.smssync.data.DeviceStatusDto
import com.tora_tech.smssync.data.DeviceStatusRepository
import com.tora_tech.smssync.data.Direction
import com.tora_tech.smssync.data.MessageDto
import com.tora_tech.smssync.data.MessagesRepository
import com.tora_tech.smssync.data.OutgoingDto
import com.tora_tech.smssync.data.OutgoingRepository
import com.tora_tech.smssync.data.OutgoingStatus
import com.tora_tech.smssync.data.PingChannel
import com.tora_tech.smssync.data.SupabaseProvider
import io.github.jan.supabase.SupabaseClient
import com.tora_tech.smssync.data.normalizePhone
import com.tora_tech.smssync.device.DeviceStatusReporter
import com.tora_tech.smssync.device.ServiceWatchdogWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import kotlin.time.Duration.Companion.milliseconds

/** Send status shown as a chip on outbound items. */
enum class MsgStatus { OUTBOX, SENT, FAILED }

/** A unified thread entry: a synced message, or a not-yet-sent queued reply. */
data class ThreadItem(
    val key: String,
    val address: String,
    val body: String,
    val outbound: Boolean,
    val timestamp: Long,
    val status: MsgStatus?, // null for inbound
)

/** One conversation thread, summarized for the list screen. */
data class Conversation(
    val address: String,
    val lastBody: String,
    val lastTimestamp: Long,
    val lastStatus: MsgStatus?,
)

/** Client-side: streams synced messages + queue, resolves names, enqueues replies. */
class MessagesViewModel(app: Application) : AndroidViewModel(app) {

    private val store = (app as SmsSyncApplication).configStore

    private val _messages = MutableStateFlow<List<MessageDto>>(emptyList())
    val messages: StateFlow<List<MessageDto>> = _messages.asStateFlow()

    /** Un-sent queue items (pending/failed) for Outbox/Failed chips. */
    private val _outgoing = MutableStateFlow<List<OutgoingDto>>(emptyList())
    val outgoing: StateFlow<List<OutgoingDto>> = _outgoing.asStateFlow()

    private val _statuses = MutableStateFlow<List<DeviceStatusDto>>(emptyList())
    val statuses: StateFlow<List<DeviceStatusDto>> = _statuses.asStateFlow()

    /** normalized phone -> contact name */
    private val _contacts = MutableStateFlow<Map<String, String>>(emptyMap())
    val contacts: StateFlow<Map<String, String>> = _contacts.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private var outgoingRepo: OutgoingRepository? = null
    private var deviceStatusRepo: DeviceStatusRepository? = null
    private var client: SupabaseClient? = null

    var deviceId: String = ""
        private set

    init {
        viewModelScope.launch {
            val config = store.current()
            deviceId = config.deviceId
            val client = SupabaseProvider.from(config) ?: return@launch
            this@MessagesViewModel.client = client
            outgoingRepo = OutgoingRepository(client)

            ServiceWatchdogWorker.schedule(getApplication())

            val deviceStatus = DeviceStatusRepository(client)
            deviceStatusRepo = deviceStatus
            val reporter = DeviceStatusReporter(getApplication())
            launch {
                while (isActive) {
                    runCatching {
                        deviceStatus.report(reporter.collect(config.deviceId, "client"))
                    }
                    delay(TELEMETRY_INTERVAL_MS.milliseconds)
                }
            }

            launch { deviceStatus.observeAll().catch { }.collect { _statuses.value = it } }
            launch {
                ContactsRepository(client).observeAll().catch { }.collect { list ->
                    _contacts.value = list.associate { it.phone to it.name }
                }
            }
            launch {
                OutgoingRepository(client).observeUnsent().catch { }.collect { _outgoing.value = it }
            }

            MessagesRepository(client).observeAll()
                .catch { }
                .collect { _messages.value = it }
        }
    }

    /** Merge messages + un-sent queue into one time-sorted list with statuses. */
    private fun buildItems(messages: List<MessageDto>, outgoing: List<OutgoingDto>): List<ThreadItem> {
        val msgItems = messages.map { m ->
            val outbound = m.direction == Direction.OUTBOUND
            ThreadItem(
                key = "m:" + (m.id ?: m.dedupeKey),
                address = m.address,
                body = m.body,
                outbound = outbound,
                timestamp = m.smsTimestamp,
                status = if (outbound) MsgStatus.SENT else null,
            )
        }
        val outItems = outgoing.map { o ->
            ThreadItem(
                key = "o:" + (o.id ?: (o.address + o.body)),
                address = o.address,
                body = o.body,
                outbound = true,
                timestamp = parseIso(o.createdAt),
                status = if (o.status == OutgoingStatus.FAILED) MsgStatus.FAILED else MsgStatus.OUTBOX,
            )
        }
        return (msgItems + outItems).sortedBy { it.timestamp }
    }

    /** Distinct threads, most-recent first, with the latest item's status. */
    fun conversations(
        messages: List<MessageDto>,
        outgoing: List<OutgoingDto>,
    ): List<Conversation> =
        buildItems(messages, outgoing)
            .groupBy { normalizeAddress(it.address) }
            .map { (_, list) ->
                val last = list.maxBy { it.timestamp }
                Conversation(last.address, last.body, last.timestamp, last.status)
            }
            .sortedByDescending { it.lastTimestamp }

    fun thread(
        messages: List<MessageDto>,
        outgoing: List<OutgoingDto>,
        address: String,
    ): List<ThreadItem> {
        val key = normalizeAddress(address)
        return buildItems(messages, outgoing).filter { normalizeAddress(it.address) == key }
    }

    fun sendReply(address: String, body: String) = viewModelScope.launch {
        if (body.isNotBlank()) outgoingRepo?.enqueue(address, body.trim())
    }

    /** Re-report this device's own status and pull the latest statuses. */
    fun refresh() = viewModelScope.launch {
        val r = deviceStatusRepo ?: return@launch
        _refreshing.value = true
        try {
            val config = store.current()
            runCatching {
                r.report(DeviceStatusReporter(getApplication()).collect(config.deviceId, "client"))
            }
            client?.let { runCatching { PingChannel.ping(it) } }
            runCatching { _statuses.value = r.fetchAll() }
        } finally {
            _refreshing.value = false
        }
    }

    /** Delete a stale/orphaned device status row. */
    fun removeDevice(id: String) = viewModelScope.launch {
        val r = deviceStatusRepo ?: return@launch
        runCatching { r.remove(id) }
        runCatching { _statuses.value = r.fetchAll() }
    }

    /** Digits-only key for threading; falls back to the raw string for short/alpha senders. */
    fun normalizeAddress(s: String): String = normalizePhone(s)

    private fun parseIso(s: String?): Long =
        s?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }
            ?: System.currentTimeMillis()

    private companion object {
        const val TELEMETRY_INTERVAL_MS = 60_000L
    }
}
