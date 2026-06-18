package com.tora_tech.smssync.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Tiny send-status label shown under outbound bubbles / in the conversation list. */
@Composable
fun StatusChip(status: MsgStatus, modifier: Modifier = Modifier) {
    val label = when (status) {
        MsgStatus.OUTBOX -> "Outbox"
        MsgStatus.SENT -> "Sent"
        MsgStatus.FAILED -> "Failed"
    }
    val color = when (status) {
        MsgStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}
