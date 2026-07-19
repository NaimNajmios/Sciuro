package com.sciuro.core.parsing.engine

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.llm.ChatMessage
import com.sciuro.core.parsing.llm.ChatRequest
import com.sciuro.core.parsing.llm.ChatResponse
import com.sciuro.core.parsing.llm.ResponseFormat
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.parsing.model.TransactionDirection
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

class LlmFallbackParser(
    private val httpClient: HttpClient,
    private val apiKeyProvider: () -> String?
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    private data class LlmResult(
        val amount: Double,
        val direction: TransactionDirection,
        val merchant: String? = null,
        val accountOrChannel: String? = null
    )

    suspend fun parse(event: RawEvent): StructuredDraft? {
        val apiKey = apiKeyProvider() ?: return null // Opt-in: silently skip if no key

        val prompt = """
            Extract the financial transaction details from the following notification text.
            Respond strictly in valid JSON format matching this schema:
            {
                "amount": double,
                "direction": "INFLOW" or "OUTFLOW",
                "merchant": "string or null",
                "accountOrChannel": "string or null"
            }
            
            Title: ${event.title}
            Text: ${event.text}
        """.trimIndent()

        val request = ChatRequest(
            model = "llama3-8b-8192", // Groq Llama 3 8B
            messages = listOf(
                ChatMessage(role = "system", content = "You are a specialized financial data extraction tool. You only output valid JSON."),
                ChatMessage(role = "user", content = prompt)
            ),
            response_format = ResponseFormat(type = "json_object"),
            temperature = 0.0
        )

        return try {
            val response: ChatResponse = httpClient.post("https://api.groq.com/openai/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(request)
            }.body()
            
            val jsonString = response.choices.firstOrNull()?.message?.content ?: return null
            
            val result = json.decodeFromString<LlmResult>(jsonString)
            
            StructuredDraft(
                amount = result.amount,
                direction = result.direction,
                merchant = result.merchant,
                accountOrChannel = result.accountOrChannel ?: event.sourcePackageOrAddress,
                referenceId = null,
                timestamp = event.timestamp,
                isConfident = false // LLM outputs always go to manual review for safety
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
