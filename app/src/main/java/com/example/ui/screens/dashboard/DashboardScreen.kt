package com.example.ui.screens.dashboard

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionNeeded
import com.example.model.Criticality
import com.example.model.DateBucket
import com.example.model.TaskDefaults
import com.example.model.TaskItem
import com.example.ui.TaskPulseViewModel
import com.example.ui.TaskUiState
import com.example.ui.components.CriticalityBadge
import com.example.ui.components.QuickAddBar
import com.example.ui.components.TaskCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: TaskUiState,
    viewModel: TaskPulseViewModel,
    onNavigateToTasks: (DateBucket?) -> Unit,
    onNavigateToEditor: (Long?) -> Unit,
    onOpenTemplates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val now = remember { System.currentTimeMillis() }
    val todayDateFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(now))
    }

    // Compute metrics
    val cal = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val startOfToday = cal.timeInMillis
    val endOfToday = startOfToday + 86400_000L

    val calWeek = remember {
        Calendar.getInstance().apply {
            timeInMillis = startOfToday
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            add(Calendar.DAY_OF_YEAR, 7)
        }
    }
    val endOfThisWeek = calWeek.timeInMillis
    val endOfNextWeek = endOfThisWeek + (7 * 86400_000L)
    val endOfNextNDays = startOfToday + (uiState.nextNDays * 86400_000L)

    val active = uiState.activeTasks
    val overdueTasks = active.filter { it.completionDateTime < now && !it.status.isFinished }
    val dueTodayTasks = active.filter { it.completionDateTime in startOfToday until endOfToday && !it.status.isFinished }
    val dueThisWeekTasks = active.filter { it.completionDateTime in startOfToday until endOfThisWeek && !it.status.isFinished }
    val dueNextWeekTasks = active.filter { it.completionDateTime in endOfThisWeek until endOfNextWeek && !it.status.isFinished }
    val dueNextNDaysTasks = active.filter { it.completionDateTime in startOfToday until endOfNextNDays && !it.status.isFinished }
    val pendingFollowUpTasks = active.filter { it.actionNeeded == ActionNeeded.FOLLOW_UP_NEEDED && !it.status.isFinished }

    // Criticality counts
    val criticalCount = active.count { it.criticality == Criticality.CRITICAL && !it.status.isFinished }
    val highCount = active.count { it.criticality == Criticality.HIGH && !it.status.isFinished }
    val mediumCount = active.count { it.criticality == Criticality.MEDIUM && !it.status.isFinished }
    val lowCount = active.count { it.criticality == Criticality.LOW && !it.status.isFinished }

    // Immediate Attention list (Critical + Action = IMMEDIATE_ATTENTION)
    val immediateAttentionTasks = active.filter {
        (!it.status.isFinished) && (it.criticality == Criticality.CRITICAL || it.actionNeeded == ActionNeeded.IMMEDIATE_ATTENTION)
    }.sortedBy { it.completionDateTime }

    // Today's focus (due today or overdue, sorted by criticality then time)
    val todaysFocusTasks = (overdueTasks + dueTodayTasks).distinctBy { it.id }.sortedWith(
        compareByDescending<TaskItem> { it.criticality.level }
            .thenBy { it.completionDateTime }
    )

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEditor(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add Task") },
                text = { Text("New Task", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                modifier = Modifier.testTag("add_task_fab")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 90.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Greeting & Date Header with Solid Templates Pill
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = todayDateFormatted,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "TaskPulse Control",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Surface(
                        onClick = onOpenTemplates,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("templates_header_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlaylistAdd,
                                contentDescription = "Templates",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Templates",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                val summaryText = when {
                    overdueTasks.isNotEmpty() && dueTodayTasks.isNotEmpty() ->
                        "${dueTodayTasks.size} due today • ${overdueTasks.size} overdue attention needed"
                    overdueTasks.isNotEmpty() ->
                        "${overdueTasks.size} overdue tasks require immediate action"
                    dueTodayTasks.isNotEmpty() ->
                        "${dueTodayTasks.size} tasks due today"
                    else ->
                        "All caught up! Zero urgent tasks remaining"
                }
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (overdueTasks.isNotEmpty()) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 2. Natural Language Quick Add Bar
            item {
                QuickAddBar(
                    onQuickAdd = { input ->
                        viewModel.quickAddTask(input)
                    }
                )
            }

            // 3. Metric Summary Cards (Horizontal scroll or responsive FlowRow)
            item {
                Column {
                    Text(
                        text = "Deadlines & Buckets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            MetricSummaryCard(
                                title = "Overdue",
                                count = overdueTasks.size,
                                icon = Icons.Filled.ErrorOutline,
                                accentColor = Color(0xFFEF4444),
                                isUrgent = overdueTasks.isNotEmpty(),
                                onClick = { onNavigateToTasks(DateBucket.OVERDUE) }
                            )
                        }
                        item {
                            MetricSummaryCard(
                                title = "Due Today",
                                count = dueTodayTasks.size,
                                icon = Icons.Filled.Today,
                                accentColor = Color(0xFF3B82F6),
                                onClick = { onNavigateToTasks(DateBucket.TODAY) }
                            )
                        }
                        item {
                            MetricSummaryCard(
                                title = "This Week",
                                count = dueThisWeekTasks.size,
                                icon = Icons.Filled.DateRange,
                                accentColor = Color(0xFF10B981),
                                onClick = { onNavigateToTasks(DateBucket.THIS_WEEK) }
                            )
                        }
                        item {
                            MetricSummaryCard(
                                title = "Next Week",
                                count = dueNextWeekTasks.size,
                                icon = Icons.Filled.CalendarMonth,
                                accentColor = Color(0xFF8B5CF6),
                                onClick = { onNavigateToTasks(DateBucket.NEXT_WEEK) }
                            )
                        }
                        item {
                            MetricSummaryCard(
                                title = "Next ${uiState.nextNDays} Days",
                                count = dueNextNDaysTasks.size,
                                icon = Icons.Filled.HourglassTop,
                                accentColor = Color(0xFF0EA5E9),
                                onClick = { onNavigateToTasks(DateBucket.NEXT_N_DAYS) }
                            )
                        }
                        item {
                            MetricSummaryCard(
                                title = "Follow-ups",
                                count = pendingFollowUpTasks.size,
                                icon = Icons.Filled.NotificationsActive,
                                accentColor = Color(0xFFF59E0B),
                                onClick = { onNavigateToTasks(DateBucket.PENDING_FOLLOW_UP) }
                            )
                        }
                    }
                }
            }

            // 4. Criticality Breakdown Chips
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Criticality Triage",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap chip to filter",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CriticalityTriageCard(
                                criticality = Criticality.CRITICAL,
                                count = criticalCount,
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.setFilterCriticality(Criticality.CRITICAL)
                                onNavigateToTasks(null)
                            }
                            CriticalityTriageCard(
                                criticality = Criticality.HIGH,
                                count = highCount,
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.setFilterCriticality(Criticality.HIGH)
                                onNavigateToTasks(null)
                            }
                            CriticalityTriageCard(
                                criticality = Criticality.MEDIUM,
                                count = mediumCount,
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.setFilterCriticality(Criticality.MEDIUM)
                                onNavigateToTasks(null)
                            }
                            CriticalityTriageCard(
                                criticality = Criticality.LOW,
                                count = lowCount,
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.setFilterCriticality(Criticality.LOW)
                                onNavigateToTasks(null)
                            }
                        }
                    }
                }
            }

            // 5. Needs Immediate Attention Section
            if (immediateAttentionTasks.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationImportant,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Needs Immediate Attention",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                            Text(
                                text = "${immediateAttentionTasks.size} tasks",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            immediateAttentionTasks.take(3).forEach { task ->
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

            // 6. Today's Focus List
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Today's Focus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "View all",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onNavigateToTasks(null) }
                            .padding(4.dp)
                    )
                }
            }

            if (todaysFocusTasks.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Today,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Clear focus for today!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use Quick Add above or pick a template to schedule new tasks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(todaysFocusTasks.take(6), key = { it.id }) { task ->
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

@Composable
fun MetricSummaryCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    isUrgent: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) {
                if (isDark) Color(0xFF381515) else Color(0xFFFEE2E2)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isUrgent) BorderStroke(1.5.dp, Color(0xFFEF4444)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .width(135.dp)
            .clickable(onClick = onClick)
            .testTag("metric_card_${title.lowercase().replace(" ", "_")}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUrgent) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CriticalityTriageCard(
    criticality: Criticality,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val color = criticality.getColor(isDark)
    val container = criticality.getContainerColor(isDark)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = container,
        border = BorderStroke(1.dp, color.copy(alpha = if (isDark) 0.4f else 0.25f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("criticality_triage_${criticality.name.lowercase()}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = criticality.getIcon(),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = criticality.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1
                )
            }
        }
    }
}
