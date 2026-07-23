package com.sciuro.core.classifier.rule

import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RuleLearner(
    private val database: SciuroDatabase,
    private val eventBus: DomainEventBus
) {
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.TransactionCategorized -> {
                        val merchant = event.merchant ?: return@collect
                        learnMerchantCategory(merchant, event.categoryId)
                    }
                    is DomainEvent.TransactionRecategorized -> {
                        val merchant = event.merchant ?: return@collect
                        learnMerchantCategory(merchant, event.newCategoryId)
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun learnMerchantCategory(merchant: String, categoryId: String) {
        val normalizedKey = merchant.lowercase().trim()
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
}
