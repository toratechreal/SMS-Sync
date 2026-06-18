package com.tora_tech.smssync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tora_tech.smssync.data.DeviceStatusDto

private val OnlineGreen = Color(0xFF2E9E5B)
private const val ONLINE_WINDOW_MS = 3 * 60 * 1000L

/** Renders one device's telemetry (shared by the host dashboard and the client status screen). */
@Composable
fun DeviceStatusCard(
    status: DeviceStatusDto,
    modifier: Modifier = Modifier,
    isSelf: Boolean = false,
    onRemove: (() -> Unit)? = null,
) {
    val lastSeenMs = parseInstantMs(status.lastSeen)
    val online = lastSeenMs != null && System.currentTimeMillis() - lastSeenMs < ONLINE_WINDOW_MS

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    status.role.replaceFirstChar { it.uppercase() } +
                        if (isSelf) " · This device" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (online) OnlineGreen
                                else MaterialTheme.colorScheme.outline
                            )
                    ) {}
                    Text(
                        if (online) "Online" else lastSeenMs?.let { formatRelative(it) } ?: "—",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            status.phoneNumber?.let { InfoRow("Number", it) }
            status.carrier?.let { InfoRow("Carrier", it) }
            status.signalStrength?.let { InfoRow("Signal", "$it/4") }
            status.batteryLevel?.let {
                val charging = if (status.batteryCharging == true) " · Charging" else ""
                InfoRow("Battery", "$it%$charging")
            }
            if (onRemove != null) {
                TextButton(onClick = onRemove, modifier = Modifier.align(Alignment.End)) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
