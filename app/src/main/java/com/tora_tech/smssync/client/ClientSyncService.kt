package com.tora_tech.smssync.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tora_tech.smssync.SmsSyncApplication
import com.tora_tech.smssync.contacts.ContactsUploader
import com.tora_tech.smssync.data.DeviceStatusRepository
import com.tora_tech.smssync.data.MessagesRepository
import com.tora_tech.smssync.data.PingChannel
import com.tora_tech.smssync.data.SupabaseProvider
import com.tora_tech.smssync.device.DeviceStatusReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Real-time client worker: holds the messages Realtime subscription so new-message
 * notifications fire even when the app is closed. Also reports telemetry so the host
 * sees this client. Used only in [com.tora_tech.smssync.data.SyncMode.REALTIME].
 */
class ClientSyncService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
        val deviceStatus = DeviceStatusRepository(client)
        val reporter = DeviceStatusReporter(this)
        val notifier = MessageNotifier(this)
        notifier.ensureChannel()

        // Listen for "ping to refresh" requests and report status immediately.
        PingChannel.start(scope, client, this, config.deviceId, "client")

        scope.launch {
            while (scope.isActive) {
                runCatching { deviceStatus.report(reporter.collect(config.deviceId, "client")) }
                delay(TELEMETRY_INTERVAL_MS.milliseconds)
            }
        }

        // Contact name sync (opt-in): upload names for texted numbers, periodically.
        scope.launch {
            while (scope.isActive) {
                runCatching { ContactsUploader.syncOnce(this@ClientSyncService, app) }
                delay(CONTACTS_INTERVAL_MS.milliseconds)
            }
        }

        messages.observeAll()
            .catch { }
            .collect { all ->
                runCatching { notifier.notifyNewInbound(app.configStore, all) }
            }
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID, "SMS Sync", NotificationManager.IMPORTANCE_MIN,
        ).apply {
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Sync")
            .setSmallIcon(com.tora_tech.smssync.R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        private const val CHANNEL_ID = "client_sync"
        private const val NOTIF_ID = 2001
        private const val TELEMETRY_INTERVAL_MS = 60_000L
        private const val CONTACTS_INTERVAL_MS = 900_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, ClientSyncService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ClientSyncService::class.java))
        }
    }
}
