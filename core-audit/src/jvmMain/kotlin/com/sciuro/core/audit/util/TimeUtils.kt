package com.sciuro.core.audit.util

import java.util.UUID

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
actual fun generateUuid(): String = UUID.randomUUID().toString()
