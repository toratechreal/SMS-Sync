package com.tora_tech.smssync.device

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.tora_tech.smssync.SmsSyncApplication
import com.tora_tech.smssync.client.ClientSyncService
import com.tora_tech.smssync.client.MessageNotifier
import com.tora_tech.smssync.contacts.ContactsUploader
import com.tora_tech.smssync.data.AppMode
import com.tora_tech.smssync.data.DeviceStatusRepository
import com.tora_tech.smssync.data.MessagesRepository
import com.tora_tech.smssync.data.OutgoingRepository
import com.tora_tech.smssync.data.SupabaseProvider
import com.tora_tech.smssync.data.SyncMode
import com.tora_tech.smssync.host.HostSyncService
import com.tora_tech.smssync.host.OutgoingDrainer
import java.util.concurrent.TimeUnit

/** Periodic safety net: restarts the host service if an OEM killer took it down. */
class ServiceWatchdogWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as SmsSyncApplication
        val config = app.configStore.current()
        if (!config.isPaired) return Result.success()
        val client = SupabaseProvider.from(config) ?: return Result.success()

        // 15-min backstop: refresh telemetry every run, in both modes.
        val role = if (config.mode == AppMode.HOST) "host" else "client"
        runCatching {
            DeviceStatusRepository(client).report(
                DeviceStatusReporter(applicationContext).collect(config.deviceId, role)
            )
        }

        // Upload contact names for texted numbers (opt-in; no-op if disabled).
        runCatching { ContactsUploader.syncOnce(applicationContext, app) }

        when (config.mode) {
            AppMode.HOST -> {
                if (config.syncMode == SyncMode.REALTIME) {
                    // Revive the foreground service if it was killed (may be blocked from
                    // the background without a battery exemption; ignore if so).
                    runCatching { HostSyncService.start(applicationContext) }
                } else {
                    // Battery saver: drain the reply queue inline, no foreground service.
                    val drainer = OutgoingDrainer(
                        applicationContext,
                        MessagesRepository(client),
                        OutgoingRepository(client),
                    )
                    runCatching { drainer.drainOnce() }
                }
            }

            AppMode.CLIENT -> {
                if (config.syncMode == SyncMode.REALTIME) {
                    runCatching { ClientSyncService.start(applicationContext) }
                } else {
                    // Battery saver: fetch + notify new messages inline, no foreground service.
                    val all = runCatching { MessagesRepository(client).fetchAll() }
                        .getOrNull().orEmpty()
                    runCatching { MessageNotifier(applicationContext).notifyNewInbound(app.configStore, all) }
                }
            }

            null -> {}
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "host_watchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(
                15, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

    }
}
