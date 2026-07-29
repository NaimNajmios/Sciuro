package com.sciuro.core.classifier.rule

import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.repository.MerchantAccountRuleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RuleLearner(
    private val database: SciuroDatabase,
    private val eventBus: DomainEventBus,
    private val merchantAccountRuleRepository: MerchantAccountRuleRepository
) {
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.TransactionCategorized -> {
                        val merchant = event.merchant
                        if (merchant == null) return@collect
                        learnMerchantCategory(merchant, event.categoryId)
                    }
                    is DomainEvent.TransactionRecategorized -> {
                        val merchant = event.merchant ?: return@collect
                        learnMerchantCategory(merchant, event.newCategoryId)
                        val accountId = event.accountId
                        if (accountId != null) {
                            learnMerchantAccount(merchant, accountId)
                        }
                    }
                    is DomainEvent.TransactionModified -> {
                        val tx = database.transactionRecordQueries.selectTransactionById(event.transactionId)
                            .executeAsOneOrNull()
                        val txMerchant = tx?.merchant
                        val txAccountId = tx?.account_id
                        if (txMerchant != null && txAccountId != null) {
                            learnMerchantAccount(txMerchant, txAccountId)
                            val catId = tx.category_id ?: return@collect
                            learnMerchantCategory(txMerchant, catId)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun learnMerchantCategory(merchant: String, categoryId: String) {
        val normalizedKey = buildString { merchant.forEach { append(it.lowercaseChar()) } }.trim()
        val existing = database.merchantCategoryRuleQueries
            .selectMerchantRuleByKey(normalizedKey)
            .executeAsOneOrNull()

        val now = currentTimeMillis()
        val newCount = (existing?.confirmation_count ?: 0) + 1
        val firstSeen = existing?.first_seen_at ?: now

        database.merchantCategoryRuleQueries.upsertMerchantRule(
            merchant_key = normalizedKey,
            category_id = categoryId,
            confirmation_count = newCount,
            first_seen_at = firstSeen,
            last_confirmed_at = now
        )

        eventBus.publish(
            DomainEvent.MerchantRuleLearned(
                merchant = normalizedKey,
                categoryId = categoryId
            )
        )
    }

    private suspend fun learnMerchantAccount(merchant: String, accountId: String) {
        val normalizedKey = buildString { merchant.forEach { append(it.lowercaseChar()) } }.trim()
        merchantAccountRuleRepository.learn(normalizedKey, accountId)

        eventBus.publish(
            DomainEvent.MerchantAccountRuleLearned(
                merchant = normalizedKey,
                accountId = accountId
            )
        )
    }
}
