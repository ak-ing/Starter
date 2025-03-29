package com.aking.starter.utils

import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.view.WindowManager
import androidx.core.graphics.toPointF

/**
 * @author Ak
 * 2025/3/29  15:09
 */
/**
 * 获取屏幕宽高
 */
fun WindowManager.getScreenSize(): PointF = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    currentWindowMetrics.bounds.run { PointF(width().toFloat(), height().toFloat()) }
} else {
    Point().also { defaultDisplay.getSize(it) }.toPointF()
}