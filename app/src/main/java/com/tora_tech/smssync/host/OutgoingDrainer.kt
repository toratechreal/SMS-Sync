package com.tora_tech.smssync.host

import android.content.Context
import com.tora_tech.smssync.data.Dedupe
import com.tora_tech.smssync.data.Direction
import com.tora_tech.smssync.data.MessageDto
import com.tora_tech.smssync.data.MessagesRepository
import com.tora_tech.smssync.data.OutgoingDto
import com.tora_tech.smssync.data.OutgoingRepository
import com.tora_tech.smssync.sms.SmsSender
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Sends queued replies through the SIM and reflects the real radio result back into
 * the `outgoing` row. Shared by the realtime [HostSyncService] and the 15-min worker.
 */
class OutgoingDrainer(
    context: Context,
    private val messages: MessagesRepository,
    private val outgoing: OutgoingRepository,
) {
    private val sender = SmsSender(context)

    /** Sends one queued item; marks it sent (and records the outbound message) or failed. */
    suspend fun sendOne(item: OutgoingDto) {
        val id = item.id ?: return
        val result = withTimeoutOrNull(SEND_TIMEOUT_MS.milliseconds) { sender.send(item.address, item.body) }
        if (result != null && result.isSuccess) {
            val ts = System.currentTimeMillis()
            runCatching {
                messages.upsert(
                    MessageDto(
                        address = item.address,
                        body = item.body,
                        direction = Direction.OUTBOUND,
                        smsTimestamp = ts,
                        dedupeKey = Dedupe.key(item.address, ts, item.body),
                    )
                )
            }
            runCatching { outgoing.markSent(id) }
        } else {
            val reason = result?.exceptionOrNull()?.message ?: "send timed out"
            runCatching { outgoing.markFailed(id, reason) }
        }
    }

    /** Drains all currently-pending items once (used by the battery-saver worker). */
    suspend fun drainOnce() {
        runCatching { outgoing.fetchPending() }.getOrNull()?.forEach { sendOne(it) }
    }

    companion object {
        private const val SEND_TIMEOUT_MS = 60_000L
    }
}
