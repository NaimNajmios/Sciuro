package com.sciuro.core.parsing.util

import com.sciuro.core.parsing.util.extractAccountNumber
import com.sciuro.core.parsing.util.extractAmount
import com.sciuro.core.parsing.util.extractMerchant
import com.sciuro.core.parsing.util.matchesAccountSuffix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegexExtractorsTest {

    @Test
    fun `extractAmount parses RM amounts`() {
        assertEquals(15.50, extractAmount("RM 15.50"))
        assertEquals(100.0, extractAmount("RM100.00"))
        assertEquals(1500.0, extractAmount("RM 1,500.00"))
        assertEquals(0.0, extractAmount("RM 0.00"))
        assertNull(extractAmount("No amount here"))
        assertNull(extractAmount("USD 50.00"))
    }

    @Test
    fun `extractMerchant parses outflow merchant after to, at, kepada, paid to`() {
        assertEquals("STARBUCKS", extractMerchant("payment to STARBUCKS was successful"))
        assertEquals("MCDONALDS", extractMerchant("used at MCDONALDS on 10/10"))
        assertEquals("TENAGA NASIONAL", extractMerchant("bayaran kepada TENAGA NASIONAL."))
        assertEquals("WARUNG PAK ALI", extractMerchant("paid to WARUNG PAK ALI."))
    }

    @Test
    fun `extractMerchant parses inflow merchant after from, dari`() {
        assertEquals("AHMAD ALI", extractMerchant("received RM 50.00 from AHMAD ALI."))
        assertEquals("SIAU KEE", extractMerchant("diterima dari SIAU KEE."))
    }

    @Test
    fun `extractMerchant returns null for text without merchant pattern`() {
        assertNull(extractMerchant("RM 100.00 has been credited"))
    }

    @Test
    fun `extractAccountNumber parses English AC pattern`() {
        assertEquals("1234567890", extractAccountNumber("transferred to A/C 1234567890"))
        assertEquals("9876543210", extractAccountNumber("to A/C: 9876543210"))
    }

    @Test
    fun `extractAccountNumber parses Account No pattern`() {
        assertEquals("1234567890", extractAccountNumber("deposited to Account No 1234567890"))
    }

    @Test
    fun `extractAccountNumber parses Acc pattern`() {
        assertEquals("1234567890", extractAccountNumber("to Acc 1234567890"))
    }

    @Test
    fun `extractAccountNumber parses English ending pattern`() {
        assertEquals("1234", extractAccountNumber("account ending 1234"))
        assertEquals("5678", extractAccountNumber("credited to your account ending 5678."))
    }

    @Test
    fun `extractAccountNumber parses Malay berakhir pattern`() {
        assertEquals("1234", extractAccountNumber("akaun anda berakhir 1234"))
        assertEquals("5678", extractAccountNumber("masuk ke dalam akaun anda berakhir 5678."))
    }

    @Test
    fun `extractAccountNumber handles masked numbers`() {
        assertEquals("****7890", extractAccountNumber("A/C ****7890"))
        assertEquals("XXXX4321", extractAccountNumber("Account XXXX4321"))
    }

    @Test
    fun `extractAccountNumber returns null for text without account number`() {
        assertNull(extractAccountNumber("Payment to STARBUCKS was successful"))
        assertNull(extractAccountNumber("No account reference here"))
    }

    @Test
    fun `extractAccountNumber does not match numbers shorter than 4 digits`() {
        assertNull(extractAccountNumber("account ending 789"))
    }

    @Test
    fun `matchesAccountSuffix matches exact digits`() {
        assertEquals(true, matchesAccountSuffix("1234", "1234"))
    }

    @Test
    fun `matchesAccountSuffix matches suffix of longer number`() {
        assertEquals(true, matchesAccountSuffix("5678", "601212345678"))
    }

    @Test
    fun `matchesAccountSuffix ignores mask characters`() {
        assertEquals(true, matchesAccountSuffix("****7890", "1234567890"))
        assertEquals(true, matchesAccountSuffix("XXXX7890", "1234567890"))
    }

    @Test
    fun `matchesAccountSuffix returns false for mismatched suffixes`() {
        assertEquals(false, matchesAccountSuffix("9999", "1234567890"))
    }

    @Test
    fun `matchesAccountSuffix returns false for empty input`() {
        assertEquals(false, matchesAccountSuffix("", "1234"))
        assertEquals(false, matchesAccountSuffix("1234", ""))
        assertEquals(false, matchesAccountSuffix("abc", "1234"))
    }
}
