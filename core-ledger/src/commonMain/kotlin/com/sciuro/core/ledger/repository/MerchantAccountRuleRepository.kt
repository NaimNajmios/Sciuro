package com.sciuro.core.ledger.repository

import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase

class MerchantAccountRuleRepository(
    private val database: SciuroDatabase
) {
    suspend fun learn(merchantKey: String, accountId: String) {
        val now = currentTimeMillis()
        database.merchantAccountRuleQueries.upsertMerchantAccountRule(
            merchant_key = merchantKey,
            account_id = accountId,
            merchant_key_ = merchantKey,
            account_id_ = accountId,
            first_seen_at = now,
            last_confirmed_at = now
        )
    }

    suspend fun getTopAccount(merchantKey: String): String? {
        return database.merchantAccountRuleQueries
            .selectTopAccountForMerchant(merchantKey)
            .executeAsOneOrNull()
            ?.account_id
    }
}
