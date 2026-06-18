package com.tora_tech.smssync.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.tora_tech.smssync.SmsSyncApplication
import com.tora_tech.smssync.data.Dedupe
import com.tora_tech.smssync.data.Direction
import com.tora_tech.smssync.data.MessageDto
import com.tora_tech.smssync.data.MessagesRepository
import com.tora_tech.smssync.data.SupabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Captures incoming SMS live and pushes each one to the user's Supabase. */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return

        val first = parts[0]
        val address = first.displayOriginatingAddress ?: first.originatingAddress ?: return
        val body = parts.joinToString("") { it.displayMessageBody ?: it.messageBody ?: "" }
        val timestamp = first.timestampMillis

        val dto = MessageDto(
            address = address,
            body = body,
            direction = Direction.INBOUND,
            smsTimestamp = timestamp,
            dedupeKey = Dedupe.key(address, timestamp, body),
        )

        val pending = goAsync()
        val app = context.applicationContext as SmsSyncApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = app.configStore.current()
                SupabaseProvider.from(config)?.let { MessagesRepository(it).upsert(dto) }
            } catch (_: Exception) {
                // Network/Supabase errors are non-fatal here; backfill will reconcile later.
            } finally {
                pending.finish()
            }
        }
    }
}
