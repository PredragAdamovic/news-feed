package dev.predrag.newsfeed.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Locale.US, not the device locale: the format is specified with English month names.
private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

private val DATE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US)

fun Instant.toDisplayDate(zone: ZoneId = ZoneId.systemDefault()): String =
    DATE_FORMATTER.withZone(zone).format(this)

fun Instant.toDisplayDateTime(zone: ZoneId = ZoneId.systemDefault()): String =
    DATE_TIME_FORMATTER.withZone(zone).format(this)
