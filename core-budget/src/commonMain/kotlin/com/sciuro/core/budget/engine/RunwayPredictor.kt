package com.sciuro.core.budget.engine

import com.sciuro.core.audit.util.currentTimeMillis

data class RunwayPrediction(
    val daysUntilNegative: Int,
    val projectedBalance: List<Double>,
    val confidence: Double,
    val dailyAverageSpend: Double,
    val isCritical: Boolean
)

class RunwayPredictor {

    fun predict(
        accountBalance: Double,
        recentOutflows: List<Double>,
        upcomingObligationAmounts: List<Double>,
        upstreamIncome: Double? = null
    ): RunwayPrediction? {
        if (recentOutflows.isEmpty()) return null

        val dailyAverage = recentOutflows.sum() / maxOf(1, recentOutflows.size)

        val projected = mutableListOf(accountBalance)
        var balance = accountBalance
        var daysUntil = 0

        for (day in 1..30) {
            balance -= dailyAverage
            val obligationsThisDay = if (day - 1 < upcomingObligationAmounts.size) upcomingObligationAmounts[day - 1] else 0.0
            balance -= obligationsThisDay

            if (upstreamIncome != null && day == 1) {
                balance += upstreamIncome
            }

            projected.add(balance)
            if (daysUntil == 0 && balance < 0) {
                daysUntil = day
            }
        }

        val variance = if (recentOutflows.size >= 2) {
            val mean = recentOutflows.average()
            val squaredDiffs = recentOutflows.map { (it - mean) * (it - mean) }
            val stdDev = kotlin.math.sqrt(squaredDiffs.average())
            stdDev / maxOf(1.0, mean)
        } else 1.0

        val confidence = maxOf(0.0, minOf(1.0, 1.0 - variance))

        return RunwayPrediction(
            daysUntilNegative = if (daysUntil == 0) 30 else daysUntil,
            projectedBalance = projected,
            confidence = confidence,
            dailyAverageSpend = dailyAverage,
            isCritical = daysUntil in 1..7
        )
    }
}
