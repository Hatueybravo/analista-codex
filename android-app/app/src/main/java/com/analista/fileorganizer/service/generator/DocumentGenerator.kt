package com.analista.fileorganizer.service.generator

import android.content.Context
import android.os.Environment
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val FONT_NAME_TAHOMA = "Tahoma"
        private const val FONT_SIZE_BODY = 11f
        private const val FONT_SIZE_TITLE = 16f
        private const val FONT_SIZE_HEADING = 13f
        private const val MARGIN = 56f // ~2cm margins
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    /**
     * Generate a nicely formatted PDF document from text content.
     */
    suspend fun generatePdf(
        title: String,
        content: String,
        outputFileName: String? = null
    ): File = withContext(Dispatchers.IO) {
        val fileName = outputFileName ?: "Document_${dateFormat.format(Date())}.pdf"
        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "FileOrganizerAI"
        )
        outputDir.mkdirs()
        val outputFile = File(outputDir, fileName)

        val writer = PdfWriter(FileOutputStream(outputFile))
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.LETTER)
        document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN)

        // Use Helvetica as a close substitute for Tahoma (Tahoma is not bundled in iText)
        // For production, embed the actual Tahoma font file
        val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)
        val fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)

        // Title
        document.add(
            Paragraph(title)
                .setFont(fontBold)
                .setFontSize(FONT_SIZE_TITLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(8f)
        )

        // Date
        document.add(
            Paragraph(displayDateFormat.format(Date()))
                .setFont(font)
                .setFontSize(9f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
        )

        // Separator line
        document.add(
            Paragraph("─".repeat(80))
                .setFont(font)
                .setFontSize(6f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(16f)
        )

        // Body content - split into paragraphs and justify
        val paragraphs = content.split("\n\n")
        for (para in paragraphs) {
            val trimmed = para.trim()
            if (trimmed.isEmpty()) continue

            // Detect headings (lines starting with # or all caps short lines)
            if (trimmed.startsWith("#")) {
                val headingText = trimmed.removePrefix("#").removePrefix("#").removePrefix("#").trim()
                document.add(
                    Paragraph(headingText)
                        .setFont(fontBold)
                        .setFontSize(FONT_SIZE_HEADING)
                        .setTextAlignment(TextAlignment.LEFT)
                        .setMarginTop(12f)
                        .setMarginBottom(6f)
                )
            } else if (trimmed.length < 80 && trimmed == trimmed.uppercase() && !trimmed.contains(".")) {
                // All-caps short line = heading
                document.add(
                    Paragraph(trimmed)
                        .setFont(fontBold)
                        .setFontSize(FONT_SIZE_HEADING)
                        .setTextAlignment(TextAlignment.LEFT)
                        .setMarginTop(12f)
                        .setMarginBottom(6f)
                )
            } else {
                // Regular paragraph - justified, Tahoma-like font
                document.add(
                    Paragraph(trimmed)
                        .setFont(font)
                        .setFontSize(FONT_SIZE_BODY)
                        .setTextAlignment(TextAlignment.JUSTIFIED)
                        .setFirstLineIndent(20f)
                        .setMarginBottom(8f)
                )
            }
        }

        // Footer
        document.add(
            Paragraph("\n─".repeat(80))
                .setFont(font)
                .setFontSize(6f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        document.add(
            Paragraph("Generated by File Organizer AI")
                .setFont(font)
                .setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )

        document.close()
        outputFile
    }

    /**
     * Generate a nicely formatted Word (.docx) document.
     */
    suspend fun generateWord(
        title: String,
        content: String,
        outputFileName: String? = null
    ): File = withContext(Dispatchers.IO) {
        val fileName = outputFileName ?: "Document_${dateFormat.format(Date())}.docx"
        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "FileOrganizerAI"
        )
        outputDir.mkdirs()
        val outputFile = File(outputDir, fileName)

        val document = XWPFDocument()

        // Title
        val titleParagraph = document.createParagraph()
        titleParagraph.alignment = ParagraphAlignment.CENTER
        val titleRun = titleParagraph.createRun()
        titleRun.fontFamily = FONT_NAME_TAHOMA
        titleRun.fontSize = FONT_SIZE_TITLE.toInt()
        titleRun.isBold = true
        titleRun.setText(title)

        // Date
        val dateParagraph = document.createParagraph()
        dateParagraph.alignment = ParagraphAlignment.CENTER
        val dateRun = dateParagraph.createRun()
        dateRun.fontFamily = FONT_NAME_TAHOMA
        dateRun.fontSize = 9
        dateRun.setText(displayDateFormat.format(Date()))

        // Separator
        val sepParagraph = document.createParagraph()
        sepParagraph.alignment = ParagraphAlignment.CENTER
        val sepRun = sepParagraph.createRun()
        sepRun.fontFamily = FONT_NAME_TAHOMA
        sepRun.fontSize = 6
        sepRun.setText("─".repeat(80))

        // Body paragraphs
        val paragraphs = content.split("\n\n")
        for (para in paragraphs) {
            val trimmed = para.trim()
            if (trimmed.isEmpty()) continue

            val p = document.createParagraph()

            if (trimmed.startsWith("#")) {
                // Heading
                val headingText = trimmed.removePrefix("#").removePrefix("#").removePrefix("#").trim()
                p.alignment = ParagraphAlignment.LEFT
                val run = p.createRun()
                run.fontFamily = FONT_NAME_TAHOMA
                run.fontSize = FONT_SIZE_HEADING.toInt()
                run.isBold = true
                run.setText(headingText)
            } else if (trimmed.length < 80 && trimmed == trimmed.uppercase() && !trimmed.contains(".")) {
                // All-caps heading
                p.alignment = ParagraphAlignment.LEFT
                val run = p.createRun()
                run.fontFamily = FONT_NAME_TAHOMA
                run.fontSize = FONT_SIZE_HEADING.toInt()
                run.isBold = true
                run.setText(trimmed)
            } else {
                // Regular justified paragraph with Tahoma
                p.alignment = ParagraphAlignment.BOTH // BOTH = justified
                p.indentationFirstLine = 400 // First line indent (~0.5 inch)

                // Handle line breaks within a paragraph
                val lines = trimmed.split("\n")
                for ((i, line) in lines.withIndex()) {
                    val run = p.createRun()
                    run.fontFamily = FONT_NAME_TAHOMA
                    run.fontSize = FONT_SIZE_BODY.toInt()
                    run.setText(line)
                    if (i < lines.size - 1) {
                        run.addBreak()
                    }
                }
            }
        }

        // Footer separator
        val footSep = document.createParagraph()
        footSep.alignment = ParagraphAlignment.CENTER
        val footSepRun = footSep.createRun()
        footSepRun.fontFamily = FONT_NAME_TAHOMA
        footSepRun.fontSize = 6
        footSepRun.setText("─".repeat(80))

        // Footer text
        val footer = document.createParagraph()
        footer.alignment = ParagraphAlignment.CENTER
        val footerRun = footer.createRun()
        footerRun.fontFamily = FONT_NAME_TAHOMA
        footerRun.fontSize = 8
        footerRun.isItalic = true
        footerRun.setText("Generated by File Organizer AI")

        // Write to file
        val fos = FileOutputStream(outputFile)
        document.write(fos)
        fos.close()
        document.close()

        outputFile
    }

    /**
     * Generate a report-style PDF from a list of file items.
     */
    suspend fun generateFileReport(
        files: List<com.analista.fileorganizer.data.model.FileItem>,
        title: String = "File Organization Report"
    ): File = withContext(Dispatchers.IO) {
        val content = StringBuilder()
        content.appendLine("SUMMARY")
        content.appendLine()
        content.appendLine("Total files: ${files.size}")

        val byCategory = files.groupBy { it.category }
        for ((category, categoryFiles) in byCategory) {
            content.appendLine("${category.displayName}: ${categoryFiles.size} files")
        }
        content.appendLine()

        content.appendLine("DETAILED FILE LISTING")
        content.appendLine()

        for ((category, categoryFiles) in byCategory) {
            content.appendLine("## ${category.displayName}")
            content.appendLine()
            for (file in categoryFiles.sortedByDescending { it.lastModified }) {
                val size = formatFileSize(file.sizeBytes)
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date(file.lastModified))
                content.appendLine("• ${file.name} ($size) - $date")
            }
            content.appendLine()
        }

        generatePdf(title, content.toString(), "FileReport_${dateFormat.format(Date())}.pdf")
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
