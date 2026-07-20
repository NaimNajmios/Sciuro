package com.sciuro.core.parsing.llm

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val response_format: ResponseFormat? = null,
    val temperature: Double = 0.0
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String
)

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice>? = null,
    val error: ChatError? = null
)

@Serializable
data class ChatError(
    val message: String? = null
)

@Serializable
data class ChatChoice(
    val message: ChatMessage
)
