package com.tora_tech.smssync.data

/** Digits-only key for matching numbers across formats; falls back to the raw string. */
fun normalizePhone(s: String): String = s.filter { it.isDigit() }.ifEmpty { s }
