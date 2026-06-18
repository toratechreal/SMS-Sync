package com.tora_tech.smssync.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** First-launch prompt offering to enable contact-name sync (requests READ_CONTACTS). */
@Composable
fun ContactsPrompt(onEnable: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) onEnable() else onDismiss() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Show contact names?") },
        text = {
            Text(
                "Show names instead of phone numbers by reading the contacts of people " +
                    "you text. Only numbers you've already texted are uploaded to your own " +
                    "Supabase — not your whole address book.",
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    onEnable()
                } else {
                    launcher.launch(Manifest.permission.READ_CONTACTS)
                }
            }) { Text("Enable") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        },
    )
}
