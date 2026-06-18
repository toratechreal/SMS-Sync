package com.tora_tech.smssync.ui.client

import androidx.compose.runtime.Composable
import com.tora_tech.smssync.pairing.ConfigQrScanner

@Composable
fun ScanScreen(onPaired: (String, String) -> Unit) {
    ConfigQrScanner(
        caption = "Point at the host's pairing QR",
        onPaired = onPaired,
    )
}
