package com.example.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.model.Criticality
import com.example.model.DateBucket
import com.example.model.TaskItem
import com.example.model.TaskStatus
import com.example.ui.GroupOption
import com.example.ui.SortOption
import com.example.ui.TaskPulseViewModel
import com.example.ui.TaskUiState
import com.example.ui.components.BulkActionsBar
import com.example.ui.components.FilterSheet
import com.example.ui.components.TaskCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TaskListScreen(
    uiState: TaskUiState,
    viewModel: TaskPulseViewModel,
    onNavigateToEditor: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFilterSheet by remember { mutableStateOf(false) }

    var showSortMenu by remember { mutableStateOf(false) }
    var showGroupMenu by remember { mutableStateOf(false) }

    // Dialog states for bulk action updates
    var showBulkCriticalityDialog by remember { mutableStateOf(false) }

    // Count active filters
    val activeFilterCount = listOfNotNull(
        uiState.activeFilterBucket,
        uiState.filterCriticality,
        uiState.filterActionNeeded,
        uiState.filterCategory,
        uiState.filterStatus,
        uiState.filterOrigin,
        uiState.filterTag
    ).size

    Scaffold(
        floatingActionButton = {
            if (!uiState.isMultiSelectActive) {
                FloatingActionButton(
                    onClick = { onNavigateToEditor(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("tasks_add_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New Task")
                }
            }
        },
        bottomBar = {
            BulkActionsBar(
                selectedCount = uiState.selectedTaskIds.size,
                isVisible = uiState.isMultiSelectActive,
                onClearSelection = { viewModel.clearSelection() },
                onBulkComplete = { viewModel.bulkComplete() },
                onBulkDelete = { viewModel.bulkDelete() },
                onChangeCriticality = { showBulkCriticalityDialog = true },
                onChangeDeadline = {
                    // Quick +1 day postponement
                    viewModel.bulkUpdateDeadline(System.currentTimeMillis() + 86400_000L)
                },
                onBulkExport = {
                    val selected = uiState.activeTasks.filter { uiState.selectedTaskIds.contains(it.id) }
                    viewModel.exportCsv(context, selected)
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = if (uiState.isMultiSelectActive) 70.dp else innerPadding.calculateBottomPadding()
                )
        ) {
            // Search & Filter header bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search title, notes, tags…", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tasks_search_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Filter icon button with badge
                BadgedBox(
                    badge = {
                        if (activeFilterCount > 0) {
                            Badge { Text(activeFilterCount.toString()) }
                        }
                    }
                ) {
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.testTag("tasks_filter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "Filter tasks",
                            tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Sort & Group dropdown button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("tasks_sort_button")
                    ) {
                        Icon(Icons.Filled.Sort, contentDescription = "Sort and Group")
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        Text(
                            text = "Sort By",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        fontWeight = if (uiState.sortBy == option) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.setSortBy(option)
                                    showSortMenu = false
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Group By",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        GroupOption.values().forEach { group ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = group.label,
                                        fontWeight = if (uiState.groupBy == group) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.setGroupBy(group)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Removable active filter chips row
            if (activeFilterCount > 0 || uiState.activeFilterBucket != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    uiState.activeFilterBucket?.let { bucket ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.setFilterBucket(null) },
                            label = { Text("Bucket: ${bucket.title}") },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                    uiState.filterCriticality?.let { crit ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.setFilterCriticality(null) },
                            label = { Text(crit.label) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                    uiState.filterActionNeeded?.let { act ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.setFilterAction(null) },
                            label = { Text(act.getShortLabel()) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                    uiState.filterCategory?.let { cat ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.setFilterCategory(null) },
                            label = { Text(cat) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                    uiState.filterStatus?.let { stat ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.setFilterStatus(null) },
                            label = { Text(stat.label) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }

            // Task list with grouping support
            if (uiState.filteredTasks.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No matching tasks found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try clearing active filters or searching a different keyword.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val groupedTasks = remember(uiState.filteredTasks, uiState.groupBy) {
                    when (uiState.groupBy) {
                        GroupOption.NONE -> mapOf("" to uiState.filteredTasks)
                        GroupOption.CRITICALITY -> uiState.filteredTasks.groupBy { it.criticality.label }
                        GroupOption.CATEGORY -> uiState.filteredTasks.groupBy { it.category }
                        GroupOption.ACTION_NEEDED -> uiState.filteredTasks.groupBy { it.actionNeeded.label }
                        GroupOption.DEADLINE_BUCKET -> uiState.filteredTasks.groupBy {
                            when {
                                it.isOverdue -> "Overdue"
                                it.completionDateTime - System.currentTimeMillis() < 86400_000L -> "Today"
                                it.completionDateTime - System.currentTimeMillis() < 7 * 86400_000L -> "This Week"
                                else -> "Later"
                            }
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedTasks.forEach { (groupTitle, tasksInGroup) ->
                        if (groupTitle.isNotBlank()) {
                            stickyHeader {
                                Surface(
                                    color = MaterialTheme.colorScheme.background,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "$groupTitle (${tasksInGroup.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        items(tasksInGroup, key = { it.id }) { task ->
                            val isSelected = uiState.selectedTaskIds.contains(task.id)
                            TaskCard(
                                task = task,
                                isSelected = isSelected,
                                isSelectionMode = uiState.isMultiSelectActive,
                                onClick = { onNavigateToEditor(task.id) },
                                onLongClick = { viewModel.toggleSelectTask(task.id) },
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

    // Filter Sheet Modal
    if (showFilterSheet) {
        FilterSheet(
            sheetState = filterSheetState,
            uiState = uiState,
            onDismiss = {
                coroutineScope.launch {
                    filterSheetState.hide()
                    showFilterSheet = false
                }
            },
            onSelectCriticality = { viewModel.setFilterCriticality(it) },
            onSelectAction = { viewModel.setFilterAction(it) },
            onSelectCategory = { viewModel.setFilterCategory(it) },
            onSelectStatus = { viewModel.setFilterStatus(it) },
            onSelectOrigin = { viewModel.setFilterOrigin(it) },
            onClearAll = { viewModel.clearFilters() }
        )
    }

    // Bulk criticality picker dialog
    if (showBulkCriticalityDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBulkCriticalityDialog = false },
            title = { Text("Change Criticality for ${uiState.selectedTaskIds.size} Tasks") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Criticality.values().forEach { crit ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = crit.getContainerColor()),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Icon(crit.getIcon(), contentDescription = null, tint = crit.getColor())
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = crit.label,
                                    fontWeight = FontWeight.Bold,
                                    color = crit.getColor()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showBulkCriticalityDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
