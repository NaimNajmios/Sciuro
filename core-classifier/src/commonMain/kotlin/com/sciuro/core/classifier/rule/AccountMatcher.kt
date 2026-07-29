package com.sciuro.core.classifier.rule

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.parsing.model.StructuredDraft

class AccountMatcher(
    private val database: SciuroDatabase
) {
    suspend fun match(rawEvent: RawEvent, draft: StructuredDraft): AccountMatch? {
        val candidates = mutableListOf<AccountMatch>()

        if (rawEvent.sourcePackageOrAddress.isNotBlank()) {
            database.accountQueries.selectAccountByPackage(rawEvent.sourcePackageOrAddress)
                .executeAsOneOrNull()?.let { acc ->
                    candidates.add(AccountMatch(acc.id, 100, "package"))
                }
        }

        draft.accountOrChannel?.let { channel ->
            val suffix = channel.takeLast(4).filter { it.isDigit() }
            if (suffix.isNotEmpty()) {
                database.accountQueries.selectAccountByNumberSuffix(suffix)
                    .executeAsOneOrNull()?.let { acc ->
                        candidates.add(AccountMatch(acc.id, 80, "suffix:$suffix"))
                    }
            }
        }

        draft.counterpartyAccountNumber?.let { counterparty ->
            val suffix = counterparty.takeLast(4).filter { it.isDigit() }
            if (suffix.isNotEmpty()) {
                database.accountQueries.selectAccountByNumberSuffix(suffix)
                    .executeAsOneOrNull()?.let { acc ->
                        candidates.add(AccountMatch(acc.id, 95, "counterparty_is_my_account"))
                    }
            }
        }

        val merchant = draft.merchant
        if (merchant != null) {
            val merchantKey = buildString { merchant.forEach { append(it.lowercaseChar()) } }.trim()
            val learnedRule = database.merchantAccountRuleQueries
                .selectTopAccountForMerchant(merchantKey)
                .executeAsOneOrNull()
            if (learnedRule != null) {
                candidates.add(AccountMatch(learnedRule.account_id, 60, "learned_merchant_account"))
            }

            if (candidates.none { it.reason == "learned_merchant_account" }) {
                val topMatch = database.accountQueries.selectMostCommonAccountForMerchant(merchant)
                    .executeAsOneOrNull()
                if (topMatch != null && topMatch.account_id != null) {
                    candidates.add(AccountMatch(topMatch.account_id, 40, "history"))
                }
            }
        }

        return candidates.maxByOrNull { it.score }
    }
}
