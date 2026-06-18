package com.tora_tech.smssync.pairing

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Camera-permission gate + QR scanner that decodes a [PairingPayload] and reports
 * the Supabase url/key. Shared by host self-config and client pairing.
 */
@Composable
fun ConfigQrScanner(
    caption: String,
    onPaired: (String, String) -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCamera = it }

    LaunchedEffect(Unit) {
        if (!hasCamera) launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasCamera) {
        Box(Modifier.fillMaxSize()) {
            QrScannerView(modifier = Modifier.fillMaxSize()) { raw ->
                PairingPayload.decode(raw)?.let { onPaired(it.url, it.key) }
            }
            Text(
                caption,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(24.dp),
            )
            if (onCancel != null) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                ) { Text("Cancel", color = Color.White) }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text("Camera needed", style = MaterialTheme.typography.headlineSmall)
            Text(
                "SMS Sync uses the camera to scan the pairing QR code.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("Allow camera")
            }
            if (onCancel != null) {
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}
