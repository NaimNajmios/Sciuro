package com.sciuro.core.parsing.engine

import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ledger.config.LlmParsingConfig
import com.sciuro.core.parsing.llm.ChatMessage
import com.sciuro.core.parsing.llm.ChatRequest
import com.sciuro.core.parsing.llm.ChatResponse
import com.sciuro.core.parsing.llm.ResponseFormat
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.parsing.model.TransactionDirection
import com.sciuro.core.parsing.util.extractAmount
import com.sciuro.core.parsing.util.PiiScrubber
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlin.math.abs

class LlmFallbackParser(
    private val httpClient: HttpClient,
    private val apiKeyProvider: () -> String?,
    private val config: LlmParsingConfig = LlmParsingConfig(),
    private val tracer: PipelineTracer? = null
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    private data class LlmResult(
        @SerialName("_reasoning") val reasoning: String? = null,
        @SerialName("is_transaction") val isTransaction: Boolean = true,
        val amount: Double? = null,
        val direction: TransactionDirection? = null,
        val merchant: String? = null,
        val accountOrChannel: String? = null
    )

    data class LlmFallbackDebugCapture(
        val prompt: String,
        val rawResponse: String?,
        val modelUsed: String,
        val latencyMs: Long
    )

    var lastDebugCapture: LlmFallbackDebugCapture? = null
        private set

    private var consecutiveFailures = 0
    private var circuitBrokenUntil: Long = 0

    private data class CacheEntry(val draft: StructuredDraft, val timestamp: Long)
    private val cache = mutableMapOf<String, CacheEntry>()

    companion object {
        private const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L
        const val VALIDATED_CONFIDENCE = 0.75f
    }

    fun isCircuitBroken(): Boolean {
        if (circuitBrokenUntil == 0L) return false
        if (System.currentTimeMillis() >= circuitBrokenUntil) {
            consecutiveFailures = 0
            circuitBrokenUntil = 0
            return false
        }
        return true
    }

    fun resetCircuitBreaker() {
        consecutiveFailures = 0
        circuitBrokenUntil = 0
    }

    suspend fun parse(event: RawEvent): StructuredDraft? {
        val cacheKey = "${event.title}|${event.text}"
        cache[cacheKey]?.let { entry ->
            val age = System.currentTimeMillis() - entry.timestamp
            if (age <= CACHE_TTL_MS) {
                tracer?.trace(event.id, null, TraceStage.PARSE_LLM, TraceOutcome.SUCCESS,
                    durationMs = 0, confidence = entry.draft.confidenceScore,
                    detail = mapOf("verdict" to "cache_hit", "cache_age_ms" to "$age"))
                return entry.draft
            }
            cache.remove(cacheKey)
        }

        if (isCircuitBroken()) {
            traceParse(event, "circuit_breaker_open", 0, mapOf("breaker_open" to "true"))
            return null
        }

        val apiKey = apiKeyProvider() ?: run {
            traceParse(event, "llm_disabled_or_no_key", 0)
            return null
        }

        val prompt = """
            Extract the financial transaction details from the following notification text.
            
            Your goals:
            1. Determine if this is a financial transaction. If it is not, return {"is_transaction": false}.
            2. Accurately capture the entity on the other side of the transaction (the sender if money is received, or the receiver if money is spent).
            3. Format the "merchant" field as a comprehensive label: "EntityName (Type)" where Type is "Personal" or "Business". If the name is completely missing, default to "Unknown Entity (Unknown)".
            
            Respond strictly in valid JSON format matching this schema:
            {
                "_reasoning": "Briefly explain who is sending and receiving, and why it is INFLOW or OUTFLOW. Omit if not a transaction.",
                "is_transaction": boolean, // true if it's a transaction, false otherwise
                "amount": double, // The exact numerical amount (only if is_transaction=true)
                "direction": "INFLOW" or "OUTFLOW", // INFLOW if money received, OUTFLOW if money spent (only if is_transaction=true)
                "merchant": "string", // The formatted label e.g. "John Doe (Personal)" (only if is_transaction=true)
                "accountOrChannel": "string or null" // The account name/number or channel if visible, else null (only if is_transaction=true)
            }
            
            --- FEW-SHOT EXAMPLES ---
            
            Example 1:
            Title: "Payment Successful"
            Text: "You have paid RM 15.50 to Starbucks Coffee via DuitNow."
            Output:
            {
                "_reasoning": "The user paid money to Starbucks, so it is an OUTFLOW. Starbucks is a Business.",
                "is_transaction": true,
                "amount": 15.50,
                "direction": "OUTFLOW",
                "merchant": "Starbucks Coffee (Business)",
                "accountOrChannel": "DuitNow"
            }
            
            Example 2:
            Title: "Transfer Received"
            Text: "RM 50.00 transferred from Ahmad Ali."
            Output:
            {
                "_reasoning": "The user received money from Ahmad Ali, so it is an INFLOW. Ahmad Ali is an individual, so it is Personal.",
                "is_transaction": true,
                "amount": 50.00,
                "direction": "INFLOW",
                "merchant": "Ahmad Ali (Personal)",
                "accountOrChannel": null
            }
            
            Example 3:
            Title: "Promo!"
            Text: "Get 50% off your next Grab ride."
            Output:
            {
                "_reasoning": "This is a promotional message, not a record of a financial transaction.",
                "is_transaction": false
            }
            
            --- TARGET INVOCATION ---
            Title: ${event.title}
            Text: ${event.text}
        """.trimIndent()

        val request = ChatRequest(
            model = config.modelName,
            messages = listOf(
                ChatMessage(role = "system", content = "You are a specialized financial data extraction tool. You only output valid JSON. Do not include markdown formatting like ```json."),
                ChatMessage(role = "user", content = prompt)
            ),
            response_format = ResponseFormat(type = "json_object"),
            temperature = config.temperature
        )

        val parseStartTime = System.currentTimeMillis()

        return try {
            val response: ChatResponse = httpClient.post(config.endpointUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(request)
            }.body()

            val elapsed = System.currentTimeMillis() - parseStartTime

            if (response.error != null) {
                val errMsg = response.error.message
                lastDebugCapture = LlmFallbackDebugCapture(prompt, null, config.modelName, elapsed)
                traceParse(event, "api_error", elapsed, mapOf("error" to errMsg))
                onFailure()
                return null
            }

            val jsonString = response.choices?.firstOrNull()?.message?.content ?: run {
                lastDebugCapture = LlmFallbackDebugCapture(prompt, null, config.modelName, elapsed)
                traceParse(event, "empty_response", elapsed)
                onFailure()
                return null
            }

            val result = json.decodeFromString<LlmResult>(jsonString)

            consecutiveFailures = 0

            lastDebugCapture = LlmFallbackDebugCapture(prompt, jsonString, config.modelName, elapsed)

            if (!result.isTransaction || result.amount == null || result.direction == null) {
                traceParse(event, "not_a_transaction", elapsed, mapOf("model" to config.modelName, "reasoning" to result.reasoning))
                return null
            }

            val validationPassed = validateAmount(result.amount, event.text, event.title) &&
                !result.merchant.isNullOrBlank()

            val confidence = if (validationPassed && config.trustValidatedLlm) VALIDATED_CONFIDENCE else 0.0f

            val verdict = when {
                validationPassed && config.trustValidatedLlm -> "validated_confident"
                validationPassed -> "validated_untrusted"
                else -> "validation_failed"
            }

            traceParse(event, verdict, elapsed,
                mapOf("model" to config.modelName, "merchant" to result.merchant,
                    "direction" to result.direction.name, "validated" to "$validationPassed", "reasoning" to result.reasoning))

            val draft = StructuredDraft(
                amount = result.amount,
                direction = result.direction,
                merchant = result.merchant,
                accountOrChannel = result.accountOrChannel ?: event.sourcePackageOrAddress,
                referenceId = null,
                timestamp = event.timestamp,
                confidenceScore = confidence
            )

            cache[cacheKey] = CacheEntry(draft, System.currentTimeMillis())

            return draft
        } catch (e: HttpRequestTimeoutException) {
            val elapsed = System.currentTimeMillis() - parseStartTime
            lastDebugCapture = LlmFallbackDebugCapture(prompt, null, config.modelName, elapsed)
            traceParse(event, "timeout", elapsed, mapOf("error" to "Request timed out"))
            onFailure()
            null
        } catch (e: IOException) {
            val elapsed = System.currentTimeMillis() - parseStartTime
            lastDebugCapture = LlmFallbackDebugCapture(prompt, null, config.modelName, elapsed)
            traceParse(event, "network_error", elapsed, mapOf("error" to (e.message ?: "")))
            onFailure()
            null
        } catch (e: kotlinx.serialization.SerializationException) {
            val elapsed = System.currentTimeMillis() - parseStartTime
            lastDebugCapture = LlmFallbackDebugCapture(prompt, null, config.modelName, elapsed)
            traceParse(event, "malformed_response", elapsed, mapOf("error" to (e.message ?: "")))
            onFailure()
            null
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - parseStartTime
            lastDebugCapture = LlmFallbackDebugCapture(prompt, null, config.modelName, elapsed)
            traceParse(event, "unexpected_error", elapsed, mapOf("error" to (e.message ?: "")))
            onFailure()
            null
        }
    }

    private fun validateAmount(llmAmount: Double, rawText: String, rawTitle: String): Boolean {
        val visibleAmount = extractAmount(rawText) ?: extractAmount(rawTitle) ?: return true
        val tolerance = maxOf(1.0, visibleAmount * 0.1)
        return abs(llmAmount - visibleAmount) <= tolerance
    }

    private fun onFailure() {
        consecutiveFailures++
        if (consecutiveFailures >= config.circuitBreakerThreshold) {
            circuitBrokenUntil = System.currentTimeMillis() + config.cooldownMs
        }
    }

    private suspend fun traceParse(
        event: RawEvent,
        verdict: String,
        latencyMs: Long,
        extra: Map<String, String?> = emptyMap()
    ) {
        tracer?.trace(
            rawEventId = event.id,
            transactionId = null,
            stage = TraceStage.PARSE_LLM,
            outcome = if (verdict == "success" || verdict == "validated_confident" || verdict == "validated_untrusted")
                TraceOutcome.SUCCESS else TraceOutcome.FAILURE,
            durationMs = latencyMs,
            detail = extra + mapOf(
                "verdict" to verdict,
                "model" to (extra["model"] ?: ""),
                "consecutive_failures" to "$consecutiveFailures",
                "breaker_open" to "${isCircuitBroken()}"
            )
        )
    }
}
