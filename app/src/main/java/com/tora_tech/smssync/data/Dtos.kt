package com.tora_tech.smssync.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A synced text message. Mirrors the `messages` table. */
@Serializable
data class MessageDto(
    val id: String? = null,
    val address: String,
    val body: String,
    val direction: String,
    @SerialName("sms_timestamp") val smsTimestamp: Long,
    @SerialName("dedupe_key") val dedupeKey: String,
    val read: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

/** A reply request the client enqueues and the host sends. Mirrors `outgoing`. */
@Serializable
data class OutgoingDto(
    val id: String? = null,
    val address: String,
    val body: String,
    val status: String = OutgoingStatus.PENDING,
    val error: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("sent_at") val sentAt: String? = null,
)

/** A contact name for a texted number. Mirrors `contacts`. */
@Serializable
data class ContactDto(
    val phone: String,
    val name: String,
    @SerialName("source_device_id") val sourceDeviceId: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** Periodic per-device telemetry. Mirrors `device_status`. */
@Serializable
data class DeviceStatusDto(
    @SerialName("device_id") val deviceId: String,
    val role: String,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val carrier: String? = null,
    @SerialName("signal_strength") val signalStrength: Int? = null,
    @SerialName("battery_level") val batteryLevel: Int? = null,
    @SerialName("battery_charging") val batteryCharging: Boolean? = null,
    @SerialName("app_version") val appVersion: String? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
)
