package com.sciuro.core.ledger.security

object IntegrityCheckPolicy {
    const val CHECK_INTERVAL_MS = 7L * 24L * 60L * 60L * 1000L

    fun isDue(lastCheckMs: Long, nowMs: Long): Boolean =
        lastCheckMs <= 0L || nowMs - lastCheckMs >= CHECK_INTERVAL_MS
}
