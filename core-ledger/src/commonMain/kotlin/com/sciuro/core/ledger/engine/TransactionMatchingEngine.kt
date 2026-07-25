package com.sciuro.core.ledger.engine

import com.sciuro.core.ledger.db.SciuroDatabase

class TransactionMatchingEngine(
    private val database: SciuroDatabase
) {
    fun getIneligibleTransactionIds(): Set<String> {
        return database.transferLinkQueries.selectAllTransferLinks().executeAsList()
            .flatMap { listOf(it.outflow_transaction_id, it.inflow_transaction_id) }
            .toSet()
    }
}
