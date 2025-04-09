package com.aking.starter.utils

import android.view.ViewConfiguration
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * @author Ak
 * 2025/4/9  16:46
 */
val LocalAndroidViewConfiguration = staticCompositionLocalOf<ViewConfiguration> {
    error("LocalAndroidViewConfiguration")
}