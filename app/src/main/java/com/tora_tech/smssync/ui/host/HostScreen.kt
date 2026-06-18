package com.tora_tech.smssync.ui.host

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tora_tech.smssync.data.AppConfig
import com.tora_tech.smssync.data.SyncMode
import com.tora_tech.smssync.device.KeepAlive
import com.tora_tech.smssync.pairing.ConfigQrScanner
import com.tora_tech.smssync.pairing.PairingPayload
import com.tora_tech.smssync.pairing.QrGenerator
import com.tora_tech.smssync.ui.ContactsToggleCard
import com.tora_tech.smssync.ui.DeviceStatusCard
import com.tora_tech.smssync.ui.HostViewModel
import com.tora_tech.smssync.ui.MainViewModel

@Composable
fun HostScreen(
    config: AppConfig,
    main: MainViewModel,
    host: HostViewModel,
) {
    if (!config.isPaired) {
        HostSetup(onSave = { url, key -> main.saveHostPairing(url, key) })
    } else {
        HostDashboard(config = config, main = main, host = host)
    }
}

@Composable
private fun HostSetup(onSave: (String, String) -> Unit) {
    var scanning by remember { mutableStateOf(false) }
    if (scanning) {
        ConfigQrScanner(
            caption = "Scan a config QR (Supabase URL + anon key)",
            onPaired = { url, key -> onSave(url, key) },
            onCancel = { scanning = false },
        )
        return
    }

    var url by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Host setup", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Scan a config QR (from the included web tool), or paste your own " +
                "Supabase project URL and anon key.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(
            onClick = { scanning = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Scan config QR") }
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Supabase URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("Supabase anon key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onSave(url, key) },
            enabled = url.isNotBlank() && key.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save & start host") }
    }
}

@Composable
private fun HostDashboard(
    config: AppConfig,
    main: MainViewModel,
    host: HostViewModel,
) {
    val context = LocalContext.current
    val statuses by host.statuses.collectAsState()

    val permissions = remember { hostPermissions() }
    var granted by remember { mutableStateOf(hasAll(context, permissions)) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result.all { it.value }
        if (granted) main.startHostServices()
    }

    // Ensure the host service is running whenever the dashboard is shown with
    // permissions already granted (e.g. reopening an already-configured host).
    LaunchedEffect(granted) {
        if (granted) main.startHostServices()
    }

    val qrBitmap = remember(config.supabaseUrl, config.supabaseKey) {
        runCatching {
            QrGenerator.encode(
                PairingPayload(config.supabaseUrl!!, config.supabaseKey!!).encode()
            )
        }.getOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Host", style = MaterialTheme.typography.headlineSmall)

        if (!granted) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Permissions needed", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Grant SMS and phone permissions so this device can capture and send messages.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = { launcher.launch(permissions) }) { Text("Grant permissions") }
                }
            }
        }

        if (!config.keepAliveDismissed) {
            KeepAliveCard(context, onDismiss = { main.dismissKeepAlive() })
        }

        HostSyncModeCard(config.syncMode, onSet = { main.setSyncMode(it) })

        ContactsToggleCard(
            enabled = config.contactsSyncEnabled,
            onSetEnabled = { main.setContactsSyncEnabled(it) },
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Pair a client", style = MaterialTheme.typography.titleMedium)
                Text("Scan this from the client device.", style = MaterialTheme.typography.bodyMedium)
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Pairing QR code",
                        modifier = Modifier.size(240.dp),
                    )
                }
            }
        }

        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Paired devices",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            val refreshing by host.refreshing.collectAsState()
            TextButton(onClick = { host.refresh() }, enabled = !refreshing) {
                Text(if (refreshing) "Refreshing…" else "Refresh")
            }
        }
        if (statuses.isEmpty()) {
            Text("No devices have reported yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            statuses.forEach { s ->
                val self = s.deviceId == host.deviceId
                DeviceStatusCard(
                    status = s,
                    isSelf = self,
                    onRemove = if (self) null else ({ host.removeDevice(s.deviceId) }),
                )
            }
        }
    }
}

@Composable
private fun KeepAliveCard(context: Context, onDismiss: () -> Unit) {
    val exempt = remember { KeepAlive.isIgnoringBatteryOptimizations(context) }
    val duraSpeed = remember { KeepAlive.isDuraSpeedPresent(context) }
    val autostart = remember { KeepAlive.autostartIntent(context) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Keep host alive", style = MaterialTheme.typography.titleMedium)
            Text(
                "Android may kill background apps. Apply these so syncing keeps working.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!exempt) {
                OutlinedButton(onClick = {
                    launchSafely(context, KeepAlive.batteryExemptionIntent(context))
                }) { Text("Ignore battery optimization") }
            } else {
                Text("✓ Battery optimization already disabled.",
                    style = MaterialTheme.typography.bodySmall)
            }
            // Only OEMs with an autostart manager have this; skip on stock Android.
            if (autostart != null) {
                OutlinedButton(onClick = { launchSafely(context, autostart) }) {
                    Text("Open autostart settings")
                }
            }
            if (duraSpeed) {
                OutlinedButton(onClick = {
                    launchSafely(context, KeepAlive.duraSpeedIntent(context))
                }) { Text("Disable DuraSpeed (MediaTek)") }
            }
            // Always-available fallback so there's never a dead end.
            OutlinedButton(onClick = {
                launchSafely(context, KeepAlive.appDetailsSettingsIntent(context))
            }) { Text("Open app settings") }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) { Text("Dismiss") }
        }
    }
}

@Composable
private fun HostSyncModeCard(syncMode: SyncMode, onSet: (SyncMode) -> Unit) {
    val realtime = syncMode == SyncMode.REALTIME
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Sync mode", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (realtime)
                        "Real-time — sends replies instantly (keeps a silent background notification)."
                    else
                        "Battery saver — sends queued replies about every 15 min, no background notification. Incoming SMS still sync instantly.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = realtime,
                onCheckedChange = {
                    onSet(if (it) SyncMode.REALTIME else SyncMode.BATTERY_SAVER)
                },
            )
        }
    }
}

private fun hostPermissions(): Array<String> = buildList {
    add(Manifest.permission.RECEIVE_SMS)
    add(Manifest.permission.READ_SMS)
    add(Manifest.permission.SEND_SMS)
    add(Manifest.permission.READ_PHONE_STATE)
    add(Manifest.permission.READ_PHONE_NUMBERS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

private fun hasAll(context: Context, permissions: Array<String>): Boolean =
    permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

private fun launchSafely(context: Context, intent: Intent?) {
    if (intent == null) return
    runCatching { context.startActivity(intent) }
}
