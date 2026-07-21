package com.sciuro.core.parsing.di

import com.sciuro.core.parsing.engine.DeterministicParser
import com.sciuro.core.parsing.engine.LlmFallbackParser
import com.sciuro.core.parsing.engine.SciuroParserPipeline
import com.sciuro.core.parsing.engine.SimulationEngine
import com.sciuro.core.parsing.rule.ParserRule
import com.sciuro.core.parsing.rule.bank.*
import com.sciuro.core.parsing.rule.ewallet.*
import com.sciuro.core.parsing.config.SettingsProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val parsingModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
            }
        }
    }

    single {
        listOf<ParserRule>(
            CimbParserRule(),
            MaybankParserRule(),
            BsnParserRule(),
            TngParserRule(),
            GrabPayParserRule(),
            BoostParserRule(),
            ShopeePayParserRule()
        )
    }
    
    single {
        DeterministicParser(rules = get())
    }
    
    single {
        SimulationEngine(rules = get(), llmFallbackParser = get())
    }
    
    single {
        val settings = get<SettingsProvider>()
        LlmFallbackParser(
            httpClient = get(),
            apiKeyProvider = {
                if (settings.isLlmEnabled()) settings.getApiKey() else null
            },
            config = settings.getLlmConfig()
        )
    }
    
    single {
        SciuroParserPipeline(
            deterministicParser = get(),
            llmFallbackParser = get()
        )
    }
}
