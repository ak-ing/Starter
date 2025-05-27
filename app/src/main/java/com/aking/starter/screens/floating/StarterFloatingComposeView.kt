package com.aking.starter.screens.floating

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aking.starter.models.DropItem
import com.aking.starter.screens.floating.drag.DragDropManager
import com.aking.starter.ui.theme.Background
import com.aking.starter.ui.theme.ColorEdgeEdit
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
                enableDrag = dragState, Modifier
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
                        })
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
                        .fillMaxSize()
                        .background(shape = CardDefaults.shape, color = Background),
                    onStarted = { scope.launch { expand() } },
                    onDrop = { event ->
                        viewModel.reducer(FloatingIntent.OnDrop(context, event))
                    }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .selectableGroup()
                    ) {
                        // 空状态显示
                        item(key = "empty_state") {
                            EmptyState(visible = state.groupedItems.isEmpty())
                        }

                        // 使用 ViewModel 中的已分组数据
                        state.groupedItems.forEach { (groupId, itemsInGroup) ->
                            item(key = "group_header_$groupId") {
                                GroupHeader(
                                    groupId = groupId,
                                    itemCount = itemsInGroup.size,
                                    onToggleExpand = {
                                        viewModel.reducer(FloatingIntent.ToggleGroupExpansion(groupId))
                                    }
                                )
                            }

                            if (groupId in state.expandedGroups) {
                                items(
                                    items = itemsInGroup,
                                    key = { "item-${it.id}" }
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
        }
    }

    override suspend fun handlerEdgeState() {
        if (edgeState) {
            deltaX = 0f
            dragTo(isRelease = true)
        } else {
            deltaX = if (isLeft) {
                contentWidthPx
            } else {
                -contentWidthPx
            }
            dragTo(isRelease = true)
        }
    }

    private suspend fun dragTo(delta: Float = 0f, isRelease: Boolean = false) {
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

        val animas = mutableListOf<suspend () -> Unit>()
        if (runEdge) {
            animas.add {
                val edgeTarget = if (isLeft) {
                    -deltaX.coerceIn(0f, edgeSlop.toFloat())
                } else {
                    (-deltaX).coerceIn(0f, edgeSlop.toFloat())
                }
                edgeOffset.animateTo(edgeTarget)
            }
        }
        if (runContent) {
            animas.add {
                val target = if (isLeft) {
                    (-contentWidthPx + deltaX).coerceIn(-contentWidthPx, 0f)
                } else {
                    (contentWidthPx + deltaX).coerceIn(0f, contentWidthPx)
                }
                if (isRelease) {
                    animOffset.animateTo(target, tween())
                } else {
                    animOffset.animateTo(target)
                }
            }
        }
        if (isRelease) {
            animas.reverse()
        }
        animas.forEach { it() }
    }

    /**
     * 屏幕边缘折叠
     */
    @Composable
    fun EdgeBar(enableDrag: Boolean, modifier: Modifier = Modifier) {
        val density = LocalDensity.current
        // 折叠条交互动画（touch变宽）
        val widthAnima by animateDpAsState(with(density) {
            (if (interactions.isEmpty()) edgeSlop else edgeSlop * 2).toDp()
        })
        val padding = remember { with(density) { (edgeSlop * 3).toDp() } }

        Box(
            modifier = modifier
                .width(widthAnima + padding)
                .height(100.dp)
                .systemGestureExclusion()
                .padding(start = if (isLeft) 0.dp else padding, end = if (isLeft) padding else 0.dp)
                .background(if (enableDrag) ColorEdgeEdit else Background, shape = RoundedCornerShape(percent = 50))
        )
    }

    @Composable
    private fun EmptyState(
        visible: Boolean,
        modifier: Modifier = Modifier
    ) {
        AnimatedVisibility(visible = visible) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = modifier.fillMaxSize()
            ) {
                Icon(Icons.Default.AddLink, "添加")
                Text("文件中转站", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    @Composable
    private fun GroupHeader(
        groupId: Long,
        itemCount: Int,
        onToggleExpand: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(MaterialTheme.shapes.small)
                    .selectable(
                        selected = false,
                        onClick = onToggleExpand
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Group $groupId ($itemCount)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "展开/收起",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    @Composable
    private fun DropItemContent(
        item: DropItem,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.small,
            tonalElevation = 1.dp
        ) {
            when (item) {
                is DropItem.Text -> Text(
                    text = item.text,
                    maxLines = 2,
                    modifier = Modifier.padding(8.dp)
                )

                is DropItem.Image -> AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small)
                )

                is DropItem.FileItem -> Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLink,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.uri.lastPathSegment ?: "未知文件",
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    @Composable
    private fun FloatingDropTarget(
        modifier: Modifier = Modifier,
        onStarted: (DragAndDropEvent) -> Unit = {},
        onDrop: (DragAndDropEvent) -> Unit,
        content: @Composable () -> Unit
    ) {
        val dropTarget = remember {
            object : DragAndDropTarget {
                override fun onStarted(event: DragAndDropEvent) = onStarted(event)
                override fun onEnded(event: DragAndDropEvent) {}
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    onDrop(event)
                    return true
                }
            }
        }
        Box(
            modifier = modifier.dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dropTarget
            )
        ) {
            content()
        }
    }
}



