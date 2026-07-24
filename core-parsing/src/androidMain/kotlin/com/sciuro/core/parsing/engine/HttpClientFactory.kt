package com.sciuro.core.parsing.engine

import com.sciuro.core.ledger.config.LlmParsingConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createHttpClient(config: LlmParsingConfig): HttpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = config.requestTimeoutMs
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = config.requestTimeoutMs
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = config.maxRetries)
        exponentialDelay()
    }
}
