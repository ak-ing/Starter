package com.aking.starter.screens.floating

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aking.starter.ui.views.BaseFloatingComposeView

/**
 * @author Ak
 * 2025/3/21  9:12
 */
class StarterFloatingComposeView(context: Context) : BaseFloatingComposeView(context) {

    @Composable
    override fun Content() {
        var text by remember { mutableStateOf("text") }
        Column(
            Modifier
                .size(100.dp)
                .background(Color.Blue)
        ) {
            Text(
                text, Modifier
                    .background(Color.Gray)
                    .clickable {
                        text = "啊啊啊啊啊啊"
                    })
        }
    }
}