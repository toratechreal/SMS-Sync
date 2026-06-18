package com.tora_tech.smssync.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tora_tech.smssync.data.AppMode
import com.tora_tech.smssync.data.SyncMode
import com.tora_tech.smssync.ui.client.ConversationsScreen
import com.tora_tech.smssync.ui.client.DeviceStatusScreen
import com.tora_tech.smssync.ui.client.NewChatScreen
import com.tora_tech.smssync.ui.client.ScanScreen
import com.tora_tech.smssync.ui.client.ThreadScreen
import com.tora_tech.smssync.ui.host.HostScreen

/** Top-level router: reacts to config (mode + pairing) so transitions are automatic. */
@Composable
fun AppNav(main: MainViewModel) {
    val config by main.config.collectAsState()
    val cfg = config

    if (cfg == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    when {
        cfg.mode == null -> ModePickerScreen(onPick = { main.setMode(it) })

        cfg.mode == AppMode.HOST -> {
            val host: HostViewModel = viewModel()
            HostScreen(config = cfg, main = main, host = host)
        }

        cfg.mode == AppMode.CLIENT && !cfg.isPaired ->
            ScanScreen(onPaired = { url, key -> main.saveClientPairing(url, key) })

        else -> ClientNav(
            main = main,
            syncMode = cfg.syncMode,
            contactsEnabled = cfg.contactsSyncEnabled,
        )
    }

    // First-launch prompt to enable contact-name sync (host + client).
    if (cfg.mode != null && cfg.isPaired && !cfg.contactsPromptShown) {
        ContactsPrompt(
            onEnable = { main.setContactsSyncEnabled(true) },
            onDismiss = { main.markContactsPromptShown() },
        )
    }
}

@Composable
private fun ClientNav(main: MainViewModel, syncMode: SyncMode, contactsEnabled: Boolean) {
    val context = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        main.startClientServices()
    }

    val vm: MessagesViewModel = viewModel()
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "conversations") {
        composable("conversations") {
            ConversationsScreen(
                vm = vm,
                onOpen = { address -> nav.navigate("thread/${Uri.encode(address)}") },
                onShowStatus = { nav.navigate("devices") },
                onNewChat = { nav.navigate("new") },
            )
        }
        composable("new") {
            NewChatScreen(
                onPicked = { address ->
                    nav.navigate("thread/${Uri.encode(address)}") {
                        popUpTo("new") { inclusive = true }
                    }
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable("thread/{address}") { entry ->
            val address = Uri.decode(entry.arguments?.getString("address").orEmpty())
            ThreadScreen(vm = vm, address = address, onBack = { nav.popBackStack() })
        }
        composable("devices") {
            DeviceStatusScreen(
                vm = vm,
                syncMode = syncMode,
                onSetSyncMode = { main.setSyncMode(it) },
                contactsEnabled = contactsEnabled,
                onSetContactsEnabled = { main.setContactsSyncEnabled(it) },
                onBack = { nav.popBackStack() },
            )
        }
    }
}
