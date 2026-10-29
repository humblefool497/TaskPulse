package com.example.ui.screens.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Criticality
import com.example.model.TaskItem
import com.example.ui.TaskPulseViewModel
import com.example.ui.TaskUiState
import com.example.ui.components.TaskCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CalendarViewMode {
    MONTH, WEEK, AGENDA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: TaskUiState,
    viewModel: TaskPulseViewModel,
    onNavigateToEditor: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    var selectedCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }
    var currentMonthCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    val monthHeaderFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayHeaderFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    // Map tasks to day timestamps (start of day millis)
    val tasksByDay = remember(uiState.activeTasks) {
        val map = mutableMapOf<Long, MutableList<TaskItem>>()
        val cal = Calendar.getInstance()
        uiState.activeTasks.forEach { task ->
            cal.timeInMillis = task.completionDateTime
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            map.getOrPut(dayStart) { mutableListOf() }.add(task)
        }
        map
    }

    // Selected day tasks
    val selectedDayStart = remember(selectedCalendar) {
        selectedCalendar.timeInMillis
    }
    val tasksForSelectedDay = tasksByDay[selectedDayStart] ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    IconButton(
                        onClick = {
                            // Quick bulk sync action to device calendar
                            tasksForSelectedDay.firstOrNull()?.let {
                                viewModel.syncToDeviceCalendar(it)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = "Sync with Calendar")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // View Mode selector (Month, Week, Agenda)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                FilterChip(
                    selected = viewMode == CalendarViewMode.MONTH,
                    onClick = { viewMode = CalendarViewMode.MONTH },
                    label = { Text("Month") }
                )
                FilterChip(
                    selected = viewMode == CalendarViewMode.WEEK,
                    onClick = { viewMode = CalendarViewMode.WEEK },
                    label = { Text("Week") }
                )
                FilterChip(
                    selected = viewMode == CalendarViewMode.AGENDA,
                    onClick = { viewMode = CalendarViewMode.AGENDA },
                    label = { Text("Agenda") }
                )
            }

            // Month navigation header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                IconButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = currentMonthCalendar.timeInMillis
                        add(Calendar.MONTH, -1)
                    }
                    currentMonthCalendar = cal
                }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Month")
                }

                Text(
                    text = monthHeaderFormat.format(Date(currentMonthCalendar.timeInMillis)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = currentMonthCalendar.timeInMillis
                        add(Calendar.MONTH, 1)
                    }
                    currentMonthCalendar = cal
                }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next Month")
                }
            }

            AnimatedContent(targetState = viewMode, label = "calendar_view") { mode ->
                when (mode) {
                    CalendarViewMode.MONTH -> {
                        MonthViewGrid(
                            monthCalendar = currentMonthCalendar,
                            selectedDateMillis = selectedDayStart,
                            tasksByDay = tasksByDay,
                            onDaySelected = { dateMillis ->
                                val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                                selectedCalendar = cal
                            }
                        )
                    }
                    CalendarViewMode.WEEK -> {
                        WeekViewGrid(
                            selectedDateMillis = selectedDayStart,
                            tasksByDay = tasksByDay,
                            onDaySelected = { dateMillis ->
                                val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                                selectedCalendar = cal
                            }
                        )
                    }
                    CalendarViewMode.AGENDA -> {
                        // Agenda mode handled in agenda list below
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Day Agenda Header & Task List
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dayHeaderFormat.format(Date(selectedDayStart)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${tasksForSelectedDay.size} tasks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (tasksForSelectedDay.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "No tasks scheduled for this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tasksForSelectedDay, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onClick = { onNavigateToEditor(task.id) },
                            onCompleteToggle = { viewModel.completeTask(task) },
                            onSnooze = { viewModel.snoozeTask(task) },
                            onDelete = { viewModel.moveToTrash(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthViewGrid(
    monthCalendar: Calendar,
    selectedDateMillis: Long,
    tasksByDay: Map<Long, List<TaskItem>>,
    onDaySelected: (Long) -> Unit
) {
    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")

    // Calculate grid days
    val cal = Calendar.getInstance().apply {
        timeInMillis = monthCalendar.timeInMillis
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Day of week labels
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days matrix
            var dayCounter = 1
            for (row in 0..5) {
                if (dayCounter > maxDays) break
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    for (col in 0..6) {
                        if (row == 0 && col < firstDayOfWeek || dayCounter > maxDays) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val thisDay = dayCounter
                            val dayCal = Calendar.getInstance().apply {
                                timeInMillis = monthCalendar.timeInMillis
                                set(Calendar.DAY_OF_MONTH, thisDay)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val dayMillis = dayCal.timeInMillis
                            val isSelected = (dayMillis == selectedDateMillis)
                            val dayTasks = tasksByDay[dayMillis] ?: emptyList()

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onDaySelected(dayMillis) }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = thisDay.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )

                                    if (dayTasks.isNotEmpty()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            dayTasks.take(3).forEach { t ->
                                                val dotColor = if (isSelected) Color.White else t.criticality.getColor()
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekViewGrid(
    selectedDateMillis: Long,
    tasksByDay: Map<Long, List<TaskItem>>,
    onDaySelected: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }

    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            for (i in 0..6) {
                val dayCal = Calendar.getInstance().apply {
                    timeInMillis = cal.timeInMillis
                    add(Calendar.DAY_OF_YEAR, i)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val dayMillis = dayCal.timeInMillis
                val isSelected = (dayMillis == selectedDateMillis)
                val tasks = tasksByDay[dayMillis] ?: emptyList()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onDaySelected(dayMillis) }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = dayNames[i],
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayCal.get(Calendar.DAY_OF_MONTH).toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    if (tasks.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color(0xFFEF4444))
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
