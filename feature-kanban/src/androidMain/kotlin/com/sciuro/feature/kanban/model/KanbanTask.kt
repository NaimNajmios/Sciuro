package com.sciuro.feature.kanban.model

enum class TaskStatus {
    TODO, IN_PROGRESS, DONE, REJECTED
}

data class KanbanTask(
    val id: String,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val accountId: String? = null,
    val categoryId: String? = null,
    val direction: String? = null
)
