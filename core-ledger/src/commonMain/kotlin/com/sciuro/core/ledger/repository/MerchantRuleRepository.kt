package com.sciuro.core.ledger.repository

import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.model.MerchantAccountRuleUiModel
import com.sciuro.core.ledger.model.MerchantRuleUiModel
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class MerchantRuleRepository(
    private val database: SciuroDatabase,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) {
    fun observeAllRules(): Flow<List<MerchantRuleUiModel>> {
        val rulesFlow = database.merchantCategoryRuleQueries
            .selectAllMerchantRules()
            .asFlow()
            .mapToList(Dispatchers.Default)

        val categoriesFlow = categoryRepository.observeCategories()

        return combine(rulesFlow, categoriesFlow) { rules, categories ->
            val catMap = categories.associateBy { it.id }
            rules.map { rule ->
                val category = catMap[rule.category_id]
                val key = rule.merchant_key
                MerchantRuleUiModel(
                    merchantKey = key,
                    displayName = MerchantRuleUiModel.displayNameFromKey(key),
                    categoryId = rule.category_id,
                    categoryName = category?.name ?: rule.category_id,
                    confirmationCount = rule.confirmation_count.toInt(),
                    firstSeenAt = rule.first_seen_at,
                    lastConfirmedAt = rule.last_confirmed_at,
                    isTrusted = rule.confirmation_count.toInt() >= MerchantRuleUiModel.TRUST_THRESHOLD
                )
            }.sortedBy { it.displayName }
        }
    }

    suspend fun deleteRule(merchantKey: String) {
        database.merchantCategoryRuleQueries.deleteMerchantRule(merchantKey)
    }

    suspend fun overrideRule(merchantKey: String, newCategoryId: String) {
        val now = currentTimeMillis()
        database.merchantCategoryRuleQueries.upsertMerchantRule(
            merchant_key = merchantKey,
            category_id = newCategoryId,
            confirmation_count = 1L,
            first_seen_at = now,
            last_confirmed_at = now
        )
    }

    fun observeAllAccountRules(): Flow<List<MerchantAccountRuleUiModel>> {
        val rulesFlow = database.merchantAccountRuleQueries
            .selectAllMerchantAccountRules()
            .asFlow()
            .mapToList(Dispatchers.Default)

        val accountsFlow = accountRepository.observeAccounts()

        return combine(rulesFlow, accountsFlow) { rules, accounts ->
            val accMap = accounts.associateBy { it.id }
            rules.map { rule ->
                val account = accMap[rule.account_id]
                MerchantAccountRuleUiModel(
                    merchantKey = rule.merchant_key,
                    displayName = MerchantAccountRuleUiModel.displayNameFromKey(rule.merchant_key),
                    accountId = rule.account_id,
                    accountName = account?.name ?: rule.account_id,
                    confirmationCount = rule.confirmation_count.toInt()
                )
            }.sortedBy { it.displayName }
        }
    }

    suspend fun deleteAccountRule(merchantKey: String, accountId: String) {
        database.merchantAccountRuleQueries.deleteMerchantAccountRule(merchantKey, accountId)
    }
}
