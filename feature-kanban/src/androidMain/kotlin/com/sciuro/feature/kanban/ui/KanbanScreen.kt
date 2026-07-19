package com.sciuro.feature.kanban.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sciuro.feature.kanban.model.KanbanTask
import com.sciuro.feature.kanban.model.TaskStatus
import com.sciuro.feature.kanban.viewmodel.KanbanViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun KanbanScreen(viewModel: KanbanViewModel = koinViewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    
    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        KanbanColumn("To Do", tasks.filter { it.status == TaskStatus.TODO }, Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        KanbanColumn("In Progress", tasks.filter { it.status == TaskStatus.IN_PROGRESS }, Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        KanbanColumn("Done", tasks.filter { it.status == TaskStatus.DONE }, Modifier.weight(1f))
    }
}

@Composable
fun KanbanColumn(title: String, tasks: List<KanbanTask>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxHeight().background(Color.LightGray.copy(alpha = 0.2f)).padding(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks) { task ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
                        Text(text = task.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}
