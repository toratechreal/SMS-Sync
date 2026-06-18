package com.tora_tech.smssync.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.tora_tech.smssync.data.DeviceStatusDto
import java.time.OffsetDateTime

/** Collects this device's telemetry (number, carrier, signal, battery) for the status table. */
class DeviceStatusReporter(private val context: Context) {

    fun collect(deviceId: String, role: String): DeviceStatusDto = DeviceStatusDto(
        deviceId = deviceId,
        role = role,
        phoneNumber = phoneNumber(),
        carrier = carrier(),
        signalStrength = signalLevel(),
        batteryLevel = batteryLevel(),
        batteryCharging = isCharging(),
        appVersion = appVersion(),
        // Set explicitly: the DB `default now()` only fires on INSERT, so an upsert
        // UPDATE would otherwise keep last_seen frozen at first-insert time.
        lastSeen = OffsetDateTime.now().toString(),
    )

    private fun hasPerm(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    // Permission is checked at runtime via hasPerm(...) and any SecurityException is
    // swallowed by runCatching; lint can't see through that, so suppress here.
    @SuppressLint("MissingPermission", "HardwareIds")
    @Suppress("DEPRECATION")
    private fun phoneNumber(): String? {
        if (!hasPerm(Manifest.permission.READ_PHONE_NUMBERS) &&
            !hasPerm(Manifest.permission.READ_PHONE_STATE)
        ) return null
        return runCatching {
            context.getSystemService(TelephonyManager::class.java)?.line1Number?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun carrier(): String? = runCatching {
        context.getSystemService(TelephonyManager::class.java)
            ?.networkOperatorName?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /** 0..4 signal bars; requires READ_PHONE_STATE on some OEMs. Handled via runCatching. */
    @SuppressLint("MissingPermission")
    private fun signalLevel(): Int? = runCatching {
        context.getSystemService(TelephonyManager::class.java)?.signalStrength?.level
    }.getOrNull()

    private fun batteryLevel(): Int? = runCatching {
        context.getSystemService(BatteryManager::class.java)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
    }.getOrNull()

    private fun isCharging(): Boolean? = runCatching {
        context.getSystemService(BatteryManager::class.java)?.isCharging
    }.getOrNull()

    private fun appVersion(): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName, PackageManager.PackageInfoFlags.of(0)
            ).versionName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }
    }.getOrNull()
}
