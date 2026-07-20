package com.najmi.sciuro.core.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemePreference {
    SYSTEM_DEFAULT, LIGHT, DARK
}

class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("sciuro_theme_prefs", Context.MODE_PRIVATE)
    
    private val _themePreference = MutableStateFlow(getSavedTheme())
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()
    
    private fun getSavedTheme(): ThemePreference {
        val name = prefs.getString("theme", ThemePreference.SYSTEM_DEFAULT.name)
        return try {
            ThemePreference.valueOf(name ?: ThemePreference.SYSTEM_DEFAULT.name)
        } catch (e: Exception) {
            ThemePreference.SYSTEM_DEFAULT
        }
    }
    
    fun setTheme(theme: ThemePreference) {
        prefs.edit().putString("theme", theme.name).apply()
        _themePreference.value = theme
    }
    
    companion object {
        @Volatile
        private var instance: ThemeManager? = null
        
        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
