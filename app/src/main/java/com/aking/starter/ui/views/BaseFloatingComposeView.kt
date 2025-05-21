package com.aking.starter.ui.views

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import android.view.DragEvent

/**
 *  A base class for creating floating views using Jetpack Compose.
 *
 *  This class handles the lifecycle, window management, and drag-and-drop functionality
 *  for a floating view that is rendered using Compose.
 * @author Ak
 * 2025/3/20  17:01
 */
abstract class BaseFloatingComposeView(context: Context) : FrameLayout(context), ViewModelStoreOwner, LifecycleOwner {

    private val windowManager: WindowManager by lazy {
        context.getSystemService(WindowManager::class.java)
    }

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
    private var targetAnimateY = (screenSize.y / 2).toInt()

    /** 屏幕边缘折叠状态 */
    val edgeState get() = _edgeState
    private var _edgeState by mutableStateOf(false)

    /** 当前停靠方向 */
    val direction get() = _direction
    private var _direction by mutableIntStateOf(Gravity.START)
    val isLeft get() = direction == Gravity.START

    /** 拖拽偏移量动画 */
    private val animOffset = Animatable(IntOffset(targetAnimateX, targetAnimateY), IntOffset.VectorConverter)

    /** ViewConfiguration */
    protected val viewConfiguration = ViewConfiguration.get(context)
    protected val touchSlop = viewConfiguration.scaledTouchSlop
    protected val edgeSlop = viewConfiguration.scaledEdgeSlop

    /** 交互状态 */
    protected val interactionSource = MutableInteractionSource()
    protected val interactions = mutableStateListOf<Interaction>()

    init {
        this.addView(ComposeView(context).apply {
            _edgeState = shrinkToEdge()
            compositionContext = viewTreeOwners.reComposer
            setViewCompositionStrategy(ViewCompositionStrategy.Default)
            setContent {
                val scope = rememberCoroutineScope()
                val start = remember { DragInteraction.Start() }
                Box(
                    modifier = Modifier
                        .hoverable(interactionSource)
                        .clickable(interactionSource, indication = null, onClick = {})
                        .pointerInput(edgeState) {
                            if (!edgeState) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onDragStart(scope, start) },
                                onDragEnd = { onDragEnd(scope, start) },
                                onDragCancel = { scope.launch { interactionSource.emit(DragInteraction.Cancel(start)) } },
                                onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                    scope.launch { onDrag(change, dragAmount) }
                                })
                        }) {
                    CompositionLocalProvider(LocalAndroidViewConfiguration provides viewConfiguration) {
                        FloatingContent()
                    }
                }
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                interactions.add(interaction)
                            }

                            is PressInteraction.Release -> {
                                interactions.remove(interaction.press)
                            }

                            is PressInteraction.Cancel -> {
                                interactions.remove(interaction.press)
                            }

                            is HoverInteraction.Enter -> {
                                interactions.add(interaction)
                            }

                            is HoverInteraction.Exit -> {
                                interactions.remove(interaction.enter)
                            }

                            is DragInteraction.Start -> {
                                interactions.add(interaction)
                            }

                            is DragInteraction.Stop -> {
                                interactions.remove(interaction.start)
                            }

                            is DragInteraction.Cancel -> {
                                interactions.remove(interaction.start)
                            }
                        }
                    }
                }
            }
        })
        this.addOnAttachStateChangeListener(viewTreeOwners)
        this.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    // Potentially expand the view when a drag starts anywhere
                    // This allows the TransferPanel to become visible
                    handleGlobalDragStart()
                    true // Indicate interest in future drag events
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    // Ensure expansion if drag enters the (possibly shrunk) view
                    handleGlobalDragStart()
                    true
                }
                // Add other cases if needed, e.g., ACTION_DRAG_ENDED to auto-shrink
                else -> true // Default to true to keep receiving events
            }
        }
    }

    @Composable
    protected abstract fun FloatingContent()

    /** Called when a global drag operation (ACTION_DRAG_STARTED or ACTION_DRAG_ENTERED) is detected. */
    protected abstract fun handleGlobalDragStart()

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
    protected suspend fun shrink() {
        _edgeState = true
        handlerEdgeState()
    }

    /** 展开悬浮窗 */
    protected suspend fun expand() {
        _edgeState = false
        handlerEdgeState()
    }

    protected open suspend fun handlerEdgeState() {}

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
        y = targetAnimateY
        // FLAG_NOT_FOCUSABLE allows events to reach views behind this one.
        // FLAG_WATCH_OUTSIDE_TOUCH is for touch events, not directly for drag.
        // For the view to be a drag target for events outside its drawn bounds,
        // it needs to not be FLAG_NOT_TOUCH_MODAL.
        // However, the primary goal here is to make the floating view *aware* of a global drag
        // to trigger its own expansion, not necessarily to be the direct system-level drop target
        // when shrunk. The Compose `actualDragAndDropTarget` handles the drop when expanded.
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS // Allows view to extend out of screen
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

    /**
     * Handles the start of a drag gesture.
     */
    private fun onDragStart(coroutineScope: CoroutineScope, dragInteraction: DragInteraction.Start) {
        coroutineScope.launch {
            interactionSource.emit(dragInteraction)
            targetAnimateX = windowParams.x
            targetAnimateY = windowParams.y
        }
    }

    /**
     * Handles the end of a drag gesture.
     */
    private fun onDragEnd(coroutineScope: CoroutineScope, dragInteraction: DragInteraction.Start) {
        coroutineScope.launch {
            returnToTheEdgeOfTheScreen()
            interactionSource.emit(DragInteraction.Stop(dragInteraction))
        }
    }

    /**
     * Handles a drag event.
     */
    private suspend fun onDrag(change: PointerInputChange, dragAmount: Offset) {
        animateTo(dragAmount)
        change.consume()
    }

    /**
     * 松手时，返回到屏幕边缘
     */
    private suspend fun returnToTheEdgeOfTheScreen(width: Int = this.width) {
        Log.i("TAG", "returnToTheEdgeOfTheScreen: ")
        //悬浮窗的中心点
        val centerX = targetAnimateX + width / 2
        val calculateDirection = calculateDirection(centerX)
        // gravity无缝切换
        if (windowParams.gravity != (calculateDirection or Gravity.TOP)) {
            animOffset.snapTo(IntOffset((screenSize.x - targetAnimateX - width).toInt(), targetAnimateY))
            windowParams.gravity = calculateDirection or Gravity.TOP
        }
        _direction = calculateDirection
        animOffset.animateTo(IntOffset(0, targetAnimateY), tween()) {
            windowParams.x = value.x
            windowManager.updateViewLayout(this@BaseFloatingComposeView, windowParams)
        }

    }

    /** 根据中心点和当前权重计算方向 */
    private fun calculateDirection(centerX: Int) = when (direction) {
        Gravity.START -> {
            if (centerX < screenSize.x / 2f) Gravity.START else Gravity.END
        }

        Gravity.END -> {
            if (centerX < screenSize.x / 2f) Gravity.END else Gravity.START
        }

        else -> error("direction error")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_OUTSIDE) {
            viewTreeOwners.runRecomposeScope.launch {
                shrink()
            }
        }
        return super.onTouchEvent(event)
    }
}