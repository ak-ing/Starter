package com.aking.starter.screens.floating

/**
 * @author Ak
 * 2025/4/9  16:15
 */
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aking.starter.ui.theme.Background
import com.aking.starter.utils.LocalAndroidViewConfiguration

@Composable
fun TransferPanel(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val viewConfiguration = LocalAndroidViewConfiguration.current
    val edgeSlop = remember { with(density) { viewConfiguration.scaledEdgeSlop.toDp() } }
    Column(
        modifier.padding(horizontal = edgeSlop)
            .size(100.dp)
            .background(Background, shape = RoundedCornerShape(10.dp))
    ) {
    }
}

@Preview
@Composable
fun TransferPreview() {
    TransferPanel()
}

