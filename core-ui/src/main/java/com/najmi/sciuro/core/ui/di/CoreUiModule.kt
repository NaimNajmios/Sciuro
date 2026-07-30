package com.najmi.sciuro.core.ui.di

import com.najmi.sciuro.core.ui.theme.ThemeManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreUiModule = module {
    single { ThemeManager(androidContext()) }
}
