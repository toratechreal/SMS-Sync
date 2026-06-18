package com.tora_tech.smssync.ui

import android.text.format.DateUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Conversation-list timestamp: time today, weekday this week, else short date. */
fun formatListTimestamp(millis: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    return when {
        sameDay(now, then) ->
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))
        withinWeek(now, millis) ->
            SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis))
        else ->
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}

/** Centered cluster header inside a thread, e.g. "Tue, 11:39 PM". */
fun formatClusterTimestamp(millis: Long): String {
    val day = DateUtils.getRelativeTimeSpanString(
        millis,
        System.currentTimeMillis(),
        DateUtils.DAY_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL,
    )
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))
    return "$day, $time"
}

/** Parses a Supabase ISO timestamp (with offset) to epoch ms, or null. */
fun parseInstantMs(iso: String?): Long? =
    iso?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }

/** Localized relative time, e.g. "just now", "5 minutes ago", "2 hours ago". */
fun formatRelative(millis: Long): String {
    val now = System.currentTimeMillis()
    if (now - millis < DateUtils.MINUTE_IN_MILLIS) return "just now"
    return DateUtils.getRelativeTimeSpanString(
        millis, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun withinWeek(now: Calendar, millis: Long): Boolean =
    now.timeInMillis - millis < 7 * DateUtils.DAY_IN_MILLIS
