package com.tora_tech.smssync.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
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

/** Contact names for texted numbers, shared across devices. Keyed by normalized phone. */
class ContactsRepository(private val client: SupabaseClient) {

    suspend fun upsertAll(contacts: List<ContactDto>) {
        if (contacts.isEmpty()) return
        client.from("contacts").upsert(contacts) { onConflict = "phone" }
    }

    suspend fun fetchAll(): List<ContactDto> =
        client.from("contacts").select().decodeList()

    /** Emits all contacts initially and on every change. */
    @OptIn(DelicateCoroutinesApi::class)
    fun observeAll(): Flow<List<ContactDto>> = channelFlow {
        val channel = client.channel(uniqueChannelName("contacts"))
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "contacts" }
        val job = launch {
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
