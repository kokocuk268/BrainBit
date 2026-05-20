package com.brainfocus.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val MODE_DARK = 1
    private const val MODE_LIGHT = 2

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun applySavedTheme(context: Context) {
        init(context)
        val mode = prefs.getInt(KEY_THEME_MODE, MODE_DARK)
        AppCompatDelegate.setDefaultNightMode(
            if (mode == MODE_LIGHT) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }

    fun isDarkTheme(context: Context): Boolean {
        init(context)
        val mode = prefs.getInt(KEY_THEME_MODE, MODE_DARK)
        return mode == MODE_DARK
    }

    fun toggleTheme(context: Context) {
        init(context)
        val currentMode = prefs.getInt(KEY_THEME_MODE, MODE_DARK)
        val newMode = if (currentMode == MODE_DARK) MODE_LIGHT else MODE_DARK
        prefs.edit().putInt(KEY_THEME_MODE, newMode).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (newMode == MODE_LIGHT) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }

    fun toggleThemeSilent(context: Context) {
        init(context)
        val currentMode = prefs.getInt(KEY_THEME_MODE, MODE_DARK)
        val newMode = if (currentMode == MODE_DARK) MODE_LIGHT else MODE_DARK
        prefs.edit().putInt(KEY_THEME_MODE, newMode).apply()
    }
}
