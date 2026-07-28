package com.najmi.sciuro.core.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

enum class ThemePreference {
    SYSTEM_DEFAULT, LIGHT, DARK
}

class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("sciuro_theme_prefs", Context.MODE_PRIVATE)
    
    private val _themePreference = MutableStateFlow(getSavedTheme())
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()
    
    private val _palettePreference = MutableStateFlow(getSavedPalette())
    val palettePreference: StateFlow<PalettePreference> = _palettePreference.asStateFlow()
    
    private fun getSavedTheme(): ThemePreference {
        val name = prefs.getString("theme", ThemePreference.SYSTEM_DEFAULT.name)
        return try {
            ThemePreference.valueOf(name ?: ThemePreference.SYSTEM_DEFAULT.name)
        } catch (e: Exception) {
            ThemePreference.SYSTEM_DEFAULT
        }
    }
    
    private fun getSavedPalette(): PalettePreference {
        val name = prefs.getString("palette", PalettePreference.MONOCHROME.name)
        return try {
            PalettePreference.valueOf(name ?: PalettePreference.MONOCHROME.name)
        } catch (e: Exception) {
            PalettePreference.MONOCHROME
        }
    }
    
    fun setTheme(theme: ThemePreference) {
        prefs.edit().putString("theme", theme.name).apply()
        _themePreference.value = theme
    }
    
    fun setPalette(palette: PalettePreference) {
        prefs.edit().putString("palette", palette.name).apply()
        _palettePreference.value = palette
    }

    fun isDarkModeScheduleEnabled(): Boolean = prefs.getBoolean("dark_mode_schedule_enabled", false)

    fun setDarkModeScheduleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode_schedule_enabled", enabled).apply()
    }

    fun getDarkModeScheduleStart(): LocalTime {
        val hour = prefs.getInt("dark_mode_start_hour", 20)
        val minute = prefs.getInt("dark_mode_start_minute", 0)
        return LocalTime.of(hour, minute)
    }

    fun setDarkModeScheduleStart(time: LocalTime) {
        prefs.edit()
            .putInt("dark_mode_start_hour", time.hour)
            .putInt("dark_mode_start_minute", time.minute)
            .apply()
    }

    fun getDarkModeScheduleEnd(): LocalTime {
        val hour = prefs.getInt("dark_mode_end_hour", 7)
        val minute = prefs.getInt("dark_mode_end_minute", 0)
        return LocalTime.of(hour, minute)
    }

    fun setDarkModeScheduleEnd(time: LocalTime) {
        prefs.edit()
            .putInt("dark_mode_end_hour", time.hour)
            .putInt("dark_mode_end_minute", time.minute)
            .apply()
    }

    fun getEffectiveTheme(): ThemePreference {
        if (!isDarkModeScheduleEnabled()) return getSavedTheme()

        val now = LocalTime.now()
        val start = getDarkModeScheduleStart()
        val end = getDarkModeScheduleEnd()

        val inDarkWindow = if (start.isBefore(end)) {
            now in start..end
        } else {
            now >= start || now <= end
        }

        return if (inDarkWindow) ThemePreference.DARK else ThemePreference.LIGHT
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
