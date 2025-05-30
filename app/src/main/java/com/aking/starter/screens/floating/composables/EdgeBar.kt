package com.aking.starter.screens.floating.composables

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aking.starter.ui.theme.Background
import com.aking.starter.ui.theme.ColorEdgeEdit

/**
 * Screen edge collapsable bar.
 */
@Composable
fun EdgeBar(
    isLeft: Boolean,
    edgeSlop: Int,
    interactionsEmpty: Boolean,
    enableDrag: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // 折叠条交互动画（touch变宽）
    val widthAnima by animateDpAsState(
        targetValue = with(density) {
            (if (interactionsEmpty) edgeSlop else edgeSlop * 2).toDp()
        },
        label = "edgeBarWidth"
    )
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