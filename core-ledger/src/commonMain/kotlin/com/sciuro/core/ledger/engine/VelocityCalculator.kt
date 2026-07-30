package com.sciuro.core.ledger.engine

import com.sciuro.core.ledger.db.Transaction_record
import com.sciuro.core.audit.util.currentTimeMillis

enum class VelocityTrend { UP, DOWN, STABLE, N_A }

data class SpendingVelocity(
    val dailyAverage: Double,
    val burnRatePercent: Float,
    val daysUntilDepleted: Long,
    val trend: VelocityTrend,
    val currentPeriodTotal: Double,
    val previousPeriodTotal: Double
)

class VelocityCalculator {
    fun calculate(
        accountBalance: Double,
        allTransactions: List<Transaction_record>
    ): SpendingVelocity {
        val now = currentTimeMillis()
        val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
        val thirtyDaysAgo = now - thirtyDaysMs
        val sixtyDaysAgo = now - 2 * thirtyDaysMs

        val currentPeriod = allTransactions.filter {
            it.direction == "OUTFLOW" && it.timestamp >= thirtyDaysAgo
        }
        val currentTotal = currentPeriod.sumOf { it.amount }
        val daysCurrent = maxOf(1, ((now - thirtyDaysAgo) / (24L * 60L * 60L * 1000L)))
        val dailyAverage = currentTotal / daysCurrent

        val previousPeriod = allTransactions.filter {
            it.direction == "OUTFLOW" && it.timestamp in sixtyDaysAgo..thirtyDaysAgo
        }
        val previousTotal = previousPeriod.sumOf { it.amount }

        val trend = when {
            currentPeriod.isEmpty() || previousPeriod.isEmpty() -> VelocityTrend.N_A
            currentTotal > previousTotal * 1.10 -> VelocityTrend.UP
            currentTotal < previousTotal * 0.90 -> VelocityTrend.DOWN
            else -> VelocityTrend.STABLE
        }

        val burnRate = if (accountBalance > 0) (dailyAverage / accountBalance).toFloat() else 1f
        val daysUntil = if (dailyAverage > 0) (accountBalance / dailyAverage).toLong() else Long.MAX_VALUE

        return SpendingVelocity(
            dailyAverage = dailyAverage,
            burnRatePercent = burnRate,
            daysUntilDepleted = daysUntil,
            trend = trend,
            currentPeriodTotal = currentTotal,
            previousPeriodTotal = previousTotal
        )
    }
}
