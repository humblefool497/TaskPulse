package com.example.ui.screens.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionNeeded
import com.example.model.AttachmentItem
import com.example.model.Criticality
import com.example.model.ProgressNote
import com.example.model.RecurrenceType
import com.example.model.Subtask
import com.example.model.TaskDefaults
import com.example.model.TaskItem
import com.example.model.TaskStatus
import com.example.ui.TaskPulseViewModel
import com.example.ui.TaskUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorScreen(
    taskId: Long?,
    uiState: TaskUiState,
    viewModel: TaskPulseViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // Find task if editing
    val existingTask = remember(taskId, uiState.activeTasks) {
        if (taskId != null && taskId > 0) {
            uiState.activeTasks.find { it.id == taskId }
        } else null
    }

    // Form fields
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var description by remember { mutableStateOf(existingTask?.description ?: "") }
    var criticality by remember { mutableStateOf(existingTask?.criticality ?: Criticality.MEDIUM) }
    var actionNeeded by remember { mutableStateOf(existingTask?.actionNeeded ?: ActionNeeded.TO_ACT_MYSELF) }
    var category by remember { mutableStateOf(existingTask?.category ?: "Official") }
    var originatedFrom by remember { mutableStateOf(existingTask?.taskOriginatedFrom ?: "Self") }
    var completionDateTime by remember {
        mutableStateOf(existingTask?.completionDateTime ?: (System.currentTimeMillis() + 4 * 3600_000L))
    }
    var followUpDate by remember { mutableStateOf<Long?>(existingTask?.followUpDate) }
    var assignedToName by remember { mutableStateOf(existingTask?.assignedToName ?: "") }
    var assignedToContact by remember { mutableStateOf(existingTask?.assignedToContact ?: "") }
    var recurrence by remember { mutableStateOf(existingTask?.recurrence ?: RecurrenceType.NONE) }
    var isPinned by remember { mutableStateOf(existingTask?.isPinned ?: false) }
    var addToCalendar by remember { mutableStateOf(existingTask?.addToCalendar ?: false) }

    // Reminder offsets: e.g. at deadline (0), 15m, 60m, 1440m (1d), 4320m (3d)
    var reminderOffsets by remember {
        mutableStateOf(existingTask?.reminderOffsetsMinutes?.toSet() ?: setOf(0L))
    }

    // Subtasks
    val subtasks = remember {
        mutableStateListOf<Subtask>().apply {
            if (existingTask != null) addAll(existingTask.subtasks)
        }
    }
    var newSubtaskText by remember { mutableStateOf("") }

    // Tags
    val tags = remember {
        mutableStateListOf<String>().apply {
            if (existingTask != null) addAll(existingTask.tags)
        }
    }
    var newTagText by remember { mutableStateOf("") }

    // Progress notes
    val notes = remember {
        mutableStateListOf<ProgressNote>().apply {
            if (existingTask != null) addAll(existingTask.notes)
        }
    }
    var newNoteText by remember { mutableStateOf("") }

    // Custom category input
    var showCustomCategoryInput by remember { mutableStateOf(false) }
    var customCategoryText by remember { mutableStateOf("") }

    // Duplicate detection
    val duplicates by remember(title) {
        derivedStateOf {
            if (title.length >= 3) {
                viewModel.checkPotentialDuplicates(title, taskId ?: 0)
            } else emptyList()
        }
    }

    // Date/Time formatting helpers
    val dateSdf = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val timeSdf = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    fun showDatePicker(initialMillis: Long, onDateSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val updatedCal = Calendar.getInstance().apply {
                    timeInMillis = initialMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                onDateSelected(updatedCal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showTimePicker(initialMillis: Long, onTimeSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val updatedCal = Calendar.getInstance().apply {
                    timeInMillis = initialMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                onTimeSelected(updatedCal.timeInMillis)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingTask != null) "Edit Task" else "Create Task") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Pin task",
                            tint = if (isPinned) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (existingTask != null) {
                        IconButton(
                            onClick = {
                                val shareBody = "Task: ${existingTask.title}\nDue: ${dateSdf.format(Date(existingTask.completionDateTime))} ${timeSdf.format(Date(existingTask.completionDateTime))}\nCriticality: ${existingTask.criticality.label}\nCategory: ${existingTask.category}\nAction: ${existingTask.actionNeeded.label}\n\n${existingTask.description}"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "[TaskPulse] ${existingTask.title}")
                                    putExtra(Intent.EXTRA_TEXT, shareBody)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Task via"))
                            }
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share Task")
                        }

                        IconButton(
                            onClick = {
                                viewModel.moveToTrash(existingTask)
                                onNavigateBack()
                            }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (title.isBlank()) return@Button

                            val taskToSave = (existingTask ?: TaskItem(
                                title = title.trim(),
                                completionDateTime = completionDateTime
                            )).copy(
                                title = title.trim(),
                                description = description.trim(),
                                criticality = criticality,
                                actionNeeded = actionNeeded,
                                category = category,
                                taskOriginatedFrom = originatedFrom.ifBlank { "Self" },
                                completionDateTime = completionDateTime,
                                followUpDate = if (actionNeeded == ActionNeeded.FOLLOW_UP_NEEDED) followUpDate else null,
                                assignedToName = if (actionNeeded == ActionNeeded.ASSIGN_TO_OTHERS) assignedToName.trim() else null,
                                assignedToContact = if (actionNeeded == ActionNeeded.ASSIGN_TO_OTHERS) assignedToContact.trim() else null,
                                subtasks = subtasks.toList(),
                                tags = tags.toList(),
                                notes = notes.toList(),
                                recurrence = recurrence,
                                isPinned = isPinned,
                                addToCalendar = addToCalendar,
                                reminderOffsetsMinutes = reminderOffsets.toList()
                            )

                            if (existingTask != null) {
                                viewModel.updateTask(taskToSave)
                            } else {
                                viewModel.createTask(taskToSave)
                            }
                            onNavigateBack()
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_task_button")
                    ) {
                        Text(
                            text = if (existingTask != null) "Update Task" else "Save Task",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Duplicate task warning banner
            if (duplicates.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFD97706))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Possible duplicate task found",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "\"${duplicates.first().title}\" already exists",
                                color = Color(0xFFB45309),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // SECTION 1: Basics
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Task Title *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 120) title = it },
                        placeholder = { Text("What needs to be accomplished?") },
                        supportingText = { Text("${title.length}/120 characters") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_title_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Description / Context",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Additional specifications, background, or links...") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // SECTION 2: Criticality
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Criticality / Priority *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Criticality.values().forEach { crit ->
                            val isSelected = criticality == crit
                            val color = crit.getColor(isDark)
                            val container = crit.getContainerColor(isDark)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) container else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) BorderStroke(2.dp, color) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clickable { criticality = crit }
                                    .testTag("criticality_select_${crit.name.lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(crit.getIcon(), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = crit.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: Action Needed
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Action Needed *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ActionNeeded.values().forEach { act ->
                        val isSelected = actionNeeded == act
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { actionNeeded = act }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = act.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Conditional: Follow-up date
                    if (actionNeeded == ActionNeeded.FOLLOW_UP_NEEDED) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Follow-up Date",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                val current = followUpDate ?: (completionDateTime - 86400_000L)
                                showDatePicker(current) { selectedDate ->
                                    followUpDate = selectedDate
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (followUpDate != null) dateSdf.format(Date(followUpDate!!)) else "Set Follow-up Date"
                            )
                        }
                    }

                    // Conditional: Assigned To
                    if (actionNeeded == ActionNeeded.ASSIGN_TO_OTHERS) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Assignee Name",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = assignedToName,
                            onValueChange = { assignedToName = it },
                            placeholder = { Text("Person's name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Assignee Email or Phone",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = assignedToContact,
                            onValueChange = { assignedToContact = it },
                            placeholder = { Text("alex@enterprise.com / +123456789") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // SECTION 4: Category & Originated From
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
                        Text(
                            text = "Task Category *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "+ Custom",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showCustomCategoryInput = !showCustomCategoryInput }
                        )
                    }

                    if (showCustomCategoryInput) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = customCategoryText,
                                onValueChange = { customCategoryText = it },
                                placeholder = { Text("New category name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (customCategoryText.isNotBlank()) {
                                        viewModel.addCustomCategory(customCategoryText.trim())
                                        category = customCategoryText.trim()
                                        customCategoryText = ""
                                        showCustomCategoryInput = false
                                    }
                                }
                            ) {
                                Text("Add")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.distinctCategories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Task Originated From *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = originatedFrom,
                        onValueChange = { originatedFrom = it },
                        placeholder = { Text("e.g. Boss, Client, Meeting, Email, Self") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.distinctOrigins.take(6).forEach { orig ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable { originatedFrom = orig }
                            ) {
                                Text(
                                    text = orig,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 5: Schedule & Reminders
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Completion Deadline *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                showDatePicker(completionDateTime) { completionDateTime = it }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(dateSdf.format(Date(completionDateTime)), maxLines = 1)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = {
                                showTimePicker(completionDateTime) { completionDateTime = it }
                            },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(timeSdf.format(Date(completionDateTime)), maxLines = 1)
                        }
                    }

                    // Quick schedule presets
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.clickable {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 17)
                                    set(Calendar.MINUTE, 0)
                                }
                                completionDateTime = cal.timeInMillis
                            }
                        ) {
                            Text("Today 5pm", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.clickable {
                                val cal = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                    set(Calendar.HOUR_OF_DAY, 9)
                                    set(Calendar.MINUTE, 0)
                                }
                                completionDateTime = cal.timeInMillis
                            }
                        ) {
                            Text("Tomorrow 9am", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.clickable {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
                                    set(Calendar.HOUR_OF_DAY, 17)
                                    set(Calendar.MINUTE, 0)
                                }
                                completionDateTime = cal.timeInMillis
                            }
                        ) {
                            Text("Friday 5pm", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Reminders",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val reminderOptions = listOf(
                            0L to "At deadline",
                            15L to "15 min before",
                            60L to "1 hr before",
                            1440L to "1 day before",
                            4320L to "3 days before"
                        )
                        reminderOptions.forEach { (mins, label) ->
                            val isSelected = reminderOffsets.contains(mins)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    reminderOffsets = if (isSelected) reminderOffsets - mins else reminderOffsets + mins
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Recurrence
                    Text(
                        text = "Recurrence",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RecurrenceType.values().forEach { rec ->
                            FilterChip(
                                selected = recurrence == rec,
                                onClick = { recurrence = rec },
                                label = { Text(rec.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Calendar integration toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Add to Device / Cloud Calendar",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Syncs with Google/Outlook calendar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = addToCalendar,
                            onCheckedChange = { addToCalendar = it }
                        )
                    }
                }
            }

            // SECTION 6: Subtasks / Checklist
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Subtasks / Checklist (${subtasks.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    subtasks.forEachIndexed { index, subtask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = subtask.isCompleted,
                                onCheckedChange = { isChecked ->
                                    subtasks[index] = subtask.copy(isCompleted = isChecked)
                                }
                            )
                            Text(
                                text = subtask.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { subtasks.removeAt(index) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Delete subtask", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            placeholder = { Text("Add subtask item...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newSubtaskText.isNotBlank()) {
                                    subtasks.add(Subtask(title = newSubtaskText.trim()))
                                    newSubtaskText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add subtask")
                        }
                    }
                }
            }

            // SECTION 7: Tags & Progress Notes
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("#$tag", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove tag",
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { tags.remove(tag) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = { newTagText = it },
                            placeholder = { Text("Add tag (e.g. Q4, Executive)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val cleanTag = newTagText.trim().replace("#", "")
                                if (cleanTag.isNotBlank() && !tags.contains(cleanTag)) {
                                    tags.add(cleanTag)
                                    newTagText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add tag")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Progress Notes / Activity Log",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    notes.forEach { note ->
                        val noteDate = remember(note.timestamp) { dateSdf.format(Date(note.timestamp)) }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(noteDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Text(note.note, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newNoteText,
                            onValueChange = { newNoteText = it },
                            placeholder = { Text("Log update: e.g. Called vendor, will reply Friday") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newNoteText.isNotBlank()) {
                                    notes.add(ProgressNote(note = newNoteText.trim()))
                                    newNoteText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add progress note")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
