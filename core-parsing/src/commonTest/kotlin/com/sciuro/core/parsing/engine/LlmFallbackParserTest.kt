package com.sciuro.core.parsing.engine

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ledger.config.LlmParsingConfig
import com.sciuro.core.parsing.model.TransactionDirection
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LlmFallbackParserTest {

    private val testJson = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun rawEvent() = RawEvent(
        id = "test-id", sourceType = SourceType.NOTIFICATION,
        sourcePackageOrAddress = "com.test.app", title = "Payment",
        text = "RM 100.00 paid to MERCHANT",
        timestamp = 1000L
    )

    private fun mockClient(responseJson: String, status: HttpStatusCode = HttpStatusCode.OK) =
        HttpClient(MockEngine) {
            install(ContentNegotiation) { json(testJson) }
            engine {
                addHandler { _ ->
                    respond(
                        content = responseJson,
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }

    @Test
    fun `returns null when apiKey is null`() = runBlocking {
        val parser = LlmFallbackParser(
            httpClient = mockClient("{}"),
            apiKeyProvider = { null }
        )
        assertNull(parser.parse(rawEvent()))
    }

    @Test
    fun `successfully parses valid JSON response`() = runBlocking {
        val parser = LlmFallbackParser(
            httpClient = mockClient("""{"choices":[{"message":{"role":"assistant","content":"{\"amount\":100.0,\"direction\":\"OUTFLOW\",\"merchant\":\"Test Merchant\"}"}}]}"""),
            apiKeyProvider = { "test-key" },
            config = LlmParsingConfig(endpointUrl = "http://localhost:9999/v1/chat/completions")
        )
        val result = parser.parse(rawEvent())
        assertNotNull(result)
        assertEquals(100.0, result.amount)
        assertEquals(TransactionDirection.OUTFLOW, result.direction)
        assertEquals("Test Merchant", result.merchant)
        assertEquals(0.0f, result.confidenceScore)
    }

    @Test
    fun `returns null on API error response`() = runBlocking {
        val parser = LlmFallbackParser(
            httpClient = mockClient("""{"error":{"message":"Rate limited"}}"""),
            apiKeyProvider = { "test-key" },
            config = LlmParsingConfig(endpointUrl = "http://localhost:9999/v1/chat/completions")
        )
        assertNull(parser.parse(rawEvent()))
    }

    @Test
    fun `returns null on malformed JSON`() = runBlocking {
        val parser = LlmFallbackParser(
            httpClient = mockClient("""{"choices":[{"message":{"role":"assistant","content":"not json"}}]}"""),
            apiKeyProvider = { "test-key" },
            config = LlmParsingConfig(endpointUrl = "http://localhost:9999/v1/chat/completions")
        )
        assertNull(parser.parse(rawEvent()))
    }

    @Test
    fun `returns null on empty choices`() = runBlocking {
        val parser = LlmFallbackParser(
            httpClient = mockClient("""{"choices":[]}"""),
            apiKeyProvider = { "test-key" },
            config = LlmParsingConfig(endpointUrl = "http://localhost:9999/v1/chat/completions")
        )
        assertNull(parser.parse(rawEvent()))
    }

    @Test
    fun `circuit breaker opens after consecutive failures`() = runBlocking {
        val parser = LlmFallbackParser(
            httpClient = mockClient("""{"error":{"message":"fail"}}"""),
            apiKeyProvider = { "test-key" },
            config = LlmParsingConfig(endpointUrl = "http://localhost:9999/v1/chat/completions", circuitBreakerThreshold = 3)
        )
        assertFalse(parser.isCircuitBroken())
        assertNull(parser.parse(rawEvent()))
        assertFalse(parser.isCircuitBroken())
        assertNull(parser.parse(rawEvent()))
        assertFalse(parser.isCircuitBroken())
        assertNull(parser.parse(rawEvent()))
        assertTrue(parser.isCircuitBroken())
    }

    @Test
    fun `circuit breaker returns null when open`() = runBlocking {
        val parser = LlmFallbackParser(
            httpClient = mockClient("""{"error":{"message":"fail"}}"""),
            apiKeyProvider = { "test-key" },
            config = LlmParsingConfig(endpointUrl = "http://localhost:9999/v1/chat/completions", circuitBreakerThreshold = 1)
        )
        parser.parse(rawEvent())
        assertTrue(parser.isCircuitBroken())
        assertNull(parser.parse(rawEvent()))
    }

    @Test
    fun `manual reset clears circuit breaker`() = runBlocking {
        val parser = LlmFallbackParser(
            httpClient = mockClient("""{"error":{"message":"fail"}}"""),
            apiKeyProvider = { "test-key" },
            config = LlmParsingConfig(endpointUrl = "http://localhost:9999/v1/chat/completions", circuitBreakerThreshold = 1)
        )
        parser.parse(rawEvent())
        assertTrue(parser.isCircuitBroken())
        parser.resetCircuitBreaker()
        assertFalse(parser.isCircuitBroken())
    }

    @Test
    fun `validated LLM draft gets elevated confidence when trust enabled`() = runBlocking {
        val parser = LlmFallbackParser(
            httpClient = mockClient("""{"choices":[{"message":{"role":"assistant","content":"{\"amount\":100.0,\"direction\":\"OUTFLOW\",\"merchant\":\"Test\"}"}}]}"""),
            apiKeyProvider = { "test-key" },
            config = LlmParsingConfig(endpointUrl = "http://localhost:9999/v1/chat/completions", trustValidatedLlm = true)
        )
        val result = parser.parse(rawEvent())
        assertNotNull(result)
        assertEquals(LlmFallbackParser.VALIDATED_CONFIDENCE, result.confidenceScore)
    }

    @Test
    fun `debug capture is populated on success`() = runBlocking {
        val parser = LlmFallbackParser(
            httpClient = mockClient("""{"choices":[{"message":{"role":"assistant","content":"{\"amount\":100.0,\"direction\":\"OUTFLOW\",\"merchant\":\"Test\"}"}}]}"""),
            apiKeyProvider = { "test-key" },
            config = LlmParsingConfig(endpointUrl = "http://localhost:9999/v1/chat/completions")
        )
        parser.parse(rawEvent())
        val capture = parser.lastDebugCapture
        assertNotNull(capture)
        assertEquals("llama-3.1-8b-instant", capture.modelUsed)
    }

    @Test
    fun `circuit breaker auto-resets after cooldown`() = runBlocking {
        val parser = LlmFallbackParser(
            httpClient = mockClient("""{"error":{"message":"fail"}}"""),
            apiKeyProvider = { "test-key" },
            config = LlmParsingConfig(endpointUrl = "http://localhost:9999/v1/chat/completions", circuitBreakerThreshold = 1, cooldownMs = 1)
        )
        parser.parse(rawEvent())
        assertTrue(parser.isCircuitBroken())
        delay(50)
        assertFalse(parser.isCircuitBroken())
    }
}
