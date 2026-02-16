package com.analista.fileorganizer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_commands")
data class AgentCommand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val input: String,
    val parsedAction: AgentAction,
    val parameters: String, // JSON string of params
    val status: CommandStatus = CommandStatus.PENDING,
    val result: String? = null,
    val source: CommandSource = CommandSource.LOCAL,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

enum class AgentAction(val description: String) {
    ORGANIZE_FILES("Organize files by type and date"),
    ORGANIZE_BY_TYPE("Organize files by type only"),
    ORGANIZE_BY_DATE("Organize files by date only"),
    READ_FILE("Read and extract content from a file"),
    READ_ALL_IN_FOLDER("Read all files in a folder"),
    GENERATE_PDF("Generate a PDF document"),
    GENERATE_WORD("Generate a Word document"),
    SCAN_DOWNLOADS("Scan the downloads folder"),
    LIST_FILES("List files in a directory"),
    SEARCH_FILES("Search for files by name or content"),
    MOVE_FILE("Move a file to a new location"),
    RENAME_FILE("Rename a file"),
    DELETE_FILE("Delete a file"),
    SEND_TELEGRAM("Send a message via Telegram"),
    SEND_FILE_TELEGRAM("Send a file via Telegram"),
    STATUS_REPORT("Generate a status report"),
    HELP("Show available commands"),
    UNKNOWN("Unknown command")
}

enum class CommandStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

enum class CommandSource {
    LOCAL,
    TELEGRAM
}
