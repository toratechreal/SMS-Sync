package com.tora_tech.smssync.data

/** The two roles a single install can take. Chosen once on first launch. */
enum class AppMode { HOST, CLIENT }

/**
 * How this device keeps in sync / delivers notifications.
 * REALTIME = foreground service (instant, minimal ongoing notification).
 * BATTERY_SAVER = ~15-min WorkManager checks (no persistent notification, delayed).
 */
enum class SyncMode { REALTIME, BATTERY_SAVER }

/** Message direction stored in the `messages` table. */
object Direction {
    const val INBOUND = "inbound"
    const val OUTBOUND = "outbound"
}

/** Status values for the `outgoing` reply queue. */
object OutgoingStatus {
    const val PENDING = "pending"
    const val SENT = "sent"
    const val FAILED = "failed"
}
