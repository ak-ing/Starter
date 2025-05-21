package com.aking.starter.screens.floating

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import android.content.ClipData
import android.content.ClipDescription
import android.content.ContentResolver
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyArray
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

// Renamed for clarity, or could be a separate class. For this exercise, appending tests here.
@RunWith(MockitoJUnitRunner::class)
class TransferPanelLogicTest { // Changed class name for clarity

    @Mock
    private lateinit var mockContentResolver: ContentResolver

    @Mock
    private lateinit var mockUri: Uri // Generic URI mock

    // Mocks for ClipData testing
    @Mock
    private lateinit var mockClipData: ClipData

    @Mock
    private lateinit var mockClipDescription: ClipDescription

    @Mock
    private lateinit var mockItem: ClipData.Item


    @Before
    fun setUp() {
        // Default behavior for mockUri, can be overridden
        `when`(mockUri.toString()).thenReturn("content://com.example/dummy")

        // Common setup for ClipData related mocks
        `when`(mockClipData.description).thenReturn(mockClipDescription)
        // Default to one item, can be overridden
        `when`(mockClipData.itemCount).thenReturn(1)
        `when`(mockClipData.getItemAt(0)).thenReturn(mockItem)
    }

    // Tests for getFileName
    @Test
    fun `getFileName returns display name when cursor is valid and has column`() {
        val specificUri = Uri.parse("content://com.example/specific_file")
        val expectedFileName = "test_file.jpg"
        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
        cursor.addRow(arrayOf(expectedFileName))

        `when`(mockContentResolver.query(eq(specificUri), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(cursor)

        val actualFileName = getFileName(specificUri, mockContentResolver)
        assertEquals(expectedFileName, actualFileName)
    }

    @Test
    fun `getFileName returns uri string when cursor is valid but column is missing`() {
        val specificUri = Uri.parse("content://com.example/file1")
        val cursor = MatrixCursor(arrayOf("another_column"))
        cursor.addRow(arrayOf("some_value"))
        `when`(specificUri.toString()).thenReturn("content://com.example/file1")

        `when`(mockContentResolver.query(eq(specificUri), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(cursor)

        val actualFileName = getFileName(specificUri, mockContentResolver)
        assertEquals("content://com.example/file1", actualFileName)
    }

    @Test
    fun `getFileName returns uri string when cursor is empty`() {
        val specificUri = Uri.parse("content://com.example/file2")
        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
        `when`(specificUri.toString()).thenReturn("content://com.example/file2")

        `when`(mockContentResolver.query(eq(specificUri), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(cursor)

        val actualFileName = getFileName(specificUri, mockContentResolver)
        assertEquals("content://com.example/file2", actualFileName)
    }

    @Test
    fun `getFileName returns uri string when cursor is null`() {
        val specificUri = Uri.parse("content://com.example/file3")
        `when`(specificUri.toString()).thenReturn("content://com.example/file3")
        `when`(mockContentResolver.query(eq(specificUri), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(null)

        val actualFileName = getFileName(specificUri, mockContentResolver)
        assertEquals("content://com.example/file3", actualFileName)
    }
    
    @Test
    fun `getFileName returns uri string when display name is null in cursor`() {
        val specificUri = Uri.parse("content://com.example/file4")
        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
        cursor.addRow(arrayOf<String?>(null))
        `when`(specificUri.toString()).thenReturn("content://com.example/file4")

        `when`(mockContentResolver.query(eq(specificUri), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(cursor)
        
        val actualFileName = getFileName(specificUri, mockContentResolver)
        assertEquals("content://com.example/file4", actualFileName)
    }

    // Tests for processClipData
    @Test
    fun `processClipData handles plain text drop`() {
        val text = "Hello World"
        `when`(mockItem.text).thenReturn(text)
        `when`(mockItem.htmlText).thenReturn(null)
        `when`(mockItem.uri).thenReturn(null)
        `when`(mockClipDescription.getMimeType(0)).thenReturn(ClipDescription.MIMETYPE_TEXT_PLAIN)

        val result = processClipData(mockClipData, mockContentResolver)
        assertEquals("Text: Hello World", result)
    }

    @Test
    fun `processClipData handles HTML text drop`() {
        val html = "<p>Hello HTML</p>"
        `when`(mockItem.text).thenReturn(null)
        `when`(mockItem.htmlText).thenReturn(html)
        `when`(mockItem.uri).thenReturn(null)
        `when`(mockClipDescription.getMimeType(0)).thenReturn(ClipDescription.MIMETYPE_TEXT_HTML)

        val result = processClipData(mockClipData, mockContentResolver)
        assertEquals("HTML: <p>Hello HTML</p>", result)
    }

    @Test
    fun `processClipData handles image URI drop with display name`() {
        val imageName = "image.png"
        val imageUri = Uri.parse("content://com.example/image.png")
        `when`(mockItem.text).thenReturn(null)
        `when`(mockItem.htmlText).thenReturn(null)
        `when`(mockItem.uri).thenReturn(imageUri)
        `when`(mockClipDescription.getMimeType(0)).thenReturn("image/png")

        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
        cursor.addRow(arrayOf(imageName))
        `when`(mockContentResolver.query(eq(imageUri), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(cursor)

        val result = processClipData(mockClipData, mockContentResolver)
        assertEquals("Image: image.png", result)
    }

    @Test
    fun `processClipData handles file URI drop with display name`() {
        val fileName = "document.pdf"
        val fileUri = Uri.parse("content://com.example/document.pdf")
        `when`(mockItem.text).thenReturn(null)
        `when`(mockItem.htmlText).thenReturn(null)
        `when`(mockItem.uri).thenReturn(fileUri)
        `when`(mockClipDescription.getMimeType(0)).thenReturn("application/pdf") // Generic file type

        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
        cursor.addRow(arrayOf(fileName))
        `when`(mockContentResolver.query(eq(fileUri), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(cursor)

        val result = processClipData(mockClipData, mockContentResolver)
        assertEquals("File: document.pdf", result)
    }

    @Test
    fun `processClipData handles URI without display name`() {
        val fileUri = Uri.parse("content://com.example/data_no_name")
        val uriString = fileUri.toString()
        `when`(mockItem.text).thenReturn(null)
        `when`(mockItem.htmlText).thenReturn(null)
        `when`(mockItem.uri).thenReturn(fileUri)
        `when`(mockClipDescription.getMimeType(0)).thenReturn("application/octet-stream")

        // Simulate no display name found
        `when`(mockContentResolver.query(eq(fileUri), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(null)

        val result = processClipData(mockClipData, mockContentResolver)
        assertEquals("File: $uriString", result)
    }
    
    @Test
    fun `processClipData handles multiple items drop`() {
        val text = "First item"
        val fileName = "second_item.txt"
        val fileUri = Uri.parse("content://com.example/second_item.txt")

        val mockItem1 = mock<ClipData.Item> {
            on { it.text } doReturn text
            on { it.htmlText } doReturn null
            on { it.uri } doReturn null
        }
        val mockItem2 = mock<ClipData.Item> {
            on { it.text } doReturn null
            on { it.htmlText } doReturn null
            on { it.uri } doReturn fileUri
        }

        `when`(mockClipData.itemCount).thenReturn(2)
        `when`(mockClipData.getItemAt(0)).thenReturn(mockItem1)
        `when`(mockClipData.getItemAt(1)).thenReturn(mockItem2)
        `when`(mockClipDescription.getMimeType(0)).thenReturn(ClipDescription.MIMETYPE_TEXT_PLAIN)
        `when`(mockClipDescription.getMimeType(1)).thenReturn("text/plain") // Example, could be anything

        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
        cursor.addRow(arrayOf(fileName))
        `when`(mockContentResolver.query(eq(fileUri), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(cursor)

        val result = processClipData(mockClipData, mockContentResolver)
        assertEquals("Text: First item\nFile: second_item.txt", result)
    }

    @Test
    fun `processClipData handles unsupported content drop`() {
        `when`(mockItem.text).thenReturn(null)
        `when`(mockItem.htmlText).thenReturn(null)
        `when`(mockItem.uri).thenReturn(null) // No actual content
        `when`(mockClipDescription.getMimeType(0)).thenReturn("application/vnd.custom-type")

        val result = processClipData(mockClipData, mockContentResolver)
        assertEquals("Unsupported item type", result)
    }

    @Test
    fun `processClipData handles empty ClipData`() {
        `when`(mockClipData.itemCount).thenReturn(0)
        val result = processClipData(mockClipData, mockContentResolver)
        assertEquals("No processable content found.", result)
    }
}
