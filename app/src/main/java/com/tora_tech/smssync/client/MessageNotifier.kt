package com.tora_tech.smssync.client

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tora_tech.smssync.MainActivity
import com.tora_tech.smssync.R
import com.tora_tech.smssync.data.ConfigStore
import com.tora_tech.smssync.data.Direction
import com.tora_tech.smssync.data.MessageDto

/** Posts notifications for new inbound messages on the client. */
class MessageNotifier(private val context: Context) {

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH,
        )
        manager.createNotificationChannel(channel)
    }

    /**
     * Notifies for inbound messages newer than the stored cursor. On the very first run
     * (cursor == 0) it just sets a baseline so the backfilled history doesn't all fire.
     */
    suspend fun notifyNewInbound(store: ConfigStore, messages: List<MessageDto>) {
        val cursor = store.current().lastNotifiedTs
        if (cursor == 0L) {
            val baseline = messages.maxOfOrNull { it.smsTimestamp } ?: System.currentTimeMillis()
            store.setLastNotifiedTs(baseline)
            return
        }
        val fresh = messages
            .filter { it.direction == Direction.INBOUND && it.smsTimestamp > cursor }
            .sortedBy { it.smsTimestamp }
        if (fresh.isEmpty()) return

        ensureChannel()
        fresh.forEach { notify(it) }
        store.setLastNotifiedTs(fresh.maxOf { it.smsTimestamp })
    }

    private fun notify(message: MessageDto) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(message.address)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        // Throws if POST_NOTIFICATIONS isn't granted (API 33+); ignore in that case.
        runCatching {
            NotificationManagerCompat.from(context).notify(message.dedupeKey.hashCode(), notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "messages"
    }
}
