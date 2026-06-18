package com.tora_tech.smssync.device

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Helpers for keeping the host alive against Doze and OEM background killers.
 * Android does not let an app guarantee its own survival, so most of this is
 * about routing the user to the right system setting.
 */
object KeepAlive {

    private const val DURASPEED_PACKAGE = "com.mediatek.duraspeed"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Intent that asks the user to exempt us from battery optimization (Doze). */
    fun batteryExemptionIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
        }

    /** True when this device ships MediaTek DuraSpeed, which silently kills background apps. */
    fun isDuraSpeedPresent(context: Context): Boolean = runCatching {
        context.packageManager.getApplicationInfo(DURASPEED_PACKAGE, 0)
        true
    }.getOrDefault(false)

    /** Opens the DuraSpeed app so the user can disable it for SMS Sync, if available. */
    fun duraSpeedIntent(context: Context): Intent? =
        context.packageManager.getLaunchIntentForPackage(DURASPEED_PACKAGE)

    /** Best-effort OEM "autostart"/"background app" manager intent, or null if none resolves. */
    fun autostartIntent(context: Context): Intent? {
        val candidates = listOf(
            "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
            "com.letv.android.letvsafe" to "com.letv.android.letvsafe.AutobootManageActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity",
            "com.oneplus.security" to "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
        )
        for ((pkg, cls) in candidates) {
            val intent = Intent().apply { component = ComponentName(pkg, cls) }
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return null
    }

    /** This app's system settings page. Always resolves; used as a last-resort fallback. */
    fun appDetailsSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        }

}
