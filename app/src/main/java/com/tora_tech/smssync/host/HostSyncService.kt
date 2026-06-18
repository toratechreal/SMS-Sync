package com.tora_tech.smssync.host

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tora_tech.smssync.R
import com.tora_tech.smssync.SmsSyncApplication
import com.tora_tech.smssync.contacts.ContactsUploader
import com.tora_tech.smssync.data.DeviceStatusRepository
import com.tora_tech.smssync.data.MessagesRepository
import com.tora_tech.smssync.data.OutgoingDto
import com.tora_tech.smssync.data.OutgoingRepository
import com.tora_tech.smssync.data.PingChannel
import com.tora_tech.smssync.data.SupabaseProvider
import com.tora_tech.smssync.device.DeviceStatusReporter
import com.tora_tech.smssync.sms.SmsReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections
import kotlin.time.Duration.Companion.milliseconds

/**
 * Long-running host worker: keeps a Realtime subscription to the reply queue,
 * sends queued replies through the SIM, backfills history once, and reports telemetry.
 */
class HostSyncService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = Collections.synchronizedSet(mutableSetOf<String>())
    @Volatile private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!started) {
            started = true
            scope.launch { run() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun run() {
        val app = application as SmsSyncApplication
        val config = app.configStore.current()
        val client = SupabaseProvider.from(config) ?: run { stopSelf(); return }

        val messages = MessagesRepository(client)
        val outgoing = OutgoingRepository(client)
        val deviceStatus = DeviceStatusRepository(client)
        val drainer = OutgoingDrainer(this, messages, outgoing)
        val reader = SmsReader(this)
        val reporter = DeviceStatusReporter(this)

        scope.launch { backfill(app, reader, messages) }
        scope.launch { telemetryLoop(reporter, deviceStatus, config.deviceId) }

        // Listen for "ping to refresh" requests and report status immediately.
        PingChannel.start(scope, client, this, config.deviceId, "host")

        // Backstop poll: re-fetch and drain the queue every 30s, independent of
        // realtime. Guarantees queued replies are sent even if the realtime
        // websocket stalls/drops without the service restarting.
        scope.launch {
            while (scope.isActive) {
                runCatching { drainPending(outgoing.fetchPending(), drainer) }
                delay(POLL_INTERVAL_MS.milliseconds)
            }
        }

        // Contact name sync (opt-in): upload names for texted numbers, periodically.
        scope.launch {
            while (scope.isActive) {
                runCatching { ContactsUploader.syncOnce(this@HostSyncService, app) }
                delay(CONTACTS_INTERVAL_MS.milliseconds)
            }
        }

        // Realtime: low-latency drain the moment a reply is enqueued.
        outgoing.observePending().collect { pending -> drainPending(pending, drainer) }
    }

    /** Sends each pending item at most once (shared by the realtime + poll paths). */
    private fun drainPending(pending: List<OutgoingDto>, drainer: OutgoingDrainer) {
        for (item in pending) {
            val id = item.id ?: continue
            if (!inFlight.add(id)) continue
            scope.launch {
                try {
                    drainer.sendOne(item)
                } finally {
                    inFlight.remove(id)
                }
            }
        }
    }

    private suspend fun backfill(
        app: SmsSyncApplication,
        reader: SmsReader,
        messages: MessagesRepository,
    ) {
        val config = app.configStore.current()
        if (config.backfillDone) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val history = reader.readSince(config.backfillCursor)
        messages.upsertAll(history)
        val maxTs = history.maxOfOrNull { it.smsTimestamp } ?: config.backfillCursor
        app.configStore.setBackfillCursor(maxTs)
        app.configStore.setBackfillDone(true)
    }

    private suspend fun telemetryLoop(
        reporter: DeviceStatusReporter,
        deviceStatus: DeviceStatusRepository,
        deviceId: String,
    ) {
        while (scope.isActive) {
            runCatching { deviceStatus.report(reporter.collect(deviceId, "host")) }
            delay(TELEMETRY_INTERVAL_MS.milliseconds)
        }
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        // IMPORTANCE_MIN keeps it silent and collapsed at the bottom of the shade —
        // the least intrusive a (mandatory) foreground-service notification can be.
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.host_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.host_running_title))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        private const val CHANNEL_ID = "host_sync"
        private const val NOTIF_ID = 1001
        private const val TELEMETRY_INTERVAL_MS = 60_000L
        private const val POLL_INTERVAL_MS = 30_000L
        private const val CONTACTS_INTERVAL_MS = 900_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, HostSyncService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HostSyncService::class.java))
        }
    }
}
