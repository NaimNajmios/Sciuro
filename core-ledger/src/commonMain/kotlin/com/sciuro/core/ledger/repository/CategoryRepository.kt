package com.sciuro.core.ledger.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.model.Category
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase
) : AuditableRepository(auditRepository) {

    suspend fun createCategory(category: Category): Category {
        return withAudit(
            entityType = EntityType.CATEGORY,
            entityId = category.id,
            action = AuditAction.CREATE,
            beforeState = null,
            afterState = category.toString(),
            source = AuditSource.USER_MANUAL
        ) {
            val now = currentTimeMillis()
            database.categoryQueries.insertCategory(
                id = category.id,
                name = category.name,
                type = category.type,
                icon = category.icon,
                color = category.color,
                created_at = now,
                updated_at = now
            )
            category
        }
    }

    fun observeCategoriesByType(type: String): Flow<List<Category>> {
        return database.categoryQueries.selectCategoriesByType(type).asFlow().mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    Category(
                        id = it.id,
                        name = it.name,
                        type = it.type,
                        icon = it.icon,
                        color = it.color
                    )
                }
            }
    }

    suspend fun seedCategories() {
        val existing = database.categoryQueries.selectAllCategories().executeAsList()
        if (existing.isEmpty()) {
            val defaults = listOf(
                Category("cat_exp_1", "Food & Beverage", "OUTFLOW"),
                Category("cat_exp_2", "Transportation", "OUTFLOW"),
                Category("cat_exp_3", "Bills", "OUTFLOW"),
                Category("cat_exp_4", "Shopping", "OUTFLOW"),
                Category("cat_exp_5", "Entertainment", "OUTFLOW"),
                Category("cat_exp_6", "Groceries", "OUTFLOW"),
                Category("cat_exp_7", "Health", "OUTFLOW"),
                Category("cat_exp_8", "Education", "OUTFLOW"),
                Category("cat_exp_9", "Others", "OUTFLOW"),
                Category("cat_inc_1", "Salary", "INFLOW"),
                Category("cat_inc_2", "Freelance", "INFLOW"),
                Category("cat_inc_3", "Gift", "INFLOW"),
                Category("cat_inc_4", "Investment Return", "INFLOW"),
                Category("cat_inc_5", "Refunds", "INFLOW"),
                Category("cat_inc_6", "Others", "INFLOW")
            )
            defaults.forEach { createCategory(it) }
        }
    }
}
