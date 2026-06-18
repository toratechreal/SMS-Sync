package com.tora_tech.smssync.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/** Reads/writes the `messages` table and streams live changes via Realtime. */
class MessagesRepository(private val client: SupabaseClient) {

    suspend fun fetchAll(): List<MessageDto> =
        client.from("messages")
            .select { order("sms_timestamp", Order.ASCENDING) }
            .decodeList()

    /** Idempotent insert keyed on `dedupe_key` so backfill never duplicates. */
    suspend fun upsert(message: MessageDto) {
        client.from("messages").upsert(message) { onConflict = "dedupe_key" }
    }

    suspend fun upsertAll(messages: List<MessageDto>) {
        if (messages.isEmpty()) return
        client.from("messages").upsert(messages) { onConflict = "dedupe_key" }
    }

    /** Emits the full list initially and again on every realtime change. */
    @OptIn(DelicateCoroutinesApi::class)
    fun observeAll(): Flow<List<MessageDto>> = channelFlow {
        val channel = client.channel(uniqueChannelName("messages"))
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "messages" }
        val job = launch {
            // A realtime/websocket error must not crash the collector; keep last data.
            runCatching { changes.collect { runCatching { trySend(fetchAll()) } } }
        }
        runCatching { channel.subscribe() }
        runCatching { trySend(fetchAll()) }
        awaitClose {
            job.cancel()
            GlobalScope.launch { runCatching { channel.unsubscribe() } }
        }
    }.flowOn(Dispatchers.IO)
}
