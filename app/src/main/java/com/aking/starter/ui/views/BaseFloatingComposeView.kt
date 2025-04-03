package com.aking.starter.ui.views

import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.aking.starter.utils.getScreenSize
import kotlinx.coroutines.launch

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

    /** 屏幕尺寸 */
    protected val screenSize by lazy { windowManager.getScreenSize() }

    private var targetAnimateX = 0
    private var targetAnimateY = 0
    protected var edgeState by mutableStateOf(true)

    /** 拖拽偏移量动画 */
    private val animOffset = Animatable(IntOffset(0, 0), IntOffset.VectorConverter)

    init {
        this.addView(ComposeView(context).apply {
            compositionContext = viewTreeOwners.reComposer
            setViewCompositionStrategy(ViewCompositionStrategy.Default)
            setContent {
                val coroutineScope = rememberCoroutineScope()
                Box(modifier = Modifier.pointerInput(Unit) {
                    detectDragGestures(onDragStart = {
                        targetAnimateX = windowParams.x
                        targetAnimateY = windowParams.y
                        edgeState = false
                    }, onDragEnd = {
                        coroutineScope.launch { returnToTheEdgeOfTheScreen() }
                    }) { _, dragAmount ->
                        coroutineScope.launch { animateTo(dragAmount) }
                    }
                }) { this@BaseFloatingComposeView.Content() }
            }
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

    /**
     * 悬浮窗拖拽
     */
    private suspend fun animateTo(dragAmount: Offset) {
        targetAnimateX = (targetAnimateX + dragAmount.x.toInt()).coerceIn(0, screenSize.x.toInt())
        targetAnimateY = (targetAnimateY + dragAmount.y.toInt()).coerceIn(0, screenSize.y.toInt())
        animOffset.animateTo(IntOffset(targetAnimateX, targetAnimateY)) {
            windowParams.x = value.x
            windowParams.y = value.y
            windowManager.updateViewLayout(this@BaseFloatingComposeView, windowParams)
        }
    }

    /**
     * 松手时，返回到屏幕边缘
     */
    private suspend fun returnToTheEdgeOfTheScreen() {
        //悬浮窗的中心点
        val centerX = targetAnimateX + width / 2
        //在屏幕的左侧，位移到0
        //在屏幕右侧，位移到屏幕宽度-自身宽度（保证右侧贴屏幕）
        val x = if (centerX < screenSize.x / 2f) 0 else (screenSize.x - width).toInt()
        animOffset.animateTo(IntOffset(x, targetAnimateY), tween()) {
            windowParams.x = value.x
            windowManager.updateViewLayout(this@BaseFloatingComposeView, windowParams)
        }
        edgeState = true
    }
}