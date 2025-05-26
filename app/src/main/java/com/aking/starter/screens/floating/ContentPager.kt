package com.aking.starter.screens.floating

/**
 * @author Created by Ak on 2025-05-19 19:58.
 */
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue

@Composable
fun ContentPager() {
    val pagerState = rememberPagerState(pageCount = { 4 })
    HorizontalPager(state = pagerState) { page ->
        Card(
            Modifier
                .size(200.dp)
                .graphicsLayer {
                    // Calculate the absolute offset for the current page from the
                    // scroll position. We use the absolute value which allows us to mirror
                    // any effects for both directions
                    Log.i(
                        "TAG",
                        "FloatingContent: ${pagerState.currentPage}  $page   ${pagerState.currentPageOffsetFraction}"
                    )
                    val pageOffset = (
                            (pagerState.currentPage - page) + pagerState
                                .currentPageOffsetFraction
                            ).absoluteValue

                    // We animate the alpha, between 50% and 100%
                    alpha = lerp(
                        start = 0.5f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                }
        ) {
            // Card content
            Box(
                Modifier
                    .size(200.dp)
                    .background(Color.DarkGray)
            ) { }
        }
    }
}

@Preview
@Composable
fun ContentPagerPreview() {
    ContentPager()
}

