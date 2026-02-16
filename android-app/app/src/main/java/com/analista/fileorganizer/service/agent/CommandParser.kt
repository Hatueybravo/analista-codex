package com.analista.fileorganizer.service.agent

import com.analista.fileorganizer.data.model.AgentAction
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses natural language commands into structured agent actions.
 * Supports English and Spanish commands.
 */
@Singleton
class CommandParser @Inject constructor() {

    private val gson = Gson()

    data class ParsedCommand(
        val action: AgentAction,
        val parameters: Map<String, String>
    ) {
        fun parametersJson(): String = Gson().toJson(parameters)
    }

    fun parse(input: String): ParsedCommand {
        val lower = input.lowercase().trim()

        return when {
            // Organize commands
            matchesAny(lower, listOf("organize by type and date", "organizar por tipo y fecha", "organize all", "organizar todo")) ->
                ParsedCommand(AgentAction.ORGANIZE_FILES, extractPathParam(input))

            matchesAny(lower, listOf("organize by type", "organizar por tipo", "sort by type", "clasificar por tipo")) ->
                ParsedCommand(AgentAction.ORGANIZE_BY_TYPE, extractPathParam(input))

            matchesAny(lower, listOf("organize by date", "organizar por fecha", "sort by date", "clasificar por fecha")) ->
                ParsedCommand(AgentAction.ORGANIZE_BY_DATE, extractPathParam(input))

            // Scan
            matchesAny(lower, listOf("scan", "escanear", "scan downloads", "escanear descargas")) ->
                ParsedCommand(AgentAction.SCAN_DOWNLOADS, emptyMap())

            // Read file
            matchesAny(lower, listOf("read ", "leer ", "open ", "abrir ", "extract text", "extraer texto")) ->
                ParsedCommand(AgentAction.READ_FILE, mapOf("path" to extractFilePath(input)))

            matchesAny(lower, listOf("read all", "leer todo", "read folder", "leer carpeta")) ->
                ParsedCommand(AgentAction.READ_ALL_IN_FOLDER, extractPathParam(input))

            // Generate documents
            matchesAny(lower, listOf("generate pdf", "generar pdf", "create pdf", "crear pdf", "make pdf", "hacer pdf")) ->
                ParsedCommand(AgentAction.GENERATE_PDF, extractDocParams(input))

            matchesAny(lower, listOf("generate word", "generar word", "create word", "crear word", "create doc", "make word")) ->
                ParsedCommand(AgentAction.GENERATE_WORD, extractDocParams(input))

            // File operations
            matchesAny(lower, listOf("list files", "listar archivos", "show files", "mostrar archivos", "ls ")) ->
                ParsedCommand(AgentAction.LIST_FILES, extractPathParam(input))

            matchesAny(lower, listOf("search ", "buscar ", "find ", "encontrar ")) ->
                ParsedCommand(AgentAction.SEARCH_FILES, mapOf("query" to extractSearchQuery(input)))

            matchesAny(lower, listOf("move ", "mover ")) ->
                ParsedCommand(AgentAction.MOVE_FILE, extractMoveParams(input))

            matchesAny(lower, listOf("rename ", "renombrar ")) ->
                ParsedCommand(AgentAction.RENAME_FILE, extractRenameParams(input))

            matchesAny(lower, listOf("delete ", "eliminar ", "borrar ", "remove ")) ->
                ParsedCommand(AgentAction.DELETE_FILE, mapOf("path" to extractFilePath(input)))

            // Telegram
            matchesAny(lower, listOf("send message", "enviar mensaje", "telegram ", "tell ")) ->
                ParsedCommand(AgentAction.SEND_TELEGRAM, mapOf("message" to extractMessage(input)))

            matchesAny(lower, listOf("send file", "enviar archivo")) ->
                ParsedCommand(AgentAction.SEND_FILE_TELEGRAM, mapOf("path" to extractFilePath(input)))

            // Status
            matchesAny(lower, listOf("status", "estado", "report", "reporte", "summary", "resumen")) ->
                ParsedCommand(AgentAction.STATUS_REPORT, emptyMap())

            // Help
            matchesAny(lower, listOf("help", "ayuda", "commands", "comandos", "what can you do", "que puedes hacer")) ->
                ParsedCommand(AgentAction.HELP, emptyMap())

            else -> ParsedCommand(AgentAction.UNKNOWN, mapOf("input" to input))
        }
    }

