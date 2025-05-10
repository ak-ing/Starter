package com.aking.starter.screens.floating

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aking.starter.ui.theme.Background
import com.aking.starter.ui.views.BaseFloatingComposeView
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * @author Ak
 * 2025/3/21  9:12
 */
class StarterFloatingComposeView(context: Context) : BaseFloatingComposeView(context) {

    override fun shrinkToEdge(): Boolean = true     // 是否支持在屏幕边缘折叠

    @Composable
    override fun FloatingContent() {
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        val width = remember { with(density) { 100.dp.toPx() } }
        Card(
            colors = CardDefaults.cardColors(containerColor = Background.copy(alpha = .5f)),
            modifier = Modifier
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onDragStart() },
                        onDragEnd = { },
                        onDragCancel = { },
                        onDrag = { change: PointerInputChange, dragAmount: Offset ->
                            scope.launch { onDrag(change, dragAmount) }
                        })
                }
//                .layout { measurable, constraints ->
//                    val placeable = measurable.measure(constraints)
//                    val scale = if (animOffset.value.x < 0) {
//                        1 - animOffset.value.x.absoluteValue / width
//                    } else {
//                        1f
//                    }
//                    val width = (placeable.width * scale).toInt()
//                    val height = (placeable.height * scale).toInt()
//
//                    layout(width, height) {
//                        placeable.placeRelative(0, 0)
//                    }
//                }
                .graphicsLayer {
                    if (animOffset.value.x < 0) {
                        val scale = 1 - animOffset.value.x.absoluteValue / width
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = scale
                        scaleY = scale
                        this@StarterFloatingComposeView.scaleX = scale
                        this@StarterFloatingComposeView.scaleY = scale
                    }
                }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddLink, contentDescription = "Add Link", modifier = Modifier.padding(8.dp))
                Text("文件中转站", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    /**
     * 屏幕边缘折叠
     */
    @Composable
    fun EdgeBar(modifier: Modifier = Modifier) {
        val density = LocalDensity.current
        // 折叠条交互动画（touch变宽）
        val width = remember { with(density) { edgeSlop.toDp() } }
        val padding = remember { with(density) { (edgeSlop * 3).toDp() } }

        Box(
            modifier = modifier
                .width(width + padding)
                .height(100.dp)
                .systemGestureExclusion()
                .padding(start = if (isLeft) 0.dp else padding, end = if (isLeft) padding else 0.dp)
                .background(Background, shape = RoundedCornerShape(percent = 50))
        )
    }
}


