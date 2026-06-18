package com.tora_tech.smssync.ui.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tora_tech.smssync.data.SyncMode
import com.tora_tech.smssync.ui.ContactsToggleCard
import com.tora_tech.smssync.ui.DeviceStatusCard
import com.tora_tech.smssync.ui.MessagesViewModel

/** Settings: sync mode, contact-name sync, and paired-device status. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStatusScreen(
    vm: MessagesViewModel,
    syncMode: SyncMode,
    onSetSyncMode: (SyncMode) -> Unit,
    contactsEnabled: Boolean,
    onSetContactsEnabled: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val statuses by vm.statuses.collectAsState()
    val sorted = remember(statuses) { statuses.sortedByDescending { it.role == "host" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val refreshing by vm.refreshing.collectAsState()
                    TextButton(onClick = { vm.refresh() }, enabled = !refreshing) {
                        Text(if (refreshing) "Refreshing…" else "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SyncModeCard(syncMode, onSetSyncMode) }
            item { ContactsToggleCard(enabled = contactsEnabled, onSetEnabled = onSetContactsEnabled) }
            item {
                Text(
                    "Paired devices",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
            if (sorted.isEmpty()) {
                item {
                    Text(
                        "No device has reported in yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            } else {
                items(sorted, key = { it.deviceId }) { s ->
                    val self = s.deviceId == vm.deviceId
                    DeviceStatusCard(
                        status = s,
                        isSelf = self,
                        onRemove = if (self) null else ({ vm.removeDevice(s.deviceId) }),
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncModeCard(syncMode: SyncMode, onSetSyncMode: (SyncMode) -> Unit) {
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
                        "Real-time — instant notifications (keeps a silent background notification)."
                    else
                        "Battery saver — checks about every 15 min, no background notification.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = realtime,
                onCheckedChange = {
                    onSetSyncMode(if (it) SyncMode.REALTIME else SyncMode.BATTERY_SAVER)
                },
            )
        }
    }
}
