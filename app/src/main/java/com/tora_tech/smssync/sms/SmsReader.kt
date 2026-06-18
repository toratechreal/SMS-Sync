package com.tora_tech.smssync.sms

import android.content.Context
import android.provider.Telephony
import com.tora_tech.smssync.data.Dedupe
import com.tora_tech.smssync.data.Direction
import com.tora_tech.smssync.data.MessageDto

/** Reads existing SMS from the system content provider for backfill. */
class SmsReader(private val context: Context) {

    /**
     * Returns inbox + sent messages newer than [sinceTimestamp] (ms epoch), oldest first.
     * Caller persists the max timestamp seen as the next cursor.
     */
    fun readSince(sinceTimestamp: Long): List<MessageDto> {
        val result = mutableListOf<MessageDto>()
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
        )
        val selection = "${Telephony.Sms.DATE} > ?"
        val args = arrayOf(sinceTimestamp.toString())
        val sort = "${Telephony.Sms.DATE} ASC"

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, projection, selection, args, sort
        )?.use { cursor ->
            val iAddress = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iBody = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val iType = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (cursor.moveToNext()) {
                val address = cursor.getString(iAddress) ?: continue
                val body = cursor.getString(iBody) ?: ""
                val date = cursor.getLong(iDate)
                val type = cursor.getInt(iType)
                val direction =
                    if (type == Telephony.Sms.MESSAGE_TYPE_SENT) Direction.OUTBOUND else Direction.INBOUND
                result += MessageDto(
                    address = address,
                    body = body,
                    direction = direction,
                    smsTimestamp = date,
                    dedupeKey = Dedupe.key(address, date, body),
                )
            }
        }
        return result
    }
}
