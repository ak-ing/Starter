package com.aking.starter.ui.views

import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.aking.starter.utils.getScreenSize

/**
 * @author Ak
 * 2025/3/20  17:01
 */
abstract class BaseFloatingComposeView(context: Context) : FrameLayout(context), ViewModelStoreOwner, LifecycleOwner {

    protected val windowManager: WindowManager by lazy { context.getSystemService(WindowManager::class.java) }

    // viewTreeOwners
    private val viewTreeOwners = FloatingViewTreeOwners()

    // ViewModelStore
    override val viewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = viewTreeOwners.lifecycle

    init {
        this.addView(ComposeView(context).apply {
            compositionContext = viewTreeOwners.reComposer
            setViewCompositionStrategy(ViewCompositionStrategy.Default)
            setContent { this@BaseFloatingComposeView.Content() }
        })
        this.addOnAttachStateChangeListener(viewTreeOwners)
    }

    @Composable
    protected abstract fun Content()

    /**
     * 添加到WindowManager
     */
    open fun addToWindowManager() {
        if (isAttachedToWindow) return
        windowManager.addView(this, windowParams)
    }

    /**
     * 从WindowManager移除
     */
    open fun removeFromWindowManager() {
        if (!isAttachedToWindow) return
        windowManager.removeView(this)
    }

    fun release() {
        removeFromWindowManager()
        viewTreeOwners.release()
        viewModelStore.clear()
    }

    /**
     * 悬浮窗参数
     */
    protected val windowParams = WindowManager.LayoutParams().apply {
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

    /** 屏幕尺寸 */
    protected val screenSize by lazy { windowManager.getScreenSize() }

    protected fun returnToTheEdgeOfTheScreen() {
        //悬浮窗的中心点
        val centerX = (2 * windowParams.x + width) / 2
        //在屏幕的左侧，位移到0
        if (centerX < screenSize.x / 2f) {
            windowParams.x = 0
        } else {
            //在屏幕右侧，位移到屏幕宽度-自身宽度（保证右侧贴屏幕）
            windowParams.x = (screenSize.x - width).toInt()
        }
        windowManager.updateViewLayout(this, windowParams)
    }
}