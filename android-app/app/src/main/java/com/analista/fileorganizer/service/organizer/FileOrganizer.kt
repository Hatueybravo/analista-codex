package com.analista.fileorganizer.service.organizer

import android.content.Context
import android.os.Environment
import com.analista.fileorganizer.data.local.FileDao
import com.analista.fileorganizer.data.model.FileCategory
import com.analista.fileorganizer.data.model.FileItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileOrganizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileDao: FileDao
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    /**
     * Scan the Downloads directory and catalog all files.
     */
    suspend fun scanDownloads(): List<FileItem> {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadDir.exists()) return emptyList()

        val files = mutableListOf<FileItem>()
        scanDirectory(downloadDir, files)

        fileDao.insertFiles(files)
        return files
    }

    /**
     * Scan any directory recursively.
     */
    suspend fun scanDirectory(directory: File, accumulator: MutableList<FileItem> = mutableListOf()): List<FileItem> {
        if (!directory.exists() || !directory.isDirectory) return accumulator

        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanDirectory(file, accumulator)
            } else {
                val ext = file.extension.lowercase()
                val item = FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    extension = ext,
                    category = FileCategory.fromExtension(ext),
                    sizeBytes = file.length(),
                    lastModified = file.lastModified()
                )
                accumulator.add(item)
            }
        }

        return accumulator
    }

    /**
     * Organize files by moving them into category-based subdirectories.
     */
    suspend fun organizeByType(baseDir: File? = null): OrganizeResult {
        val downloadDir = baseDir ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val organized = OrganizeResult()

        val files = mutableListOf<FileItem>()
        scanDirectory(downloadDir, files)

        for (fileItem in files) {
            val sourceFile = File(fileItem.path)
            if (!sourceFile.exists()) continue

            val categoryDir = File(downloadDir, "Organized/${fileItem.category.displayName}")
            categoryDir.mkdirs()

            val destFile = getUniqueFile(categoryDir, sourceFile.name)
            try {
                sourceFile.copyTo(destFile, overwrite = false)
                sourceFile.delete()

                val updatedItem = fileItem.copy(
                    organizedPath = destFile.absolutePath,
                    isOrganized = true
                )
                fileDao.insertFile(updatedItem)
                organized.movedFiles++
            } catch (e: Exception) {
                organized.errors.add("${fileItem.name}: ${e.message}")
            }
        }

        organized.totalFiles = files.size
        return organized
    }

    /**
     * Organize files by moving them into date-based subdirectories (YYYY-MM).
     */
    suspend fun organizeByDate(baseDir: File? = null): OrganizeResult {
        val downloadDir = baseDir ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val organized = OrganizeResult()

        val files = mutableListOf<FileItem>()
        scanDirectory(downloadDir, files)

        for (fileItem in files) {
            val sourceFile = File(fileItem.path)
            if (!sourceFile.exists()) continue

            val dateStr = monthFormat.format(Date(fileItem.lastModified))
            val dateDir = File(downloadDir, "Organized/$dateStr")
            dateDir.mkdirs()

            val destFile = getUniqueFile(dateDir, sourceFile.name)
            try {
                sourceFile.copyTo(destFile, overwrite = false)
                sourceFile.delete()

                val updatedItem = fileItem.copy(
                    organizedPath = destFile.absolutePath,
                    isOrganized = true
                )
                fileDao.insertFile(updatedItem)
                organized.movedFiles++
            } catch (e: Exception) {
                organized.errors.add("${fileItem.name}: ${e.message}")
            }
        }

        organized.totalFiles = files.size
        return organized
    }

    /**
     * Organize files by both type and date (Type/YYYY-MM/).
     */
    suspend fun organizeByTypeAndDate(baseDir: File? = null): OrganizeResult {
        val downloadDir = baseDir ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val organized = OrganizeResult()

        val files = mutableListOf<FileItem>()
        scanDirectory(downloadDir, files)

        for (fileItem in files) {
            val sourceFile = File(fileItem.path)
            if (!sourceFile.exists()) continue

            val dateStr = monthFormat.format(Date(fileItem.lastModified))
            val destDir = File(downloadDir, "Organized/${fileItem.category.displayName}/$dateStr")
            destDir.mkdirs()

            val destFile = getUniqueFile(destDir, sourceFile.name)
            try {
                sourceFile.copyTo(destFile, overwrite = false)
                sourceFile.delete()

                val updatedItem = fileItem.copy(
                    organizedPath = destFile.absolutePath,
                    isOrganized = true
                )
                fileDao.insertFile(updatedItem)
                organized.movedFiles++
            } catch (e: Exception) {
                organized.errors.add("${fileItem.name}: ${e.message}")
            }
        }

        organized.totalFiles = files.size
        return organized
    }

    /**
     * List files in a directory with category info.
     */
    suspend fun listFiles(directory: File): List<FileItem> {
        val files = mutableListOf<FileItem>()
        scanDirectory(directory, files)
        return files
    }

    /**
     * Search for files matching a query.
     */
    suspend fun searchFiles(query: String, baseDir: File? = null): List<FileItem> {
        val dir = baseDir ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val allFiles = mutableListOf<FileItem>()
        scanDirectory(dir, allFiles)

        val lowerQuery = query.lowercase()
        return allFiles.filter {
            it.name.lowercase().contains(lowerQuery) ||
                    it.extension.lowercase().contains(lowerQuery) ||
                    it.category.displayName.lowercase().contains(lowerQuery)
        }
    }

    private fun getUniqueFile(directory: File, fileName: String): File {
        var file = File(directory, fileName)
        var counter = 1
        val nameWithoutExt = fileName.substringBeforeLast(".")
        val ext = fileName.substringAfterLast(".", "")

        while (file.exists()) {
            val newName = if (ext.isNotEmpty()) "${nameWithoutExt}_($counter).$ext" else "${nameWithoutExt}_($counter)"
            file = File(directory, newName)
            counter++
        }

        return file
    }
}

data class OrganizeResult(
    var totalFiles: Int = 0,
    var movedFiles: Int = 0,
    val errors: MutableList<String> = mutableListOf()
) {
    val successRate: Float
        get() = if (totalFiles > 0) movedFiles.toFloat() / totalFiles else 0f

    fun summary(): String {
        val sb = StringBuilder()
        sb.appendLine("Organization Complete:")
        sb.appendLine("  Total files: $totalFiles")
        sb.appendLine("  Moved: $movedFiles")
        if (errors.isNotEmpty()) {
            sb.appendLine("  Errors: ${errors.size}")
            errors.take(5).forEach { sb.appendLine("    - $it") }
        }
        return sb.toString()
    }
}
