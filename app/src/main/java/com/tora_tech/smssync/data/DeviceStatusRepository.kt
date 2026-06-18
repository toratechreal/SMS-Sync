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

/** Per-device telemetry shared across paired devices. */
class DeviceStatusRepository(private val client: SupabaseClient) {

    /** Upsert this device's latest status. */
    suspend fun report(status: DeviceStatusDto) {
        client.from("device_status").upsert(status) { onConflict = "device_id" }
    }

    suspend fun fetchAll(): List<DeviceStatusDto> =
        client.from("device_status").select().decodeList()

    /** Delete a device's status row (e.g. a stale/orphaned entry). */
    suspend fun remove(deviceId: String) {
        client.from("device_status").delete { filter { eq("device_id", deviceId) } }
    }

    /** Emits all device statuses initially and on every change. */
    @OptIn(DelicateCoroutinesApi::class)
    fun observeAll(): Flow<List<DeviceStatusDto>> = channelFlow {
        val channel = client.channel(uniqueChannelName("device-status"))
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "device_status" }
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
