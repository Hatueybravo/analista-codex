package com.analista.fileorganizer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "files")
data class FileItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String,
    val extension: String,
    val category: FileCategory,
    val sizeBytes: Long,
    val lastModified: Long,
    val organizedPath: String? = null,
    val isOrganized: Boolean = false,
    val extractedText: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class FileCategory(val displayName: String, val extensions: List<String>) {
    DOCUMENTS(
        "Documents",
        listOf("pdf", "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx", "csv")
    ),
    IMAGES(
        "Images",
        listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff", "heic", "heif")
    ),
    AUDIO(
        "Audio",
        listOf("mp3", "wav", "aac", "flac", "ogg", "m4a", "wma")
    ),
    VIDEO(
        "Video",
        listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "3gp")
    ),
    ARCHIVES(
        "Archives",
        listOf("zip", "rar", "7z", "tar", "gz", "bz2")
    ),
    OTHER("Other", emptyList());

    companion object {
        fun fromExtension(ext: String): FileCategory {
            val lower = ext.lowercase()
            return entries.firstOrNull { it.extensions.contains(lower) } ?: OTHER
        }
    }
}
