package com.sciuro.core.ledger.config

interface LlmUsageStore {
    fun dailyLlmCallCount(): Int
    fun incrementDailyLlmCallCount(): Int
    fun dailyLlmCallLimit(): Int
    fun setDailyLlmCallLimit(limit: Int)
}
