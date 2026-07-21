package com.sciuro.core.parsing.engine

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.config.LlmParsingConfig
import com.sciuro.core.parsing.llm.ChatMessage
import com.sciuro.core.parsing.llm.ChatRequest
import com.sciuro.core.parsing.llm.ChatResponse
import com.sciuro.core.parsing.llm.ResponseFormat
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.parsing.model.TransactionDirection
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

class LlmFallbackParser(
    private val httpClient: HttpClient,
    private val apiKeyProvider: () -> String?,
    private val config: LlmParsingConfig = LlmParsingConfig()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    private data class LlmResult(
        val amount: Double,
        val direction: TransactionDirection,
        val merchant: String? = null,
        val accountOrChannel: String? = null
    )

    private var consecutiveFailures = 0
    private var circuitBrokenUntil: Long = 0

    fun isCircuitBroken(): Boolean {
        if (System.currentTimeMillis() >= circuitBrokenUntil) {
            consecutiveFailures = 0
            return false
        }
        return true
    }

    fun resetCircuitBreaker() {
        consecutiveFailures = 0
        circuitBrokenUntil = 0
    }

    suspend fun parse(event: RawEvent): StructuredDraft? {
        if (isCircuitBroken()) {
            println("SCIURO_LLM: Circuit breaker open. Skipping LLM call.")
            return null
        }

        val apiKey = apiKeyProvider() ?: return null

        val prompt = """
            Extract the financial transaction details from the following notification text.
            You must accurately capture the entity on the other side of the transaction (the sender if money is received, or the receiver if money is spent).
            Because this system uses a single "merchant" text field to describe that entity, you must format the "merchant" field as a comprehensive label.
            
            Rules for the "merchant" field:
            1. Identify if the transaction is "Personal" (e.g., transferring money to a friend's bank account, DuitNow to an individual) or "Business" (e.g., retail store, restaurant, Shopee, corporate payroll).
            2. Identify the exact name of the sender (if INFLOW) or receiver (if OUTFLOW) from the text.
            3. Combine them into this format: "EntityName (Type)". Examples: "Ahmad Ali (Personal)", "Starbucks Coffee (Business)", "Salary Deposit (Business)".
            4. If the name is completely missing, default to "Unknown Entity (Personal/Business)".
            
            Respond strictly in valid JSON format matching this schema:
            {
                "amount": double, // The exact numerical amount
                "direction": "INFLOW" or "OUTFLOW", // INFLOW if money is received, OUTFLOW if money is sent/spent
                "merchant": "string", // The formatted label e.g. "John Doe (Personal)"
                "accountOrChannel": "string or null" // The account name/number or channel if visible
            }
            
            Title: ${event.title}
            Text: ${event.text}
        """.trimIndent()

        val request = ChatRequest(
            model = config.modelName,
            messages = listOf(
                ChatMessage(role = "system", content = "You are a specialized financial data extraction tool. You only output valid JSON."),
                ChatMessage(role = "user", content = prompt)
            ),
            response_format = ResponseFormat(type = "json_object"),
            temperature = config.temperature
        )

        return try {
            val response: ChatResponse = httpClient.post(config.endpointUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(request)
            }.body()

            consecutiveFailures = 0

            if (response.error != null) {
                println("SCIURO_LLM: API error: ${response.error.message}")
                onFailure("API error: ${response.error.message}")
                return null
            }

            val jsonString = response.choices?.firstOrNull()?.message?.content ?: run {
                onFailure("Empty response from LLM")
                return null
            }

            val result = json.decodeFromString<LlmResult>(jsonString)

            StructuredDraft(
                amount = result.amount,
                direction = result.direction,
                merchant = result.merchant,
                accountOrChannel = result.accountOrChannel ?: event.sourcePackageOrAddress,
                referenceId = null,
                timestamp = event.timestamp,
                confidenceScore = 0.0f
            )
        } catch (e: HttpRequestTimeoutException) {
            println("SCIURO_LLM: Request timed out")
            onFailure("Request timed out")
            null
        } catch (e: IOException) {
            println("SCIURO_LLM: Network error: ${e.message}")
            onFailure("Network error: ${e.message}")
            null
        } catch (e: kotlinx.serialization.SerializationException) {
            println("SCIURO_LLM: Malformed response: ${e.message}")
            onFailure("Malformed response: ${e.message}")
            null
        } catch (e: Exception) {
            println("SCIURO_LLM: Unexpected error: ${e.message}")
            e.printStackTrace()
            onFailure("Unexpected error: ${e.message}")
            null
        }
    }

    private fun onFailure(reason: String) {
        consecutiveFailures++
        if (consecutiveFailures >= config.circuitBreakerThreshold) {
            circuitBrokenUntil = System.currentTimeMillis() + config.cooldownMs
            println("SCIURO_LLM: Circuit breaker opened after $consecutiveFailures failures until $circuitBrokenUntil")
        }
    }
}
