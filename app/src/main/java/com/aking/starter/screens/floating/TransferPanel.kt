package com.aking.starter.screens.floating

/**
 * @author Ak
 * 2025/4/9  16:15
 */
import android.content.ClipData
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.drag.actualDragAndDropTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aking.starter.ui.theme.Background
import com.aking.starter.ui.theme.Primary
import com.aking.starter.utils.LocalAndroidViewConfiguration

fun getFileName(uri: Uri, contentResolver: ContentResolver): String {
    var name = uri.toString() // Default to URI string
    val cursor = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    return name
}

fun processClipData(clipData: ClipData, contentResolver: ContentResolver): String {
    val sb = StringBuilder()
    for (i in 0 until clipData.itemCount) {
        val item = clipData.getItemAt(i)
        val mimeType = clipData.description.getMimeType(i)

        when {
            item.text != null -> {
                sb.append("Text: ${item.text}\n")
            }
            item.htmlText != null && mimeType == ClipData.MIMETYPE_TEXT_HTML -> {
                sb.append("HTML: ${item.htmlText}\n") // Displaying raw HTML as text
            }
            item.uri != null -> {
                val fileName = getFileName(item.uri, contentResolver)
                if (mimeType?.startsWith("image/") == true) {
                    sb.append("Image: $fileName\n")
                } else {
                    sb.append("File: $fileName\n")
                }
            }
            else -> {
                sb.append("Unsupported item type\n")
            }
        }
    }

    return if (sb.isNotEmpty()) {
        sb.toString().trim()
    } else {
        "No processable content found."
    }
}

@Composable
fun TransferPanel(
    modifier: Modifier = Modifier,
    initialIsDragging: Boolean = false, // Added for testing
    initialDroppedContent: String = "Drop here" // Added for testing consistency
) {
    val density = LocalDensity.current
    val viewConfiguration = LocalAndroidViewConfiguration.current
    val edgeSlop = remember { with(density) { viewConfiguration.scaledEdgeSlop.toDp() } }
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    var isDragging by remember { mutableStateOf(initialIsDragging) }
    var droppedContent by remember { mutableStateOf(initialDroppedContent) }

    Column(
        modifier = modifier
            .testTag("TransferPanel") // Added test tag
            .padding(horizontal = edgeSlop)
            .size(100.dp)
            .background(
                color = if (isDragging) Primary.copy(alpha = 0.5f) else Background,
                shape = RoundedCornerShape(10.dp)
            )
            .actualDragAndDropTarget(
                onDragStart = { },
                onDragEnter = { isDragging = true },
                onDragExit = { isDragging = false },
                onDrop = { event ->
                    isDragging = false
                    droppedContent = processClipData(event.clipData, contentResolver)
                    true // Indicate that the drop was handled
                }
            )
            .padding(8.dp) // Inner padding for the text
            .verticalScroll(rememberScrollState()), // Make content scrollable if it overflows
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = droppedContent,
            modifier = Modifier.testTag("DroppedContentText"), // Added test tag
            textAlign = TextAlign.Center,
            color = Color.White
            // Text will wrap by default if it's long.
            // Max lines can be set if needed, but scrolling is better for variable content length.
        )
    }
}

@Preview
@Composable
fun TransferPreview() {
    TransferPanel(initialIsDragging = true)
}

@Preview
@Composable
fun TransferPreviewDrop() {
    TransferPanel(initialDroppedContent = "Text: Hello\nFile: test.txt")
}
