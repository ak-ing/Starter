package com.aking.starter.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.core.net.toUri

/**
 * 悬浮窗权限
 * @author Ak
 * 2025/3/14  10:17
 */
object FloatingPermissionHelper {

    /**
     * 检查当前应用是否具有悬浮窗权限
     *
     * @param content 上下文对象，用于访问系统服务进行权限检查。通常为Activity或Application上下文
     * @return Boolean 类型检查结果：true 表示已授予悬浮窗权限，false 表示未授予权限
     */
    fun checkFloatingPermission(content: Context): Boolean {
        val granted = Settings.canDrawOverlays(content)
        return granted
    }

    /**
     * 请求悬浮窗权限。
     *
     * 该函数用于启动系统设置页面，引导用户授予应用悬浮窗权限（即覆盖其他应用的权限）。
     *
     * @param context 上下文对象，用于启动活动和获取包名。
     *                通常为 Activity 或 Application 的上下文。
     */
    fun request(context: Context) {
        if (checkFloatingPermission(context)) return
        // 创建一个 Intent，用于跳转到悬浮窗权限管理页面。
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()
            )
        )
    }

    /**
     * 启动意图以请求悬浮窗权限
     *
     * 此函数负责检查应用是否已经拥有悬浮窗权限，如果没有，则通过指定的launcher启动一个意图
     * 该意图引导用户到系统设置页面，以手动授予悬浮窗权限
     *
     * @param launcher 用于启动活动的结果launcher，通过它来启动意图并获取结果
     * @param context 上下文对象，用于检查悬浮窗权限和获取应用包名
     */
    fun launch(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>, context: Context) {
        // 检查是否已经拥有悬浮窗权限，如果没有权限则执行以下代码块
        if (checkFloatingPermission(context)) return
        // 创建一个意图，指向系统设置的悬浮窗权限管理页面，并包含应用包名信息
        launcher.launch(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()
            )
        )
    }
}