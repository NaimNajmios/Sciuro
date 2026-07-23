package com.sciuro.core.ledger.config

data class LlmParsingConfig(
    val modelName: String = "llama-3.1-8b-instant",
    val temperature: Double = 0.0,
    val endpointUrl: String = "https://api.groq.com/openai/v1/chat/completions",
    val requestTimeoutMs: Long = 30_000,
    val maxRetries: Int = 3,
    val circuitBreakerThreshold: Int = 5,
    val cooldownMs: Long = 300_000
)
