package com.sciuro.feature.kanban.model

import com.sciuro.core.obligations.model.Obligation

enum class BillStatus { UPCOMING, DUE_SOON, OVERDUE, SETTLED }

data class BillTask(
    val id: String,
    val name: String,
    val amount: Double,
    val status: BillStatus,
    val dueDate: Long,
    val categoryId: String?,
    val accountId: String?,
    val obligation: Obligation
) {
    companion object {
        fun fromObligation(obligation: Obligation, now: Long): BillTask {
            val daysUntilDue = (obligation.nextDueDate - now) / (24L * 60L * 60L * 1000L)
            
            val lastPaidDate = obligation.lastPaidDate
            val isSettled = if (lastPaidDate != null) {
                val cycleDays = when (obligation.frequency.name) {
                    "WEEKLY" -> 7L
                    "YEARLY" -> 365L
                    else -> 30L
                }
                val daysSinceLastPaid = (now - lastPaidDate) / (24L * 60L * 60L * 1000L)
                daysSinceLastPaid < cycleDays
            } else false

            val status = when {
                daysUntilDue < 0 -> BillStatus.OVERDUE
                isSettled -> BillStatus.SETTLED
                daysUntilDue <= 3 -> BillStatus.DUE_SOON
                else -> BillStatus.UPCOMING
            }
            return BillTask(
                id = obligation.id,
                name = obligation.name,
                amount = obligation.amount,
                status = status,
                dueDate = obligation.nextDueDate,
                categoryId = obligation.categoryId,
                accountId = obligation.accountId,
                obligation = obligation
            )
        }
    }
}
