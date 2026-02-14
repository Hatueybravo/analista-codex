package com.analista.fileorganizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.analista.fileorganizer.data.model.FileCategory
import com.analista.fileorganizer.data.model.FileItem
import com.analista.fileorganizer.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(viewModel: MainViewModel) {
    val files by viewModel.files.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var selectedCategory by remember { mutableStateOf<FileCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredFiles = files.filter { file ->
        (selectedCategory == null || file.category == selectedCategory) &&
                (searchQuery.isEmpty() || file.name.contains(searchQuery, ignoreCase = true))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search files...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        // Category filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                onClick = { selectedCategory = null },
                label = { Text("All") },
                selected = selectedCategory == null,
                modifier = Modifier.height(32.dp)
            )
            FileCategory.entries.take(4).forEach { category ->
                val count = files.count { it.category == category }
                if (count > 0) {
                    FilterChip(
                        onClick = {
                            selectedCategory = if (selectedCategory == category) null else category
                        },
                        label = { Text("${category.displayName} ($count)") },
                        selected = selectedCategory == category,
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scan button
        if (files.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No files scanned yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.scanDownloads() },
                    enabled = !uiState.isProcessing
                ) {
                    if (uiState.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Scan Downloads")
                }
            }
        } else {
            // File stats bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${filteredFiles.size} files",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { viewModel.scanDownloads() }) {
                    Text("Rescan")
                }
            }

            // File list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredFiles) { file ->
                    FileItemCard(file = file)
                }
            }
        }
    }
}

@Composable
private fun FileItemCard(file: FileItem) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val icon = when (file.category) {
        FileCategory.DOCUMENTS -> Icons.Default.Description
        FileCategory.IMAGES -> Icons.Default.Image
        FileCategory.AUDIO -> Icons.Default.AudioFile
        FileCategory.VIDEO -> Icons.Default.VideoFile
        FileCategory.ARCHIVES -> Icons.Default.Archive
        FileCategory.OTHER -> Icons.Default.InsertDriveFile
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = file.category.displayName,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatSize(file.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(Date(file.lastModified)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AssistChip(
                onClick = { },
                label = { Text(file.category.displayName, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
