package com.tora_tech.smssync.data

/** Stable key so the same SMS is never stored twice across backfill + live capture. */
object Dedupe {
    fun key(address: String, timestamp: Long, body: String): String =
        "$address|$timestamp|${body.hashCode()}"
}
