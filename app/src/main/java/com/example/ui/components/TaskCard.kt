package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionNeeded
import com.example.model.Criticality
import com.example.model.RecurrenceType
import com.example.model.TaskItem
import com.example.model.TaskStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskCard(
    task: TaskItem,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onCompleteToggle: () -> Unit,
    onSnooze: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val isFinished = task.status.isFinished
    val isOverdue = task.isOverdue

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissVal ->
            when (dismissVal) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onCompleteToggle()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSnooze()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isSelectionMode,
        enableDismissFromEndToStart = !isSelectionMode,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF10B981) // Green complete
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFF59E0B) // Amber snooze
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                label = "swipe_color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, contentDescription = "Complete task", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Complete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Snooze 1h", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.Alarm, contentDescription = "Snooze task", tint = Color.White)
                    }
                }
            }
        },
        modifier = modifier
    ) {
        val cardBorder = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else if (isOverdue) {
            BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
        } else if (task.criticality == Criticality.CRITICAL && !isFinished) {
            BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else if (isOverdue) {
                    if (isDark) Color(0xFF2A1515) else Color(0xFFFFF1F2)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
            border = cardBorder,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) onLongClick() else onClick()
                    },
                    onLongClick = onLongClick
                )
                .testTag("task_card_${task.id}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header row: Checkbox, Title, Pinned star
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onLongClick() },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    } else {
                        IconButton(
                            onClick = onCompleteToggle,
                            modifier = Modifier
                                .size(36.dp)
                                .padding(end = 4.dp)
                                .testTag("complete_checkbox_${task.id}")
                        ) {
                            Icon(
                                imageVector = if (isFinished) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = if (isFinished) "Mark uncompleted" else "Mark completed",
                                tint = if (isFinished) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (isFinished) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (isFinished) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (task.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (task.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Pinned Task",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier
                                .size(20.dp)
                                .padding(start = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Badges and Chips Row
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Criticality badge
                    CriticalityBadge(criticality = task.criticality, compact = true)

                    // Action Needed chip
                    ActionNeededChip(action = task.actionNeeded)

                    // Category chip
                    CategoryChip(category = task.category)

                    // Origin chip if not self
                    if (task.taskOriginatedFrom.isNotBlank() && task.taskOriginatedFrom != "Self") {
                        OriginChip(origin = task.taskOriginatedFrom)
                    }

                    // Recurrence chip
                    if (task.recurrence != RecurrenceType.NONE) {
                        RecurrenceChip(recurrence = task.recurrence)
                    }
                }

                // Subtasks progress bar if subtasks exist
                if (task.subtasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { task.subtasksProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${task.completedSubtaskCount}/${task.totalSubtaskCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Footer row: Assignee avatar & Relative deadline label
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Assignee indicator if present
                    if (!task.assignedToName.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Assignee",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.assignedToName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Deadline Relative Pill
                    DeadlineRelativePill(deadlineMillis = task.completionDateTime, isFinished = isFinished)
                }
            }
        }
    }
}

@Composable
fun ActionNeededChip(action: ActionNeeded) {
    val isDark = isSystemInDarkTheme()
    val (bg, fg) = when (action) {
        ActionNeeded.IMMEDIATE_ATTENTION -> (if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)) to Color(0xFFDC2626)
        ActionNeeded.FOLLOW_UP_NEEDED -> (if (isDark) Color(0xFF38230D) else Color(0xFFFEF3C7)) to Color(0xFFD97706)
        ActionNeeded.ASSIGN_TO_OTHERS -> (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)) to Color(0xFF475569)
        ActionNeeded.TO_ACT_MYSELF -> (if (isDark) Color(0xFF142C2A) else Color(0xFFCCFBF1)) to Color(0xFF0D9488)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = action.getShortLabel(),
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CategoryChip(category: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 11.sp
        )
    }
}

@Composable
fun OriginChip(origin: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Text(
            text = "From: $origin",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp
        )
    }
}

@Composable
fun RecurrenceChip(recurrence: RecurrenceType) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Autorenew,
            contentDescription = "Recurring",
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(10.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = recurrence.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DeadlineRelativePill(deadlineMillis: Long, isFinished: Boolean) {
    val now = System.currentTimeMillis()
    val isOverdue = deadlineMillis < now && !isFinished
    val diffMillis = deadlineMillis - now

    val text = if (isFinished) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        "Completed (${sdf.format(Date(deadlineMillis))})"
    } else if (isOverdue) {
        val days = (now - deadlineMillis) / 86400_000L
        val hours = ((now - deadlineMillis) % 86400_000L) / 3600_000L
        if (days > 0) "Overdue by ${days}d" else "Overdue by ${hours.coerceAtLeast(1)}h"
    } else {
        val days = diffMillis / 86400_000L
        val hours = (diffMillis % 86400_000L) / 3600_000L
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        when {
            days == 0L && hours <= 12 -> "Today, ${sdf.format(Date(deadlineMillis))}"
            days == 1L -> "Tomorrow, ${sdf.format(Date(deadlineMillis))}"
            days in 2..6 -> "In ${days} days"
            else -> {
                val fullSdf = SimpleDateFormat("MMM d", Locale.getDefault())
                fullSdf.format(Date(deadlineMillis))
            }
        }
    }

    val (bg, fg) = when {
        isFinished -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        isOverdue -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
        diffMillis < 86400_000L -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
