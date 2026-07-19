package com.sciuro.feature.kanban.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.feature.kanban.model.KanbanTask
import com.sciuro.feature.kanban.model.TaskStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KanbanViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    // For D2 Integration: We map unreviewed transactions to Kanban Tasks in the "TODO" column.
    // Manually created tasks would be merged here if we had a core-task module.
    val tasks: StateFlow<List<KanbanTask>> = transactionRepository.observeUnreviewedTransactions()
        .map { unreviewedTxs ->
            unreviewedTxs.map { tx ->
                KanbanTask(
                    id = tx.id,
                    title = "Review Transaction: ${tx.merchant}",
                    description = "Amount: RM ${tx.amount} (${tx.direction})",
                    status = TaskStatus.TODO
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        // In a full implementation, moving to "DONE" would trigger transactionRepository.reviewTransaction()
        if (newStatus == TaskStatus.DONE) {
            viewModelScope.launch {
                // We assume the user has categorized it via a prompt, but for now we just mark reviewed without a new category
                transactionRepository.reviewTransaction(taskId, null)
            }
        }
    }
}
