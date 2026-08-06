package com.sciuro.core.ledger.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuarantineFilesTest {

    @Test
    fun `quarantine filename matches prefix pattern`() {
        assertTrue(QuarantineFiles.isQuarantineFileName("sciuro.db.quarantined.1720000000000"))
        assertTrue(QuarantineFiles.isQuarantineFileName("sciuro.db.quarantined.1"))
        assertFalse(QuarantineFiles.isQuarantineFileName("sciuro.db"))
        assertFalse(QuarantineFiles.isQuarantineFileName("sciuro.db-wal"))
        assertFalse(QuarantineFiles.isQuarantineFileName("other.db.quarantined.123"))
        assertFalse(QuarantineFiles.isQuarantineFileName(""))
    }

    @Test
    fun `quarantine filename embeds the timestamp`() {
        assertEquals("sciuro.db.quarantined.987654321", QuarantineFiles.quarantineFileName(987654321L))
    }

    @Test
    fun `timestamp round-trips from a quarantine name`() {
        assertEquals(987654321L, QuarantineFiles.timestampFromFileName("sciuro.db.quarantined.987654321"))
    }

    @Test
    fun `timestamp parsing tolerates malformed suffixes`() {
        assertNull(QuarantineFiles.timestampFromFileName("sciuro.db.quarantined.not-a-number"))
        assertNull(QuarantineFiles.timestampFromFileName("sciuro.db"))
        assertNull(QuarantineFiles.timestampFromFileName("sciuro.db.quarantined."))
    }
}
