package com.aking.starter.ui.views

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.aking.starter.utils.LocalAndroidViewConfiguration
import com.aking.starter.utils.getScreenSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 *  A base class for creating floating views using Jetpack Compose.
 *
 *  This class handles the lifecycle, window management, and drag-and-drop functionality
 *  for a floating view that is rendered using Compose.
 * @author Ak
 * 2025/3/20  17:01
 */
abstract class BaseFloatingComposeView(context: Context) : FrameLayout(context), ViewModelStoreOwner, LifecycleOwner {

    private val windowManager: WindowManager by lazy { context.getSystemService(WindowManager::class.java) }

    // viewTreeOwners
    private val viewTreeOwners = FloatingViewTreeOwners()

    // ViewModelStore
    override val viewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = viewTreeOwners.lifecycle

    /** 屏幕尺寸 */
    protected val screenSize by lazy { windowManager.getScreenSize() }

    /** The target X coordinate for animation. */
    private var targetAnimateX = 0

    /** The target Y coordinate for animation. */
    private var targetAnimateY = 0

    /** 屏幕边缘折叠状态 */
    val edgeState get() = _edgeState
    private var _edgeState by mutableStateOf(false)

    /** 当前停靠方向 */
    val direction get() = _direction
    private var _direction by mutableIntStateOf(Gravity.START)

    /** 拖拽偏移量动画 */
    private val animOffset = Animatable(IntOffset(0, 0), IntOffset.VectorConverter)

    /** ViewConfiguration */
    private val androidViewConfiguration = ViewConfiguration.get(context)
    private val touchSlop = androidViewConfiguration.scaledTouchSlop

    init {
        this.addView(ComposeView(context).apply {
            _edgeState = shrinkToEdge()
            compositionContext = viewTreeOwners.reComposer
            setViewCompositionStrategy(ViewCompositionStrategy.Default)
            setContent {
                val coroutineScope = rememberCoroutineScope()
                Box(modifier = Modifier.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onDragStart() },
                        onDragEnd = { onDragEnd(coroutineScope) },
                        onDrag = { change: PointerInputChange, dragAmount: Offset ->
                            coroutineScope.launch { onDrag(change, dragAmount) }
                        })
                }) {
                    CompositionLocalProvider(
                        LocalAndroidViewConfiguration provides androidViewConfiguration
                    ) {
                        FloatingContent()
                    }
                }
            }
        })
        this.addOnAttachStateChangeListener(viewTreeOwners)
    }

    @Composable
    protected abstract fun FloatingContent()

    /** 是否支持在屏幕边缘折叠 */
    protected open fun shrinkToEdge(): Boolean = false

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

    /** 收缩悬浮窗 */
    protected fun shrink() {
        _edgeState = true
    }

    /** 展开悬浮窗 */
    protected fun expand() {
        _edgeState = false
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
        format = PixelFormat.TRANSLUCENT
    }

    /**
     * 悬浮窗拖拽动画
     */
    private suspend fun animateTo(dragAmount: Offset) {
        val dragX = if (direction == Gravity.START) dragAmount.x else -dragAmount.x
        targetAnimateX = (targetAnimateX + dragX.toInt()).coerceIn(0, screenSize.x.toInt())
        targetAnimateY = (targetAnimateY + dragAmount.y.toInt()).coerceIn(0, screenSize.y.toInt())
        animOffset.animateTo(IntOffset(targetAnimateX, targetAnimateY)) {
            windowParams.x = value.x
            windowParams.y = value.y
            windowManager.updateViewLayout(this@BaseFloatingComposeView, windowParams)
        }
    }

    private var deltaX = 0f
    private var deltaY = 0f
    private var canDrag: Boolean? = null

    /**
     * Handles the start of a drag gesture.
     */
    private fun onDragStart() {
        targetAnimateX = windowParams.x
        targetAnimateY = windowParams.y
        // Reset these as they are used to detect if a drag should start
        deltaX = 0f
        deltaY = 0f
        canDrag = if (shrinkToEdge()) {
            if (edgeState) false else null
        } else {
            true
        }
    }

    /**
     * Handles the end of a drag gesture.
     */
    private fun onDragEnd(coroutineScope: CoroutineScope) {
        coroutineScope.launch { returnToTheEdgeOfTheScreen() }
    }

    /**
     * Handles a drag event.
     */
    private suspend fun onDrag(change: PointerInputChange, dragAmount: Offset) {
        if (canDrag == true) {
            animateTo(dragAmount)
            change.consume()
        }
        if (canDrag != null) {
            return
        }
        deltaX += dragAmount.x
        deltaY += dragAmount.y
        if (deltaX.absoluteValue >= touchSlop || deltaY.absoluteValue >= touchSlop) {
            canDrag = if (direction == Gravity.START && deltaX <= -touchSlop) {
                // 向左侧收缩
                shrink()
                false
            } else if (direction == Gravity.END && deltaX >= touchSlop) {
                // 向右侧收缩
                shrink()
                false
            } else {
                true
            }
        }
    }

    /**
     * 松手时，返回到屏幕边缘
     */
    private suspend fun returnToTheEdgeOfTheScreen(width: Int = this.width) {
        //悬浮窗的中心点
        val centerX = targetAnimateX + width / 2
        calculateDirection(centerX)
        // gravity无缝切换
        if (windowParams.gravity != (direction or Gravity.TOP)) {
            animOffset.snapTo(IntOffset((screenSize.x - targetAnimateX - width).toInt(), targetAnimateY))
            windowParams.gravity = direction or Gravity.TOP
        }
        animOffset.animateTo(IntOffset(0, targetAnimateY), tween()) {
            windowParams.x = value.x
            windowManager.updateViewLayout(this@BaseFloatingComposeView, windowParams)
        }
    }

    /** 根据中心点和当前权重计算方向 */
    private fun calculateDirection(centerX: Int) = when (direction) {
        Gravity.START -> {
            _direction = if (centerX < screenSize.x / 2f) Gravity.START else Gravity.END
        }

        Gravity.END -> {
            _direction = if (centerX < screenSize.x / 2f) Gravity.END else Gravity.START
        }

        else -> error("direction error")
    }

}