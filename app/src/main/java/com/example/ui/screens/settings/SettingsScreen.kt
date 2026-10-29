package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TaskPulseViewModel
import com.example.ui.TaskUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: TaskUiState,
    viewModel: TaskPulseViewModel,
    onOpenOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showTrashDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showApiSetupGuide by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var restoreJsonText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Calendar & Cloud Account Connections
            item {
                SettingsSectionHeader("Calendar & Cloud Accounts")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color(0xFF4285F4))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Google Calendar & Sheets", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (uiState.googleCalendarConnected) "Connected" else "Sync ready via Device / OAuth",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.googleCalendarConnected,
                                onCheckedChange = { viewModel.setGoogleCalendarConnected(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color(0xFF0078D4))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Microsoft Outlook Calendar", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (uiState.outlookCalendarConnected) "Connected" else "Sync ready via Device / MSAL",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.outlookCalendarConnected,
                                onCheckedChange = { viewModel.setOutlookCalendarConnected(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = { showApiSetupGuide = true }) {
                            Text("View Google & Microsoft OAuth Setup Guide")
                        }
                    }
                }
            }

            // 2. Dashboard Preferences
            item {
                SettingsSectionHeader("Dashboard View Preferences")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Next N Days Range: ${uiState.nextNDays} days",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = uiState.nextNDays.toFloat(),
                            onValueChange = { viewModel.setNextNDays(it.toInt()) },
                            valueRange = 7f..90f,
                            steps = 8
                        )
                        Text(
                            text = "Configures the 'Due in the Next N Days' metric card on Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Export & Backup
            item {
                SettingsSectionHeader("Export, Backup & Restore")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingsRow(
                            icon = Icons.Filled.TableChart,
                            title = "Export to Google Sheets / CSV",
                            subtitle = "Share tasks spreadsheet or save locally",
                            onClick = {
                                viewModel.exportCsv(context)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsRow(
                            icon = Icons.Filled.Backup,
                            title = "Backup Database (JSON)",
                            subtitle = "Export full encrypted database snapshot",
                            onClick = { showBackupDialog = true }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsRow(
                            icon = Icons.Filled.CloudDownload,
                            title = "Restore from JSON",
                            subtitle = "Import tasks from a previously saved backup",
                            onClick = { showRestoreDialog = true }
                        )
                    }
                }
            }

            // 4. Trash & Archive Management
            item {
                SettingsSectionHeader("Data Maintenance")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingsRow(
                            icon = Icons.Filled.Delete,
                            title = "Trash Bin (${uiState.trashedTasks.size} tasks)",
                            subtitle = "Auto-purged after 30 days. Tap to review or empty.",
                            onClick = { showTrashDialog = true }
                        )
                    }
                }
            }

            // 5. Security & App Info
            item {
                SettingsSectionHeader("Security & Information")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Biometric / PIN App Lock", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "Require unlock on app resume",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.onboardingCompleted, // or appLock toggle
                                onCheckedChange = { viewModel.setAppLockEnabled(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsRow(
                            icon = Icons.Filled.Tune,
                            title = "Replay App Tour & Onboarding",
                            subtitle = "Review the 3-step feature walkthrough",
                            onClick = onOpenOnboarding
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsRow(
                            icon = Icons.Filled.Info,
                            title = "About TaskPulse",
                            subtitle = "Version 1.0 • Professional Task & Follow-up Manager",
                            onClick = { showAboutDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Trash Dialog
    if (showTrashDialog) {
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            title = { Text("Trash Bin (${uiState.trashedTasks.size} tasks)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.trashedTasks.isEmpty()) {
                        Text("Trash is currently empty.")
                    } else {
                        Text("Tasks in trash are permanently purged after 30 days.")
                        uiState.trashedTasks.take(5).forEach { task ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(task.title, maxLines = 1, modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.restoreTask(task) }) {
                                    Text("Restore")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (uiState.trashedTasks.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.emptyTrash()
                            showTrashDialog = false
                        }
                    ) {
                        Text("Empty Trash")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrashDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Backup Dialog (Copy JSON)
    if (showBackupDialog) {
        val json = viewModel.exportJsonBackup()
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Database JSON Backup") },
            text = {
                Column {
                    Text("A complete JSON snapshot of all your active tasks is ready.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("TaskPulse_Backup", json)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Backup copied to clipboard", Toast.LENGTH_SHORT).show()
                            showBackupDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy JSON to Clipboard")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Restore Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Tasks from JSON") },
            text = {
                Column {
                    Text("Paste your exported JSON array below to import tasks:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        placeholder = { Text("[{\"title\": \"...\"}]") },
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonText.isNotBlank()) {
                            viewModel.importJsonBackup(restoreJsonText)
                            showRestoreDialog = false
                            restoreJsonText = ""
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // API Setup Guide Modal
    if (showApiSetupGuide) {
        AlertDialog(
            onDismissRequest = { showApiSetupGuide = false },
            title = { Text("API Setup & Credentials Guide") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "1. Google Calendar & Sheets Setup:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• In Google Cloud Console, enable 'Google Calendar API', 'Google Sheets API', and 'Google Drive API'.\n" +
                                "• Create OAuth 2.0 Client ID for Android with package name 'com.aistudio.taskpulse.wqkvtz' and SHA-1 certificate fingerprint.\n" +
                                "• Configure OAuth Consent screen with scopes: https://www.googleapis.com/auth/calendar.events and https://www.googleapis.com/auth/spreadsheets.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "2. Microsoft Outlook MSAL Setup:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• In Azure Portal (Entra ID), register an Application.\n" +
                                "• Add Android platform with package name and signature hash.\n" +
                                "• Grant delegated permissions for 'Calendars.ReadWrite' in Microsoft Graph.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note: TaskPulse automatically supports immediate zero-setup calendar synchronization through Android's system CalendarContract directly with logged-in accounts on device.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showApiSetupGuide = false }) {
                    Text("Got it")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("TaskPulse v1.0") },
            text = {
                Text("TaskPulse is an enterprise-grade reminder and task tracker designed to make sure nothing slips through across personal and official responsibilities.\n\nBuilt with Kotlin, Jetpack Compose, Material 3, Room, WorkManager, and Coroutines.")
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
