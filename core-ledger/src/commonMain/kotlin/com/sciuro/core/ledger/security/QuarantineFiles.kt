package com.sciuro.core.ledger.security

object QuarantineFiles {
    const val QUARANTINE_PREFIX = "sciuro.db.quarantined."

    fun isQuarantineFileName(name: String): Boolean = name.startsWith(QUARANTINE_PREFIX)

    fun quarantineFileName(timestampMs: Long): String = "$QUARANTINE_PREFIX$timestampMs"

    fun timestampFromFileName(name: String): Long? =
        if (isQuarantineFileName(name)) name.substring(QUARANTINE_PREFIX.length).toLongOrNull() else null
}
