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

    @Test
    fun `resolveCustomExtrasFallback returns known key content for mapped package`() {
        val extras = mapOf(
            "full_desc" to "Successful payment of RM 1.00 to SITI FIKRIYAH BINTI I.R A. REF: QR82244072.",
            "another_key" to "longer non-financial debug string that should be ignored"
        )
        val result = NotificationTextResolver.resolveCustomExtrasFallback("com.maybank2u.life", extras)
        assertEquals("Successful payment of RM 1.00 to SITI FIKRIYAH BINTI I.R A. REF: QR82244072.", result)
    }

    @Test
    fun `resolveCustomExtrasFallback picks longest financial extra for unmapped package`() {
        val extras = mapOf(
            "custom_text" to "You've received RM 0.20 from SITI FIKRIYAH BINTI I.R ABDUL KHAWI. REF: 485956734Q.",
            "short_id" to "txn RM 5",
            "debug_log" to "some non-financial debug information"
        )
        val result = NotificationTextResolver.resolveCustomExtrasFallback("com.unknown.bank", extras)
        assertEquals("You've received RM 0.20 from SITI FIKRIYAH BINTI I.R ABDUL KHAWI. REF: 485956734Q.", result)
    }

    @Test
    fun `resolveCustomExtrasFallback returns blank when no financial extras found`() {
        val extras = mapOf(
            "soundName" to "default",
            "android.title" to "Some App",
            "debug_log" to "non financial debug string",
            "timestamp" to "2026-07-28T10:00:00Z"
        )
        val result = NotificationTextResolver.resolveCustomExtrasFallback("com.unknown.bank", extras)
        assertTrue(result.isBlank())
    }

    @Test
    fun `resolveCustomExtrasFallback excludes known noise keys and android prefix keys`() {
        val extras = mapOf(
            "android.title" to "Maybank2u: Scan & Pay",
            "soundName" to "default",
            "tranVerCode" to "12345",
            "custom_body" to "Payment of RM 50.00 to MERCHANT A was successful."
        )
        val result = NotificationTextResolver.resolveCustomExtrasFallback("com.unknown.bank", extras)
        assertEquals("Payment of RM 50.00 to MERCHANT A was successful.", result)
    }
}
