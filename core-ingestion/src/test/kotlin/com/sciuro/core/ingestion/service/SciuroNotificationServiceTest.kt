package com.sciuro.core.ingestion.service

import android.app.Notification
import android.os.Bundle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SciuroNotificationServiceTest {

    @Test
    fun resolveText_uses_bigText_when_shortText_empty() {
        val extras = Bundle()
        extras.putString(Notification.EXTRA_TITLE, "Test Title")
        extras.putString(Notification.EXTRA_BIG_TEXT, "Full content that was only in big text")
        val notification = Notification()
        notification.extras = extras

        val result = SciuroNotificationService.resolveText(notification)

        assertEquals("Full content that was only in big text", result)
    }

    @Test
    fun resolveText_uses_textLines_when_bigText_empty() {
        val extras = Bundle()
        extras.putString(Notification.EXTRA_TITLE, "Test Title")
        extras.putString(Notification.EXTRA_TEXT, "")
        extras.putCharSequenceArray(Notification.EXTRA_TEXT_LINES, arrayOf("Line 1", "Line 2", "Line 3"))
        val notification = Notification()
        notification.extras = extras

        val result = SciuroNotificationService.resolveText(notification)

        assertEquals("Line 1\nLine 2\nLine 3", result)
    }

    @Test
    fun resolveText_falls_back_to_shortText_when_both_bigText_and_textLines_empty() {
        val extras = Bundle()
        extras.putString(Notification.EXTRA_TITLE, "Test Title")
        extras.putString(Notification.EXTRA_TEXT, "Short preview text")
        extras.putString(Notification.EXTRA_BIG_TEXT, "")
        val notification = Notification()
        notification.extras = extras

        val result = SciuroNotificationService.resolveText(notification)

        assertEquals("Short preview text", result)
    }

    @Test
    fun resolveText_prefers_bigText_over_textLines() {
        val extras = Bundle()
        extras.putString(Notification.EXTRA_TITLE, "Test Title")
        extras.putString(Notification.EXTRA_TEXT, "Short text")
        extras.putString(Notification.EXTRA_BIG_TEXT, "Full big text content")
        extras.putCharSequenceArray(Notification.EXTRA_TEXT_LINES, arrayOf("Line 1", "Line 2"))
        val notification = Notification()
        notification.extras = extras

        val result = SciuroNotificationService.resolveText(notification)

        assertEquals("Full big text content", result)
    }

    @Test
    fun resolveText_returns_blank_when_no_text_fields_populated() {
        val extras = Bundle()
        extras.putString(Notification.EXTRA_TITLE, "Test Title")
        val notification = Notification()
        notification.extras = extras

        val result = SciuroNotificationService.resolveText(notification)

        assertTrue(result.isBlank())
    }
}
