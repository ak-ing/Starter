package com.aking.starter.ui.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

/**
 * @author Ak
 * 2025/3/20  17:01
 */
abstract class BaseFloatingComposeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val windowManager by lazy { context.getSystemService(WindowManager::class.java) }

    override fun onFinishInflate() {
        super.onFinishInflate()
        addView(ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.Default)
            setContent { Content() }
        })
    }

    @Composable
    protected abstract fun Content()

    /**
     * 悬浮窗参数
     */
    private val windowParams = WindowManager.LayoutParams().apply {
        // 设置悬浮窗类型
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        // 设置悬浮窗位置
        gravity = Gravity.START or Gravity.TOP
        // 设置悬浮窗宽高
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        //避免获取焦点（如果悬浮窗获取到焦点，那么悬浮窗以外的地方就不可操控了，造成假死机现象）
        flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    }

    /**
     * 添加到WindowManager
     */
    fun addToWindowManager() {
        if (isAttachedToWindow) return
        windowManager.addView(this, windowParams)
    }

    /**
     * 从WindowManager移除
     */
    fun removeFromWindowManager() {
        if (isAttachedToWindow) windowManager.removeView(this)
    }
}