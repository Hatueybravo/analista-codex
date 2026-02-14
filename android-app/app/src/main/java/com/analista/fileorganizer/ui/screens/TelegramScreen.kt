package com.analista.fileorganizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.analista.fileorganizer.ui.MainViewModel

@Composable
fun TelegramScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val telegramConfig by viewModel.telegramConfig.collectAsState()

    var botToken by remember { mutableStateOf(telegramConfig.botToken) }
    var chatId by remember { mutableStateOf(telegramConfig.chatId) }
    var showToken by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf("") }

    // Update fields when config loads
    LaunchedEffect(telegramConfig) {
        botToken = telegramConfig.botToken
        chatId = telegramConfig.chatId
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Telegram Integration",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            "Connect your Telegram bot to control the agent remotely. Send commands and receive results directly in Telegram.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Connection status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (telegramConfig.isEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (telegramConfig.isEnabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (telegramConfig.isEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    if (telegramConfig.isEnabled) "Connected" else "Disconnected",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Bot token input
        OutlinedTextField(
            value = botToken,
            onValueChange = { botToken = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Bot Token") },
            placeholder = { Text("123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11") },
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle visibility"
                    )
                }
            },
            singleLine = true
        )

        // Chat ID input
        OutlinedTextField(
            value = chatId,
            onValueChange = { chatId = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Chat ID") },
            placeholder = { Text("Your Telegram chat ID") },
            supportingText = { Text("Send /start to @userinfobot to get your chat ID") },
            singleLine = true
        )

        // Connect / Disconnect buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.configureTelegram(botToken, chatId) },
                modifier = Modifier.weight(1f),
                enabled = botToken.isNotBlank() && chatId.isNotBlank() && !uiState.isProcessing
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Connect")
            }

            OutlinedButton(
                onClick = { viewModel.disconnectTelegram() },
                modifier = Modifier.weight(1f),
                enabled = telegramConfig.isEnabled
            ) {
                Text("Disconnect")
            }
        }

        // Divider
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Test message
        Text("Send Test Message", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = testMessage,
                onValueChange = { testMessage = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a test message...") },
                singleLine = true,
                enabled = telegramConfig.isEnabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    if (testMessage.isNotBlank()) {
                        viewModel.executeCommand("send message $testMessage")
                        testMessage = ""
                    }
                },
                enabled = telegramConfig.isEnabled && testMessage.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }

        // Setup instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Setup Instructions:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1. Open Telegram and search for @BotFather\n" +
                            "2. Send /newbot and follow the instructions\n" +
                            "3. Copy the bot token and paste it above\n" +
                            "4. Send /start to @userinfobot to get your Chat ID\n" +
                            "5. Enter your Chat ID and click Connect\n\n" +
                            "Once connected, send commands directly to your bot!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
