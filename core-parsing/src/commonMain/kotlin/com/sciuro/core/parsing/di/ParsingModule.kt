package com.sciuro.core.parsing.di

import com.sciuro.core.parsing.engine.DeterministicParser
import com.sciuro.core.parsing.engine.LlmFallbackParser
import com.sciuro.core.parsing.engine.SciuroParserPipeline
import com.sciuro.core.parsing.engine.SimulationEngine
import com.sciuro.core.parsing.engine.createHttpClient
import com.sciuro.core.parsing.rule.ParserRule
import com.sciuro.core.parsing.rule.bank.*
import com.sciuro.core.parsing.rule.ewallet.*
import com.sciuro.core.ledger.config.SettingsProvider
import org.koin.dsl.module

val parsingModule = module {
    single {
        createHttpClient()
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

