package com.aking.starter.screens.floating

import android.app.Activity
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.aking.starter.screens.floating.composables.DropItemContent
import com.aking.starter.screens.floating.composables.EdgeBar
import com.aking.starter.screens.floating.composables.EmptyState
import com.aking.starter.screens.floating.composables.FloatingDropTarget
import com.aking.starter.screens.floating.drag.DragDropManager
import com.aking.starter.ui.theme.Background
import com.aking.starter.ui.views.BaseFloatingComposeView
import com.aking.starter.utils.dpValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * @author Ak
 * 2025/3/21  9:12
 */
class StarterFloatingComposeView(context: Context) : BaseFloatingComposeView(context) {

    // 记住交互事件（松手时需要移除）
    private val pressInteraction = PressInteraction.Press(Offset.Zero)
    private val contentWidthPx = 150.dpValue + edgeSlop * 2

    // 滑动距离
    private var deltaX = 0f

    // 内容是否显示
    private var visibleContent = false

    // EdgeBar偏移动画
    private val edgeOffset = Animatable(0f)

    //内容偏移动画
    private val animOffset = Animatable(-contentWidthPx)
    private val alignment by derivedStateOf {
        if (isLeft) Alignment.TopStart else Alignment.TopEnd
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun FloatingContent() {
        val viewModel = remember { FloatingViewModel() }
        val dragDropManager = remember { DragDropManager() }
        val state = viewModel.uiState
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        Box(contentAlignment = alignment) {
            val density = LocalDensity.current
            // 拖动状态（长按拖拽）
            val dragState by interactionSource.collectIsDraggedAsState()

            LaunchedEffect(direction) {
                if (isLeft) {
                    animOffset.snapTo(-contentWidthPx)
                } else {
                    animOffset.snapTo(contentWidthPx)
                }
            }

            var job: Job? = remember { null }
            val draggableState = rememberDraggableState {
                job = scope.launch { dragTo(delta = it) }
            }
            // 屏幕边缘折叠条
            EdgeBar(
                isLeft = isLeft,
                edgeSlop = edgeSlop,
                interactionsEmpty = interactions.isEmpty(),
                enableDrag = dragState,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = edgeOffset.value
                        alpha = 1f - edgeOffset.value.absoluteValue / edgeSlop.toFloat()
                    }
                    .draggable(
                        enabled = !dragState,
                        orientation = Orientation.Horizontal,
                        state = draggableState,
                        onDragStarted = { interactionSource.emit(pressInteraction) },
                        onDragStopped = {
                            job?.cancel()
                            interactionSource.emit(PressInteraction.Release(pressInteraction))
                            if (deltaX.absoluteValue >= contentWidthPx / 2) {
                                expand()
                            } else {
                                shrink()
                            }
                        }
                    )
            )

            val measureResults: MutableMap<Boolean, MeasureResult> = remember { mutableMapOf() }
            Box(
                modifier = Modifier
                    // 通过 layout 动态裁剪布局边界
                    .layout { measurable, constraints ->
                        // 计算实际可见区域
                        visibleContent = if (isLeft) {
                            (contentWidthPx + animOffset.value) > 0 && !dragState
                        } else {
                            (contentWidthPx - animOffset.value) > 0 && !dragState
                        }
                        measureResults.getOrPut(visibleContent) {
                            val placeable = measurable.measure(constraints)
                            layout(if (visibleContent) placeable.width else 0, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                    }
                    .graphicsLayer {
                        translationX = animOffset.value
                        alpha = 1f - (animOffset.value.absoluteValue / contentWidthPx)
                    }
                    .padding(horizontal = with(density) { edgeSlop.toDp() })
                    .width(150.dp)
            ) {
                FloatingDropTarget(
                    modifier = Modifier
                        .height(300.dp)
                        .background(shape = CardDefaults.shape, color = Background),
                    onStarted = { scope.launch { expand() } },
                    onDrop = { event ->
                        ActivityCompat.requestDragAndDropPermissions(context as Activity, event.toAndroidDragEvent())
                        viewModel.reducer(FloatingIntent.OnDrop(context.contentResolver, event))
                    }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 空状态显示
                        item(key = "empty_state") {
                            EmptyState(visible = state.items.isEmpty())
                        }
                        items(
                            items = state.items,
                            key = { it.id }
                        ) { item ->
                            DropItemContent(
                                item = item,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .animateItem()
                            )
                        }
                    }
                }
            }
        }
    }

    override suspend fun handlerEdgeState() {
        if (edgeState) {
            deltaX = 0f
            dragTo(isRelease = true, reverse = true)
        } else {
            deltaX = if (isLeft) {
                contentWidthPx
            } else {
                -contentWidthPx
            }
            dragTo(isRelease = true, reverse = false)
        }
    }

    private suspend fun dragTo(delta: Float = 0f, isRelease: Boolean = false, reverse: Boolean = false) {
        if (isRelease.not()) deltaX += delta
        var runEdge = isRelease
        var runContent = isRelease
        if (isLeft && deltaX >= 0) {
            if (deltaX - delta <= edgeSlop) {
                runEdge = true
            } else {
                runContent = true
            }
        } else if (!isLeft && deltaX <= 0) {
            if (deltaX - delta >= -edgeSlop && !visibleContent) {
                runEdge = true
            } else {
                runContent = true
            }
        }

        val animationActions = mutableListOf<suspend () -> Unit>()
        if (runEdge) {
            animationActions.add {
                val edgeTarget = if (isLeft) {
                    -deltaX.coerceIn(0f, edgeSlop.toFloat())
                } else {
                    (-deltaX).coerceIn(0f, edgeSlop.toFloat())
                }
                if (reverse) {
                    edgeOffset.animateTo(
                        edgeTarget,
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                    )
                } else {
                    edgeOffset.animateTo(edgeTarget)
                }
            }
        }
        if (runContent) {
            animationActions.add {
                val target = if (isLeft) {
                    (-contentWidthPx + deltaX).coerceIn(-contentWidthPx, 0f)
                } else {
                    (contentWidthPx + deltaX).coerceIn(0f, contentWidthPx)
                }
                if (isRelease) {
                    animOffset.animateTo(target, tween(easing = FastOutSlowInEasing))
                } else {
                    animOffset.animateTo(target)
                }
            }
        }
        if (reverse) {
            animationActions.reverse()
        }
        animationActions.forEach { it() }
    }
}



