package com.tora_tech.smssync.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tora_tech.smssync.data.AppMode

@Composable
fun ModePickerScreen(onPick: (AppMode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("SMS Sync", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Choose this device's role. You can reinstall to change it.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = { onPick(AppMode.HOST) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Host — this phone has the SIM") }
        Button(
            onClick = { onPick(AppMode.CLIENT) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Client — read & reply from here") }
    }
}
