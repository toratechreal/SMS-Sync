package com.tora_tech.smssync.sms

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume

/** Sends SMS through the SIM and reports the actual radio result via the sent-intent. */
class SmsSender(private val context: Context) {

    @Suppress("DEPRECATION")
    private fun smsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(SmsManager::class.java)
        else
            SmsManager.getDefault()

    /**
     * Sends [body] to [address], splitting into parts when needed, and suspends until
     * the system reports the send result for every part. Returns success only when the
     * radio accepted all parts; otherwise a failure carrying the first error.
     *
     * Note: success means "accepted for transmission", not "delivered to recipient".
     */
    suspend fun send(address: String, body: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val sms = smsManager()
            val parts = sms.divideMessage(body)
            val total = parts.size.coerceAtLeast(1)
            val action = "com.tora_tech.smssync.SMS_SENT." + UUID.randomUUID()

            var received = 0
            var firstError: String? = null

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, i: Intent) {
                    received++
                    if (resultCode != Activity.RESULT_OK && firstError == null) {
                        firstError = errorText(resultCode)
                    }
                    if (received >= total) {
                        runCatching { context.unregisterReceiver(this) }
                        if (cont.isActive) {
                            cont.resume(
                                firstError?.let { Result.failure(IOException(it)) }
                                    ?: Result.success(Unit)
                            )
                        }
                    }
                }
            }

            ContextCompat.registerReceiver(
                context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED
            )
            cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            try {
                if (parts.size > 1) {
                    val sentIntents = ArrayList<PendingIntent>(parts.size)
                    for (idx in parts.indices) {
                        sentIntents.add(
                            PendingIntent.getBroadcast(
                                context, idx,
                                Intent(action).setPackage(context.packageName), flags
                            )
                        )
                    }
                    sms.sendMultipartTextMessage(address, null, parts, sentIntents, null)
                } else {
                    val sentIntent = PendingIntent.getBroadcast(
                        context, 0,
                        Intent(action).setPackage(context.packageName), flags
                    )
                    sms.sendTextMessage(address, null, body, sentIntent, null)
                }
            } catch (e: Exception) {
                runCatching { context.unregisterReceiver(receiver) }
                if (cont.isActive) cont.resume(Result.failure(e))
            }
        }

    private fun errorText(resultCode: Int): String = when (resultCode) {
        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "generic failure"
        SmsManager.RESULT_ERROR_NO_SERVICE -> "no service"
        SmsManager.RESULT_ERROR_NULL_PDU -> "null PDU"
        SmsManager.RESULT_ERROR_RADIO_OFF -> "radio off"
        else -> "send failed (code $resultCode)"
    }
}
