package com.sciuro.feature.kanban.viewmodel

import androidx.lifecycle.ViewModel
import com.sciuro.feature.kanban.model.KanbanTask
import com.sciuro.feature.kanban.model.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KanbanViewModel : ViewModel() {
    private val _tasks = MutableStateFlow<List<KanbanTask>>(emptyList())
    val tasks: StateFlow<List<KanbanTask>> = _tasks.asStateFlow()
    
    init {
        // Mock data for Phase C1 scaffolding
        _tasks.value = listOf(
            KanbanTask("1", "Review Luno Transaction", "Verify BTC purchase price", TaskStatus.TODO),
            KanbanTask("2", "Pay Rent", "Transfer RM1500 to landlord", TaskStatus.IN_PROGRESS),
            KanbanTask("3", "Reconcile Maybank", "Check statement for May", TaskStatus.DONE)
        )
    }
    
    fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) it.copy(status = newStatus) else it
        }
    }
}
