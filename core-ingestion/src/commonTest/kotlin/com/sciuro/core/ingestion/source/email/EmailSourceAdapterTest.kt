package com.sciuro.core.ingestion.source.email

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class EmailSourceAdapterTest {

    private val adapter = EmailSourceAdapter()

    @Test
    fun `source type is EMAIL`() {
        assertEquals(SourceType.EMAIL, adapter.sourceType)
    }

    @Test
    fun `emitEmail surfaces event on observeEvents`(): Unit = runBlocking {
        val event = RawEvent(
            id = "evt-1",
            sourceType = SourceType.EMAIL,
            sourcePackageOrAddress = "user@gmail.com",
            title = "Transaction receipt",
            text = "RM 50.00 paid at MyGrocer",
            timestamp = 1234567890L
        )

        adapter.emitEmail(event)
        val received = adapter.observeEvents().first()
        assertEquals(event.id, received.id)
        assertEquals(SourceType.EMAIL, received.sourceType)
    }
}
