package com.sciuro.core.parsing.engine

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ledger.config.LlmParsingConfig
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.parsing.model.TransactionDirection
import com.sciuro.core.parsing.rule.ParserRule
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SciuroParserPipelineTest {

    private val packageName = "com.test.app"
    private val testJson = Json { ignoreUnknownKeys = true; isLenient = true }

    private val confidentDraft = StructuredDraft(
        amount = 100.0, direction = TransactionDirection.OUTFLOW,
        merchant = "Test Merchant", accountOrChannel = "Test Bank",
        referenceId = null, timestamp = 1000L, confidenceScore = 1.0f
    )

    private val lowConfidenceDraft = StructuredDraft(
        amount = 50.0, direction = TransactionDirection.INFLOW,
        merchant = null, accountOrChannel = "Test Bank",
        referenceId = null, timestamp = 1000L, confidenceScore = 0.5f
    )

    private fun rawEvent() = RawEvent(
        id = "test-id", sourceType = SourceType.NOTIFICATION,
        sourcePackageOrAddress = packageName, title = "Test",
        text = "Test body", timestamp = 1000L
    )

    private class TestRule(
        private val pkg: String, private val draft: StructuredDraft?
    ) : ParserRule {
        override fun matches(event: RawEvent): Boolean = event.sourcePackageOrAddress == pkg
        override fun extract(event: RawEvent): StructuredDraft? = draft
    }

    private fun noKeyLlmParser() = LlmFallbackParser(
        httpClient = HttpClient(MockEngine { respond("") }) {
            install(ContentNegotiation) { json(testJson) }
        },
        apiKeyProvider = { null }
    )

    private fun successLlmParser() = LlmFallbackParser(
        httpClient = HttpClient(MockEngine {
            respond(
                content = """{"choices":[{"message":{"role":"assistant","content":"{\"amount\":200.0,\"direction\":\"OUTFLOW\",\"merchant\":\"LLM Merchant\"}"}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json(testJson) }
        },
        apiKeyProvider = { "test-key" },
        config = LlmParsingConfig(endpointUrl = "http://localhost:9999/v1/chat/completions")
    )

    private fun errorLlmParser() = LlmFallbackParser(
        httpClient = HttpClient(MockEngine {
            respond(
                content = """{"error":{"message":"Service unavailable"}}""",
                status = HttpStatusCode.ServiceUnavailable,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json(testJson) }
        },
        apiKeyProvider = { "test-key" },
        config = LlmParsingConfig(endpointUrl = "http://localhost:9999/v1/chat/completions")
    )

    @Test
    fun `confident deterministic result`() = runBlocking {
        val parser = DeterministicParser(listOf(TestRule(packageName, confidentDraft)))
        val pipeline = SciuroParserPipeline(parser, noKeyLlmParser())
        val result = pipeline.process(rawEvent())
        assertNotNull(result)
        assertEquals(100.0, result.amount)
        assertEquals(TransactionDirection.OUTFLOW, result.direction)
    }

    @Test
    fun `deterministic null — LLM succeeds`() = runBlocking {
        val parser = DeterministicParser(listOf(TestRule(packageName, null)))
        val pipeline = SciuroParserPipeline(parser, successLlmParser())
        val result = pipeline.process(rawEvent())
        assertNotNull(result)
        assertEquals(200.0, result.amount)
    }

    @Test
    fun `low confidence deterministic — LLM succeeds and wins`() = runBlocking {
        val parser = DeterministicParser(listOf(TestRule(packageName, lowConfidenceDraft)))
        val pipeline = SciuroParserPipeline(parser, successLlmParser())
        val result = pipeline.process(rawEvent())
        assertNotNull(result)
        assertEquals(200.0, result.amount)
    }

    @Test
    fun `LLM returns null — falls back to deterministic`() = runBlocking {
        val parser = DeterministicParser(listOf(TestRule(packageName, lowConfidenceDraft)))
        val pipeline = SciuroParserPipeline(parser, errorLlmParser())
        val result = pipeline.process(rawEvent())
        assertNotNull(result)
        assertEquals(50.0, result.amount)
    }

    @Test
    fun `no API key — returns deterministic even if low confidence`() = runBlocking {
        val parser = DeterministicParser(listOf(TestRule(packageName, lowConfidenceDraft)))
        val pipeline = SciuroParserPipeline(parser, noKeyLlmParser())
        val result = pipeline.process(rawEvent())
        assertNotNull(result)
        assertEquals(TransactionDirection.INFLOW, result.direction)
    }

    @Test
    fun `both deterministic and LLM return null`() = runBlocking {
        val parser = DeterministicParser(listOf(TestRule(packageName, null)))
        val pipeline = SciuroParserPipeline(parser, errorLlmParser())
        assertNull(pipeline.process(rawEvent()))
    }
}
