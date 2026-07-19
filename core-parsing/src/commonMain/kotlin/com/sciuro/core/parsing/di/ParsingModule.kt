package com.sciuro.core.parsing.di

import com.sciuro.core.parsing.engine.DeterministicParser
import com.sciuro.core.parsing.engine.LlmFallbackParser
import com.sciuro.core.parsing.engine.SciuroParserPipeline
import com.sciuro.core.parsing.rule.bank.*
import com.sciuro.core.parsing.rule.ewallet.*
import com.sciuro.core.parsing.config.SettingsProvider
import io.ktor.client.HttpClient
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
        }
    }
    
    single {
        DeterministicParser(
            rules = listOf(
                CimbParserRule(),
                MaybankParserRule(),
                BsnParserRule(),
                TngParserRule(),
                GrabPayParserRule(),
                BoostParserRule(),
                ShopeePayParserRule()
            )
        )
    }
    
    single {
        LlmFallbackParser(
            httpClient = get(),
            apiKeyProvider = { 
                val settings = get<SettingsProvider>()
                if (settings.isLlmEnabled()) settings.getApiKey() else null 
            }
        )
    }
    
    single {
        SciuroParserPipeline(
            deterministicParser = get(),
            llmFallbackParser = get()
        )
    }
}
