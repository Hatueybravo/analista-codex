package com.analista.fileorganizer.service.agent

import android.os.Environment
import com.analista.fileorganizer.data.local.CommandDao
import com.analista.fileorganizer.data.local.FileDao
import com.analista.fileorganizer.data.model.*
import com.analista.fileorganizer.service.generator.DocumentGenerator
import com.analista.fileorganizer.service.organizer.FileOrganizer
import com.analista.fileorganizer.service.reader.FileReader
import com.analista.fileorganizer.service.telegram.TelegramBot
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentExecutor @Inject constructor(
    private val commandParser: CommandParser,
    private val fileOrganizer: FileOrganizer,
    private val fileReader: FileReader,
    private val documentGenerator: DocumentGenerator,
    private val telegramBot: TelegramBot,
    private val commandDao: CommandDao,
    private val fileDao: FileDao
) {

    private val gson = Gson()

    /**
     * Process a natural language command and execute the corresponding action.
     */
    suspend fun execute(
        input: String,
        source: CommandSource = CommandSource.LOCAL
    ): String = withContext(Dispatchers.IO) {
        val parsed = commandParser.parse(input)

        val command = AgentCommand(
            input = input,
            parsedAction = parsed.action,
            parameters = parsed.parametersJson(),
            status = CommandStatus.RUNNING,
            source = source
        )
        val commandId = commandDao.insertCommand(command)

        val result = try {
            executeAction(parsed)
        } catch (e: Exception) {
            commandDao.updateCommand(
                command.copy(
                    id = commandId,
                    status = CommandStatus.FAILED,
                    result = "Error: ${e.message}",
                    completedAt = System.currentTimeMillis()
                )
            )
            "Error executing command: ${e.message}"
        }

        commandDao.updateCommand(
            command.copy(
                id = commandId,
                status = CommandStatus.COMPLETED,
                result = result,
                completedAt = System.currentTimeMillis()
            )
        )

        result
    }

    private suspend fun executeAction(parsed: CommandParser.ParsedCommand): String {
        val params = parsed.parameters

        return when (parsed.action) {
            AgentAction.ORGANIZE_FILES -> {
                val dir = params["path"]?.let { File(it) }
                val result = fileOrganizer.organizeByTypeAndDate(dir)
                result.summary()
            }

            AgentAction.ORGANIZE_BY_TYPE -> {
                val dir = params["path"]?.let { File(it) }
                val result = fileOrganizer.organizeByType(dir)
                result.summary()
            }

            AgentAction.ORGANIZE_BY_DATE -> {
                val dir = params["path"]?.let { File(it) }
                val result = fileOrganizer.organizeByDate(dir)
                result.summary()
            }

            AgentAction.SCAN_DOWNLOADS -> {
                val files = fileOrganizer.scanDownloads()
                val byCategory = files.groupBy { it.category }
                val sb = StringBuilder("Scan Complete: ${files.size} files found\n")
                for ((cat, catFiles) in byCategory) {
                    sb.appendLine("  ${cat.displayName}: ${catFiles.size}")
                }
                sb.toString()
            }

            AgentAction.READ_FILE -> {
                val path = params["path"] ?: return "Please specify a file path."
                val file = File(path)
                if (!file.exists()) return "File not found: $path"
                val content = fileReader.readFile(file)
                "Content from ${content.fileName} (${content.type}, ${content.pageCount} pages):\n\n${content.text.take(2000)}"
            }

            AgentAction.READ_ALL_IN_FOLDER -> {
                val path = params["path"]
                    ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
                val dir = File(path)
                if (!dir.exists() || !dir.isDirectory) return "Directory not found: $path"

                val results = StringBuilder()
                dir.listFiles()?.filter { it.isFile }?.forEach { file ->
                    val content = fileReader.readFile(file)
                    results.appendLine("=== ${content.fileName} ===")
                    results.appendLine(content.text.take(500))
                    results.appendLine()
                }
                results.toString().ifEmpty { "No files found in directory." }
            }

            AgentAction.GENERATE_PDF -> {
                val title = params["title"] ?: "Document"
                val content = params["content"] ?: ""
                val sourceFile = params["source"]

                val finalContent = if (sourceFile != null) {
                    val file = File(sourceFile)
                    if (file.exists()) {
                        fileReader.readFile(file).text
                    } else {
                        return "Source file not found: $sourceFile"
                    }
                } else if (content.isNotEmpty()) {
                    content
                } else {
                    return "Please provide content or a source file."
                }

                val outputFile = documentGenerator.generatePdf(title, finalContent)
                "PDF generated: ${outputFile.absolutePath}"
            }

            AgentAction.GENERATE_WORD -> {
                val title = params["title"] ?: "Document"
                val content = params["content"] ?: ""
                val sourceFile = params["source"]

                val finalContent = if (sourceFile != null) {
                    val file = File(sourceFile)
                    if (file.exists()) {
                        fileReader.readFile(file).text
                    } else {
                        return "Source file not found: $sourceFile"
                    }
                } else if (content.isNotEmpty()) {
                    content
                } else {
                    return "Please provide content or a source file."
                }

                val outputFile = documentGenerator.generateWord(title, finalContent)
                "Word document generated: ${outputFile.absolutePath}"
            }

            AgentAction.LIST_FILES -> {
                val path = params["path"]
                    ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
                val files = fileOrganizer.listFiles(File(path))
                if (files.isEmpty()) return "No files found in $path"

                val sb = StringBuilder("Files in $path (${files.size} total):\n")
                files.sortedByDescending { it.lastModified }.take(50).forEach { f ->
                    val size = formatSize(f.sizeBytes)
                    sb.appendLine("  [${f.category.displayName}] ${f.name} ($size)")
                }
                sb.toString()
            }

            AgentAction.SEARCH_FILES -> {
                val query = params["query"] ?: return "Please specify a search query."
                val results = fileOrganizer.searchFiles(query)
                if (results.isEmpty()) return "No files found matching '$query'."

                val sb = StringBuilder("Found ${results.size} files matching '$query':\n")
                results.forEach { f ->
                    sb.appendLine("  ${f.name} - ${f.path}")
                }
                sb.toString()
            }

            AgentAction.MOVE_FILE -> {
                val source = params["source"] ?: return "Please specify source and destination."
                val dest = params["destination"] ?: return "Please specify destination."
                val sourceFile = File(source)
                if (!sourceFile.exists()) return "Source file not found: $source"

                val destFile = File(dest)
                destFile.parentFile?.mkdirs()
                sourceFile.renameTo(destFile)
                "Moved: $source → $dest"
            }

            AgentAction.RENAME_FILE -> {
                val source = params["source"] ?: return "Please specify the file to rename."
                val newName = params["newName"] ?: return "Please specify the new name."
                val sourceFile = File(source)
                if (!sourceFile.exists()) return "File not found: $source"

                val destFile = File(sourceFile.parentFile, newName)
                sourceFile.renameTo(destFile)
                "Renamed: ${sourceFile.name} → $newName"
            }

            AgentAction.DELETE_FILE -> {
                val path = params["path"] ?: return "Please specify the file to delete."
                val file = File(path)
                if (!file.exists()) return "File not found: $path"
                file.delete()
                "Deleted: $path"
            }

            AgentAction.SEND_TELEGRAM -> {
                val message = params["message"] ?: return "Please specify a message to send."
                telegramBot.sendMessage(message)
            }

            AgentAction.SEND_FILE_TELEGRAM -> {
                val path = params["path"] ?: return "Please specify the file to send."
                val file = File(path)
                if (!file.exists()) return "File not found: $path"
                telegramBot.sendDocument(file)
            }

            AgentAction.STATUS_REPORT -> {
                val totalFiles = fileDao.totalCount()
                val docs = fileDao.countByCategory(FileCategory.DOCUMENTS)
                val imgs = fileDao.countByCategory(FileCategory.IMAGES)
                val audio = fileDao.countByCategory(FileCategory.AUDIO)
                val video = fileDao.countByCategory(FileCategory.VIDEO)

                """
                |Status Report:
                |  Total files tracked: $totalFiles
                |  Documents: $docs
                |  Images: $imgs
                |  Audio: $audio
                |  Video: $video
                |  Telegram: ${if (telegramBot.isConnected()) "Connected" else "Disconnected"}
                """.trimMargin()
            }

            AgentAction.HELP -> commandParser.getHelpText()

            AgentAction.UNKNOWN -> "I don't understand that command. Type 'help' to see available commands."
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
