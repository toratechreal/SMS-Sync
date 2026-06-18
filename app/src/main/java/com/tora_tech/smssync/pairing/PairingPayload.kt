package com.tora_tech.smssync.pairing

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What the pairing QR encodes: the user's Supabase project coordinates. */
@Serializable
data class PairingPayload(
    val url: String,
    val key: String,
) {
    fun encode(): String = Json.encodeToString(serializer(), this)

    companion object {
        fun decode(raw: String): PairingPayload? =
            runCatching { Json.decodeFromString(serializer(), raw) }.getOrNull()
    }
}
