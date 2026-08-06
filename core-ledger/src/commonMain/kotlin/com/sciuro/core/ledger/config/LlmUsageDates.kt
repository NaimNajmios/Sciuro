package com.sciuro.core.ledger.config

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

fun currentLocalDateKey(): String =
    Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

fun localDateKeyFor(timestampMs: Long, timeZone: TimeZone): String {
    return Instant.fromEpochMilliseconds(timestampMs).toLocalDateTime(timeZone)
        .let { "%04d-%02d-%02d".format(it.year, it.month.ordinal + 1, it.dayOfMonth) }
}
