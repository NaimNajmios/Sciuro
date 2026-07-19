package com.sciuro.feature.kanban.di

import com.sciuro.feature.kanban.viewmodel.KanbanViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val kanbanModule = module {
    viewModel { KanbanViewModel(get()) }
}
