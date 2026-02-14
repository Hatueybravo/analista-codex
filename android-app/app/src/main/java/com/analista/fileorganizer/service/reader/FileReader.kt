package com.analista.fileorganizer.service.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class FileReader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Read any file and extract its text content.
     */
    suspend fun readFile(file: File): FileContent {
        val ext = file.extension.lowercase()
        return when (ext) {
            "pdf" -> readPdf(file)
            "txt", "csv", "log", "md", "json", "xml", "html" -> readTextFile(file)
            "doc", "docx" -> readWordDocument(file)
            "jpg", "jpeg", "png", "bmp", "webp" -> readImageWithOcr(file)
            "xls", "xlsx" -> readExcelFile(file)
            else -> FileContent(
                fileName = file.name,
                text = "[Binary file - cannot extract text from .${ext} files]",
                type = ContentType.BINARY,
                pageCount = 0
            )
        }
    }

    /**
     * Extract text from a PDF using Android's PdfRenderer + ML Kit OCR.
     */
    suspend fun readPdf(file: File): FileContent = withContext(Dispatchers.IO) {
        val text = StringBuilder()
        var pageCount = 0

        try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            pageCount = renderer.pageCount

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(
                    page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                // OCR the rendered page
                val pageText = recognizeText(bitmap)
                text.appendLine("--- Page ${i + 1} ---")
                text.appendLine(pageText)
                text.appendLine()

                bitmap.recycle()
            }

            renderer.close()
            fd.close()
        } catch (e: Exception) {
            text.appendLine("Error reading PDF: ${e.message}")
        }

        FileContent(
            fileName = file.name,
            text = text.toString(),
            type = ContentType.PDF,
            pageCount = pageCount
        )
    }

    /**
     * Read a plain text file.
     */
    suspend fun readTextFile(file: File): FileContent = withContext(Dispatchers.IO) {
        val text = try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }

        FileContent(
            fileName = file.name,
            text = text,
            type = ContentType.TEXT,
            pageCount = 1
        )
    }

    /**
     * Read a Word document (.docx).
     */
    suspend fun readWordDocument(file: File): FileContent = withContext(Dispatchers.IO) {
        val text = StringBuilder()

        try {
            val fis = FileInputStream(file)
            val document = XWPFDocument(fis)

            for (paragraph in document.paragraphs) {
                text.appendLine(paragraph.text)
            }

            // Also read tables
            for (table in document.tables) {
                for (row in table.rows) {
                    val cells = row.tableCells.joinToString(" | ") { it.text }
                    text.appendLine(cells)
                }
                text.appendLine()
            }

            document.close()
            fis.close()
        } catch (e: Exception) {
            text.appendLine("Error reading Word document: ${e.message}")
        }

        FileContent(
            fileName = file.name,
            text = text.toString(),
            type = ContentType.DOCUMENT,
            pageCount = 1
        )
    }

    /**
     * Read an image using ML Kit OCR.
     */
    suspend fun readImageWithOcr(file: File): FileContent = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: return@withContext FileContent(
                fileName = file.name,
                text = "Could not decode image",
                type = ContentType.IMAGE,
                pageCount = 1
            )

        val text = recognizeText(bitmap)
        bitmap.recycle()

        FileContent(
            fileName = file.name,
            text = text.ifEmpty { "[No text detected in image]" },
            type = ContentType.IMAGE,
            pageCount = 1
        )
    }

    /**
     * Read an Excel file.
     */
    suspend fun readExcelFile(file: File): FileContent = withContext(Dispatchers.IO) {
        val text = StringBuilder()

        try {
            val fis = FileInputStream(file)
            val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook(fis)

            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                text.appendLine("--- Sheet: ${sheet.sheetName} ---")

                for (row in sheet) {
                    val cells = (0 until row.lastCellNum).map { idx ->
                        row.getCell(idx)?.toString() ?: ""
                    }
                    text.appendLine(cells.joinToString(" | "))
                }
                text.appendLine()
            }

            workbook.close()
            fis.close()
        } catch (e: Exception) {
            text.appendLine("Error reading Excel: ${e.message}")
        }

        FileContent(
            fileName = file.name,
            text = text.toString(),
            type = ContentType.SPREADSHEET,
            pageCount = 1
        )
    }

    /**
     * Use ML Kit to recognize text in a bitmap.
     */
    private suspend fun recognizeText(bitmap: Bitmap): String {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    continuation.resume("OCR Error: ${e.message}")
                }
        }
    }
}

data class FileContent(
    val fileName: String,
    val text: String,
    val type: ContentType,
    val pageCount: Int
)

enum class ContentType {
    PDF,
    TEXT,
    DOCUMENT,
    IMAGE,
    SPREADSHEET,
    BINARY
}
