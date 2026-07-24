package com.sciuro.core.parsing.engine

import com.sciuro.core.ledger.config.LlmParsingConfig
import io.ktor.client.HttpClient

expect fun createHttpClient(config: LlmParsingConfig = LlmParsingConfig()): HttpClient
