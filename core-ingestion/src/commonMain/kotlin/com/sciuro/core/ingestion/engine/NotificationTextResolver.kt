package com.sciuro.core.ingestion.engine

object NotificationTextResolver {
    fun resolveTextFallback(shortText: String, bigText: String, textLines: String): String =
        bigText.ifBlank { textLines.ifBlank { shortText } }
}
