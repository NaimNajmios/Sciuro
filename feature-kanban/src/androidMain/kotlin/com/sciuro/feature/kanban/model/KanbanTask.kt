package com.sciuro.feature.kanban.model

enum class TaskStatus {
    TODO, IN_PROGRESS, DONE
}

data class KanbanTask(
    val id: String,
    val title: String,
    val description: String,
    val status: TaskStatus
)
