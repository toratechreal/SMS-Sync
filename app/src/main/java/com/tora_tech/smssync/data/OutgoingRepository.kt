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
import java.time.Instant

/** The reply queue: clients enqueue, the host drains and sends. */
class OutgoingRepository(private val client: SupabaseClient) {

    /** Client side: enqueue a reply to be sent by the host. */
    suspend fun enqueue(address: String, body: String) {
        client.from("outgoing").insert(
            OutgoingDto(address = address, body = body, status = OutgoingStatus.PENDING)
        )
    }

    /** Host side: fetch everything still waiting to be sent, oldest first. */
    suspend fun fetchPending(): List<OutgoingDto> =
        client.from("outgoing")
            .select {
                filter { eq("status", OutgoingStatus.PENDING) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList()

    suspend fun markSent(id: String) {
        client.from("outgoing").update({
            set("status", OutgoingStatus.SENT)
            set("sent_at", Instant.now().toString())
        }) { filter { eq("id", id) } }
    }

    suspend fun markFailed(id: String, error: String) {
        client.from("outgoing").update({
            set("status", OutgoingStatus.FAILED)
            set("error", error)
        }) { filter { eq("id", id) } }
    }

    /** Client side: not-yet-sent items (pending + failed) for status chips. */
    suspend fun fetchUnsent(): List<OutgoingDto> =
        client.from("outgoing")
            .select {
                filter {
                    or {
                        eq("status", OutgoingStatus.PENDING)
                        eq("status", OutgoingStatus.FAILED)
                    }
                }
                order("created_at", Order.ASCENDING)
            }
            .decodeList()

    /** Client side: emits the un-sent items (Outbox/Failed) initially and on every change. */
    @OptIn(DelicateCoroutinesApi::class)
    fun observeUnsent(): Flow<List<OutgoingDto>> = channelFlow {
        val channel = client.channel(uniqueChannelName("outgoing"))
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "outgoing" }
        val job = launch {
            runCatching { changes.collect { runCatching { trySend(fetchUnsent()) } } }
        }
        runCatching { channel.subscribe() }
        runCatching { trySend(fetchUnsent()) }
        awaitClose {
            job.cancel()
            GlobalScope.launch { runCatching { channel.unsubscribe() } }
        }
    }.flowOn(Dispatchers.IO)

    /** Host side: emits the pending queue initially and on every change. */
    @OptIn(DelicateCoroutinesApi::class)
    fun observePending(): Flow<List<OutgoingDto>> = channelFlow {
        val channel = client.channel(uniqueChannelName("outgoing"))
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "outgoing" }
        val job = launch {
            runCatching { changes.collect { runCatching { trySend(fetchPending()) } } }
        }
        runCatching { channel.subscribe() }
        runCatching { trySend(fetchPending()) }
        awaitClose {
            job.cancel()
            GlobalScope.launch { runCatching { channel.unsubscribe() } }
        }
    }.flowOn(Dispatchers.IO)
}
