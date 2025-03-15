package com.aking.starter.screens.home

import com.aking.starter.R

/**
 * @author Ak
 * 2025/3/14  11:29
 */
data class Permission(
    val name: Int,
    val description: Int,
    val featuresInvolved: Int,
    val icon: Int,
    val isGranted: Boolean = false
)

/**
 * 悬浮窗权限
 */
val FloatingPermission = Permission(
    R.string.text_float_window,
    R.string.text_float_window_description,
    R.string.text_float_window_features_involved,
    R.drawable.ic_float_window
)