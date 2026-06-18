package com.tora_tech.smssync.data

import java.util.concurrent.atomic.AtomicLong

private val channelCounter = AtomicLong(0)

/**
 * Builds a process-unique Realtime channel topic. supabase-kt keys channels by topic,
 * so two concurrent subscribers (e.g. ClientSyncService + MessagesViewModel) must use
 * distinct names or they collide.
 */
fun uniqueChannelName(prefix: String): String = "$prefix-${channelCounter.incrementAndGet()}"
