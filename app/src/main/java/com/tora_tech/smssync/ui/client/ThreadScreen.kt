package com.tora_tech.smssync.ui.client

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tora_tech.smssync.ui.ContactAvatar
import com.tora_tech.smssync.ui.MessagesViewModel
import com.tora_tech.smssync.ui.StatusChip
import com.tora_tech.smssync.ui.ThreadItem
import com.tora_tech.smssync.ui.formatClusterTimestamp

private const val CLUSTER_GAP_MS = 15 * 60 * 1000L

/** Opens the system add-contact screen, prefilled with the number. */
private fun saveContact(context: Context, number: String) {
    runCatching {
        context.startActivity(
            Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, number)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    vm: MessagesViewModel,
    address: String,
    onBack: () -> Unit,
) {
    val messages by vm.messages.collectAsState()
    val outgoing by vm.outgoing.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val contactName = contacts[vm.normalizeAddress(address)]
    val thread = remember(messages, outgoing, address) { vm.thread(messages, outgoing, address) }
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(thread.size) {
        if (thread.isNotEmpty()) listState.animateScrollToItem(thread.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ContactAvatar(address = contactName ?: address, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                contactName ?: address,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (contactName != null) {
                                Text(
                                    address,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (contactName == null) {
                        TextButton(onClick = { saveContact(context, address) }) {
                            Text("Save")
                        }
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
            ) {
                itemsIndexed(thread, key = { _, m -> m.key }) { i, item ->
                    val prev = thread.getOrNull(i - 1)
                    val next = thread.getOrNull(i + 1)
                    val dir = item.outbound
                    val start = prev == null ||
                        prev.outbound != dir ||
                        item.timestamp - prev.timestamp > CLUSTER_GAP_MS
                    val end = next == null ||
                        next.outbound != dir ||
                        next.timestamp - item.timestamp > CLUSTER_GAP_MS
                    val showStamp = prev == null ||
                        item.timestamp - prev.timestamp > CLUSTER_GAP_MS

                    if (showStamp) {
                        Text(
                            text = formatClusterTimestamp(item.timestamp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    MessageBubble(item = item, address = address, start = start, end = end)
                    Spacer(Modifier.size(if (end) 8.dp else 2.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    maxLines = 5,
                )
                val canSend = draft.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            if (canSend) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = {
                            vm.sendReply(address, draft)
                            draft = ""
                        },
                        enabled = canSend,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    item: ThreadItem,
    address: String,
    start: Boolean,
    end: Boolean,
) {
    if (item.outbound) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(bubbleShape(outbound = true, start = start, end = end))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(item.body, color = MaterialTheme.colorScheme.onPrimary)
            }
            if (end && item.status != null) {
                StatusChip(item.status, modifier = Modifier.padding(top = 2.dp, end = 4.dp))
            }
        }
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Box(Modifier.size(36.dp)) {
                if (end) ContactAvatar(address = address, size = 36.dp)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(bubbleShape(outbound = false, start = start, end = end))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(item.body, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

/** Corner radii mirroring QUIK's message_{in,out}_{first,middle,last,only} drawables. */
private fun bubbleShape(outbound: Boolean, start: Boolean, end: Boolean): RoundedCornerShape {
    val r = 18.dp
    val t = 4.dp
    if (start && end) return RoundedCornerShape(r) // only
    return if (!outbound) {
        // incoming: tight corners on the left edge (topStart, topEnd, bottomEnd, bottomStart)
        when {
            start -> RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = r, bottomStart = t)
            end -> RoundedCornerShape(topStart = t, topEnd = r, bottomEnd = r, bottomStart = r)
            else -> RoundedCornerShape(topStart = t, topEnd = r, bottomEnd = r, bottomStart = t)
        }
    } else {
        // outgoing: tight corners on the right edge
        when {
            start -> RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = t, bottomStart = r)
            end -> RoundedCornerShape(topStart = r, topEnd = t, bottomEnd = r, bottomStart = r)
            else -> RoundedCornerShape(topStart = r, topEnd = t, bottomEnd = t, bottomStart = r)
        }
    }
}