    private fun matchesAny(input: String, patterns: List<String>): Boolean {
        return patterns.any { input.contains(it) || input.startsWith(it) }
    }

    private fun extractFilePath(input: String): String {
        // Try to find a file path in the input
        val pathRegex = Regex("""[/\w\-. ]+\.\w+""")
        val match = pathRegex.find(input)
        return match?.value?.trim() ?: input.substringAfterLast(" ").trim()
    }

    private fun extractPathParam(input: String): Map<String, String> {
        val path = extractFilePath(input)
        return if (path.isNotEmpty() && path.contains("/")) {
            mapOf("path" to path)
        } else {
            emptyMap()
        }
    }

    private fun extractSearchQuery(input: String): String {
        val keywords = listOf("search ", "buscar ", "find ", "encontrar ", "for ", "para ")
        var query = input
        for (kw in keywords) {
            query = query.replace(kw, "", ignoreCase = true)
        }
        return query.trim()
    }

    private fun extractDocParams(input: String): Map<String, String> {
        val params = mutableMapOf<String, String>()

        // Extract title if specified with "title:" or "titled" or "called"
        val titleRegex = Regex("""(?:title[d:]?\s*|called\s+|named\s+|titulado\s+)["']?([^"'\n]+)["']?""", RegexOption.IGNORE_CASE)
        val titleMatch = titleRegex.find(input)
        if (titleMatch != null) {
            params["title"] = titleMatch.groupValues[1].trim()
        }

        // Extract content after "with content" or "content:" or "containing"
        val contentRegex = Regex("""(?:with content\s*|content:\s*|containing\s+|con contenido\s+)(.+)""", RegexOption.IGNORE_CASE)
        val contentMatch = contentRegex.find(input)
        if (contentMatch != null) {
            params["content"] = contentMatch.groupValues[1].trim()
        }

        // Extract source file if specified
        val fromRegex = Regex("""(?:from\s+|de\s+)([/\w\-. ]+\.\w+)""", RegexOption.IGNORE_CASE)
        val fromMatch = fromRegex.find(input)
        if (fromMatch != null) {
            params["source"] = fromMatch.groupValues[1].trim()
        }

        return params
    }

    private fun extractMoveParams(input: String): Map<String, String> {
        val parts = input.split(Regex("""\s+to\s+|\s+a\s+""", RegexOption.IGNORE_CASE))
        return if (parts.size >= 2) {
            mapOf(
                "source" to extractFilePath(parts[0]),
                "destination" to parts[1].trim()
            )
        } else {
            mapOf("input" to input)
        }
    }

    private fun extractRenameParams(input: String): Map<String, String> {
        val parts = input.split(Regex("""\s+to\s+|\s+a\s+""", RegexOption.IGNORE_CASE))
        return if (parts.size >= 2) {
            mapOf(
                "source" to extractFilePath(parts[0]),
                "newName" to parts[1].trim()
            )
        } else {
            mapOf("input" to input)
        }
    }

    private fun extractMessage(input: String): String {
        val keywords = listOf("send message ", "enviar mensaje ", "telegram ", "tell ")
        var msg = input
        for (kw in keywords) {
            msg = msg.replace(kw, "", ignoreCase = true)
        }
        return msg.trim()
    }

    fun getHelpText(): String {
        return """
            |📋 Available Commands (English / Español):
            |
            |📁 FILE ORGANIZATION:
            |  • organize by type - Sort files into category folders
            |  • organize by date - Sort files by month
            |  • organize by type and date - Combined sorting
            |  • scan downloads - Scan and catalog files
            |
            |📖 FILE READING:
            |  • read <file> - Extract text from any file
            |  • read all in <folder> - Read all files in folder
            |
            |📄 DOCUMENT GENERATION:
            |  • generate pdf title "My Doc" with content ...
            |  • generate word title "My Doc" with content ...
            |  • generate pdf from <file> - Convert file to PDF
            |
            |🔍 FILE OPERATIONS:
            |  • list files - Show files in downloads
            |  • search <query> - Find files by name
            |  • move <file> to <destination>
            |  • rename <file> to <new name>
            |  • delete <file>
            |
            |📨 TELEGRAM:
            |  • send message <text> - Send via Telegram
            |  • send file <path> - Send file via Telegram
            |
            |📊 OTHER:
            |  • status - Show organization status
            |  • help - Show this help
        """.trimMargin()
    }
}
