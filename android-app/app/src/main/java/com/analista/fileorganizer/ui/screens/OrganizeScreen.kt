package com.analista.fileorganizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.analista.fileorganizer.ui.MainViewModel
import com.analista.fileorganizer.ui.OrganizeMode

@Composable
fun OrganizeScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val files by viewModel.files.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Organize Your Files",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            "Choose how you want to organize your download files. Files will be moved into structured folders.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(count = files.size, label = "Total Files")
                StatItem(count = files.count { it.isOrganized }, label = "Organized")
                StatItem(count = files.count { !it.isOrganized }, label = "Pending")
            }
        }

        // Organize options
        OrganizeOptionCard(
            title = "By Type & Date",
            description = "Organize into: Documents/2024-01/, Images/2024-02/, etc.",
            icon = Icons.Default.AccountTree,
            isLoading = uiState.isProcessing,
            onClick = { viewModel.organizeFiles(OrganizeMode.TYPE_AND_DATE) }
        )

        OrganizeOptionCard(
            title = "By File Type",
            description = "Sort into: Documents/, Images/, Audio/, Video/, etc.",
            icon = Icons.Default.Category,
            isLoading = uiState.isProcessing,
            onClick = { viewModel.organizeFiles(OrganizeMode.BY_TYPE) }
        )

        OrganizeOptionCard(
            title = "By Date",
            description = "Sort into monthly folders: 2024-01/, 2024-02/, etc.",
            icon = Icons.Default.CalendarMonth,
            isLoading = uiState.isProcessing,
            onClick = { viewModel.organizeFiles(OrganizeMode.BY_DATE) }
        )

        // Scan button
        OutlinedButton(
            onClick = { viewModel.scanDownloads() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isProcessing
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rescan Downloads")
        }

        // Status message
        uiState.statusMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun OrganizeOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
