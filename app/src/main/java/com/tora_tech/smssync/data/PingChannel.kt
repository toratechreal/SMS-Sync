package com.tora_tech.smssync.data

import android.content.Context
import com.tora_tech.smssync.device.DeviceStatusReporter
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * Cross-device "ping to refresh" over Supabase Realtime broadcast (no DB table).
 * Long-lived workers [start] to listen — when pinged, they report their telemetry
 * immediately. Any device (or the web) calls [ping] to ask the others to report now.
 *
 * Only reaches devices currently connected to realtime; a device whose service is
 * dead can't answer.
 */
object PingChannel {
    private const val TOPIC = "device-ping"
    private const val EVENT = "refresh"

    @Volatile private var listening = false

    /** Listen for refresh pings and report this device's status when one arrives. */
    fun start(
        scope: CoroutineScope,
        client: SupabaseClient,
        context: Context,
        deviceId: String,
        role: String,
    ) {
        if (listening) return
        listening = true
        val channel = client.channel(TOPIC)
        val repo = DeviceStatusRepository(client)
        val reporter = DeviceStatusReporter(context)
        scope.launch {
            runCatching {
                channel.broadcastFlow<JsonObject>(EVENT).collect {
                    runCatching { repo.report(reporter.collect(deviceId, role)) }
                }
            }
        }
        scope.launch { runCatching { channel.subscribe() } }
    }

    /** Ask other connected devices to report their status now. */
    suspend fun ping(client: SupabaseClient) {
        val channel = client.channel(TOPIC)
        runCatching { channel.subscribe() }
        runCatching { channel.broadcast(EVENT, buildJsonObject { }) }
    }
}
