package com.tora_tech.smssync.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tora_tech.smssync.SmsSyncApplication
import com.tora_tech.smssync.data.AppMode
import com.tora_tech.smssync.host.HostSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-arms the host worker after a reboot so syncing resumes without opening the app. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val pending = goAsync()
        val app = context.applicationContext as SmsSyncApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = app.configStore.current()
                if (config.mode == AppMode.HOST && config.isPaired) {
                    HostSyncService.start(context)
                    ServiceWatchdogWorker.schedule(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
