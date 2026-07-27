package com.sciuro.core.ingestion.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationTextResolverTest {

    @Test
    fun `resolveTextFallback uses bigText when available`() {
        val result = NotificationTextResolver.resolveTextFallback(
            shortText = "Short text",
            bigText = "Full big text content",
            textLines = "Lines content"
        )
        assertEquals("Full big text content", result)
    }

    @Test
    fun `resolveTextFallback uses textLines when bigText blank`() {
        val result = NotificationTextResolver.resolveTextFallback(
            shortText = "Short text",
            bigText = "",
            textLines = "Line 1\nLine 2\nLine 3"
        )
        assertEquals("Line 1\nLine 2\nLine 3", result)
    }

    @Test
    fun `resolveTextFallback falls back to shortText when bigText and textLines blank`() {
        val result = NotificationTextResolver.resolveTextFallback(
            shortText = "Short text",
            bigText = "",
            textLines = ""
        )
        assertEquals("Short text", result)
    }

    @Test
    fun `resolveTextFallback prefers bigText over textLines when both present`() {
        val result = NotificationTextResolver.resolveTextFallback(
            shortText = "Short text",
            bigText = "Full big text",
            textLines = "Line 1\nLine 2"
        )
        assertEquals("Full big text", result)
    }

    @Test
    fun `resolveTextFallback returns blank when all fields blank`() {
        val result = NotificationTextResolver.resolveTextFallback(
            shortText = "",
            bigText = "",
            textLines = ""
        )
        assertTrue(result.isBlank())
    }

    @Test
    fun `resolveTextFallback handles bigText being null equivalent`() {
        val result = NotificationTextResolver.resolveTextFallback(
            shortText = "Short",
            bigText = "",
            textLines = ""
        )
        assertEquals("Short", result)
    }
}
