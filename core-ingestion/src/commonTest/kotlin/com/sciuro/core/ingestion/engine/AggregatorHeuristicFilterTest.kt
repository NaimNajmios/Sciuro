package com.sciuro.core.ingestion.engine

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AggregatorHeuristicFilterTest {

    @Test
    fun `isFinancial should return false for general spam and unrelated texts`() {
        assertFalse(AggregatorHeuristicFilter.isFinancial("Welcome to my farm", "We have lots of animals."))
        assertFalse(AggregatorHeuristicFilter.isFinancial("Join the army", "Be a hero."))
        assertFalse(AggregatorHeuristicFilter.isFinancial("It is very warm today", "Enjoy the sun!"))
        assertFalse(AggregatorHeuristicFilter.isFinancial("Your package has arrived", "Pick it up at the lobby."))
        assertFalse(AggregatorHeuristicFilter.isFinancial("Meeting reminder", "Don't forget the standup."))
    }

    @Test
    fun `isFinancial should return true for valid English financial keywords`() {
        assertTrue(AggregatorHeuristicFilter.isFinancial("Bank", "You have spent RM 50.00 at Starbucks."))
        assertTrue(AggregatorHeuristicFilter.isFinancial("Transfer Alert", "Funds received from John Doe."))
        assertTrue(AggregatorHeuristicFilter.isFinancial("Transaction Alert", "You made a transaction of 50.00"))
        assertTrue(AggregatorHeuristicFilter.isFinancial("Receipt", "Here is your receipt for your recent purchase."))
        assertTrue(AggregatorHeuristicFilter.isFinancial("CIMB Notification", "Your account has been debited."))
        assertTrue(AggregatorHeuristicFilter.isFinancial("Payment", "Payment successful."))
    }

    @Test
    fun `isFinancial should return true for valid BM financial keywords`() {
        assertTrue(AggregatorHeuristicFilter.isFinancial("Notifikasi", "Pemindahan sebanyak 100.00 berjaya."))
        assertTrue(AggregatorHeuristicFilter.isFinancial("Alert", "Bayaran kepada TNB berjaya."))
        assertTrue(AggregatorHeuristicFilter.isFinancial("Penolakan", "Akaun anda telah ditolak RM 50."))
        assertTrue(AggregatorHeuristicFilter.isFinancial("Transaksi", "Transaksi anda telah diluluskan."))
    }

    @Test
    fun `isFinancial should return true for Malaysian payment networks`() {
        assertTrue(AggregatorHeuristicFilter.isFinancial("DuitNow", "DuitNow transfer to Ali successful."))
        assertTrue(AggregatorHeuristicFilter.isFinancial("JomPAY", "JomPAY bill payment successful."))
        assertTrue(AggregatorHeuristicFilter.isFinancial("FPX", "Your FPX transaction is complete."))
        assertTrue(AggregatorHeuristicFilter.isFinancial("SPayLater", "Your SPayLater bill is due."))
    }

    @Test
    fun `isFinancial should be case insensitive`() {
        assertTrue(AggregatorHeuristicFilter.isFinancial("Rm 50", "spent"))
        assertTrue(AggregatorHeuristicFilter.isFinancial("rM 50", "spent"))
        assertTrue(AggregatorHeuristicFilter.isFinancial("RM 50", "spent"))
        assertTrue(AggregatorHeuristicFilter.isFinancial("rm 50", "spent"))
        assertTrue(AggregatorHeuristicFilter.isFinancial("DUITNOW", "Transfer"))
        assertTrue(AggregatorHeuristicFilter.isFinancial("duitnow", "Transfer"))
    }
}
