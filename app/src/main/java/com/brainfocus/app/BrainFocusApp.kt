package com.brainfocus.app

import android.app.Application
import com.brainfocus.app.ui.theme.ThemeManager

class BrainFocusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.init(this)
    }
}
