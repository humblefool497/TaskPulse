package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.TaskPulseApplication
import com.example.data.TaskRepository
import com.example.data.UserPreferencesRepository
import com.example.engine.CalendarSyncManager
import com.example.engine.NaturalLanguageTaskParser
import com.example.engine.SheetsExportManager
import com.example.engine.TaskDuplicateDetector
import com.example.model.ActionNeeded
import com.example.model.Criticality
import com.example.model.DateBucket
import com.example.model.Subtask
import com.example.model.TaskItem
import com.example.model.TaskStatus
import com.example.notification.TaskPulseAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

enum class SortOption(val label: String) {
    DEADLINE("Deadline (Earliest)"),
    CRITICALITY("Criticality (Highest)"),
    CREATED_AT("Recently Added"),
    TITLE("Alphabetical")
}

enum class GroupOption(val label: String) {
    NONE("No Grouping"),
    CRITICALITY("By Criticality"),
    DEADLINE_BUCKET("By Deadline"),
    CATEGORY("By Category"),
    ACTION_NEEDED("By Action Needed")
}

data class TaskUiState(
    val activeTasks: List<TaskItem> = emptyList(),
    val filteredTasks: List<TaskItem> = emptyList(),
    val trashedTasks: List<TaskItem> = emptyList(),
    val archivedTasks: List<TaskItem> = emptyList(),
    val searchQuery: String = "",
    val activeFilterBucket: DateBucket? = null,
    val filterCriticality: Criticality? = null,
    val filterActionNeeded: ActionNeeded? = null,
    val filterCategory: String? = null,
    val filterStatus: TaskStatus? = null,
    val filterOrigin: String? = null,
    val filterTag: String? = null,
    val sortBy: SortOption = SortOption.DEADLINE,
    val groupBy: GroupOption = GroupOption.NONE,
    val selectedTaskIds: Set<Long> = emptySet(),
    val isMultiSelectActive: Boolean = false,
    val distinctOrigins: List<String> = emptyList(),
    val distinctCategories: List<String> = emptyList(),
    val nextNDays: Int = 30,
    val userCustomCategories: List<String> = emptyList(),
    val userCustomOrigins: List<String> = emptyList(),
    val googleCalendarConnected: Boolean = false,
    val outlookCalendarConnected: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val userMessage: String? = null
)

class TaskPulseViewModel(
    private val repository: TaskRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val alarmScheduler: TaskPulseAlarmScheduler,
    private val nlpParser: NaturalLanguageTaskParser,
    private val duplicateDetector: TaskDuplicateDetector,
    private val sheetsExportManager: SheetsExportManager,
    private val calendarSyncManager: CalendarSyncManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterBucket = MutableStateFlow<DateBucket?>(null)
    private val _filterCriticality = MutableStateFlow<Criticality?>(null)
    private val _filterAction = MutableStateFlow<ActionNeeded?>(null)
    private val _filterCategory = MutableStateFlow<String?>(null)
    private val _filterStatus = MutableStateFlow<TaskStatus?>(null)
    private val _filterOrigin = MutableStateFlow<String?>(null)
    private val _filterTag = MutableStateFlow<String?>(null)
    private val _sortBy = MutableStateFlow(SortOption.DEADLINE)
    private val _groupBy = MutableStateFlow(GroupOption.NONE)
    private val _selectedTaskIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _userMessage = MutableStateFlow<String?>(null)

    private val _lastDeletedTask = MutableStateFlow<TaskItem?>(null)

    val uiState: StateFlow<TaskUiState> = combine(
        repository.activeTasks,
        repository.deletedTasks,
        repository.archivedTasks,
        repository.distinctOrigins,
        repository.distinctCategories,
        prefsRepository.nextNDays,
        prefsRepository.customCategories,
        prefsRepository.customOrigins,
        prefsRepository.googleCalendarConnected,
        prefsRepository.outlookCalendarConnected,
        prefsRepository.onboardingCompleted,
        _searchQuery,
        _filterBucket,
        _filterCriticality,
        _filterAction,
        _filterCategory,
        _filterStatus,
        _filterOrigin,
        _filterTag,
        _sortBy,
        _groupBy,
        _selectedTaskIds,
        _userMessage
    ) { args: Array<Any?> ->
        var idx = 0
        @Suppress("UNCHECKED_CAST") val active = args[idx++] as List<TaskItem>
        @Suppress("UNCHECKED_CAST") val deleted = args[idx++] as List<TaskItem>
        @Suppress("UNCHECKED_CAST") val archived = args[idx++] as List<TaskItem>
        @Suppress("UNCHECKED_CAST") val origins = args[idx++] as List<String>
        @Suppress("UNCHECKED_CAST") val categories = args[idx++] as List<String>
        val nDays = args[idx++] as Int
        @Suppress("UNCHECKED_CAST") val customCats = args[idx++] as List<String>
        @Suppress("UNCHECKED_CAST") val customOrigins = args[idx++] as List<String>
        val gCal = args[idx++] as Boolean
        val oCal = args[idx++] as Boolean
        val onbDone = args[idx++] as Boolean
        val query = args[idx++] as String
        val bucket = args[idx++] as DateBucket?
        val crit = args[idx++] as Criticality?
        val act = args[idx++] as ActionNeeded?
        val cat = args[idx++] as String?
        val stat = args[idx++] as TaskStatus?
        val origin = args[idx++] as String?
        val tag = args[idx++] as String?
        val sort = args[idx++] as SortOption
        val group = args[idx++] as GroupOption
        @Suppress("UNCHECKED_CAST") val selectedIds = args[idx++] as Set<Long>
        val msg = args[idx++] as String?

        // Compute filtered & sorted tasks
        val filtered = filterAndSortTasks(
            tasks = active,
            query = query,
            bucket = bucket,
            criticality = crit,
            action = act,
            category = cat,
            status = stat,
            origin = origin,
            tag = tag,
            sortBy = sort,
            nextNDays = nDays
        )

        TaskUiState(
            activeTasks = active,
            filteredTasks = filtered,
            trashedTasks = deleted,
            archivedTasks = archived,
            searchQuery = query,
            activeFilterBucket = bucket,
            filterCriticality = crit,
            filterActionNeeded = act,
            filterCategory = cat,
            filterStatus = stat,
            filterOrigin = origin,
            filterTag = tag,
            sortBy = sort,
            groupBy = group,
            selectedTaskIds = selectedIds,
            isMultiSelectActive = selectedIds.isNotEmpty(),
            distinctOrigins = (origins + customOrigins).distinct(),
            distinctCategories = (categories + customCats).distinct(),
            nextNDays = nDays,
            userCustomCategories = customCats,
            userCustomOrigins = customOrigins,
            googleCalendarConnected = gCal,
            outlookCalendarConnected = oCal,
            onboardingCompleted = onbDone,
            userMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setFilterBucket(bucket: DateBucket?) {
        _filterBucket.value = if (_filterBucket.value == bucket) null else bucket
    }

    fun setFilterCriticality(crit: Criticality?) {
        _filterCriticality.value = if (_filterCriticality.value == crit) null else crit
    }

    fun setFilterAction(action: ActionNeeded?) {
        _filterAction.value = if (_filterAction.value == action) null else action
    }

    fun setFilterCategory(category: String?) {
        _filterCategory.value = if (_filterCategory.value == category) null else category
    }

    fun setFilterStatus(status: TaskStatus?) {
        _filterStatus.value = if (_filterStatus.value == status) null else status
    }

    fun setFilterOrigin(origin: String?) {
        _filterOrigin.value = if (_filterOrigin.value == origin) null else origin
    }

    fun setFilterTag(tag: String?) {
        _filterTag.value = if (_filterTag.value == tag) null else tag
    }

    fun clearFilters() {
        _filterBucket.value = null
        _filterCriticality.value = null
        _filterAction.value = null
        _filterCategory.value = null
        _filterStatus.value = null
        _filterOrigin.value = null
        _filterTag.value = null
        _searchQuery.value = ""
    }

    fun setSortBy(sort: SortOption) {
        _sortBy.value = sort
    }

    fun setGroupBy(group: GroupOption) {
        _groupBy.value = group
    }

    fun toggleSelectTask(id: Long) {
        _selectedTaskIds.update { set ->
            if (set.contains(id)) set - id else set + id
        }
    }

    fun selectAll(tasks: List<TaskItem>) {
        _selectedTaskIds.value = tasks.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedTaskIds.value = emptySet()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun quickAddTask(rawInput: String) {
        if (rawInput.isBlank()) return
        viewModelScope.launch {
            val draft = nlpParser.parse(rawInput)
            val newTask = TaskItem(
                title = draft.title.take(120),
                criticality = draft.criticality,
                actionNeeded = draft.actionNeeded,
                category = draft.category,
                taskOriginatedFrom = draft.originatedFrom,
                completionDateTime = draft.deadlineMillis
            )
            val id = repository.insertTask(newTask)
            val inserted = repository.getTaskById(id)
            if (inserted != null) {
                alarmScheduler.scheduleTaskReminders(inserted)
            }
            _userMessage.value = "Task created: ${newTask.title}"
        }
    }

    fun createTask(task: TaskItem) {
        viewModelScope.launch {
            val id = repository.insertTask(task)
            val inserted = repository.getTaskById(id)
            if (inserted != null) {
                alarmScheduler.scheduleTaskReminders(inserted)
                if (inserted.addToCalendar) {
                    calendarSyncManager.createDeviceCalendarEvent(inserted)
                }
            }
            _userMessage.value = "Task saved"
        }
    }

    fun updateTask(task: TaskItem) {
        viewModelScope.launch {
            repository.updateTask(task)
            alarmScheduler.scheduleTaskReminders(task)
            _userMessage.value = "Task updated"
        }
    }

    fun completeTask(task: TaskItem) {
        viewModelScope.launch {
            val nextOccurrence = repository.completeTask(task)
            alarmScheduler.cancelTaskReminders(task.id)
            if (nextOccurrence != null) {
                alarmScheduler.scheduleTaskReminders(nextOccurrence)
                _userMessage.value = "Completed. Next occurrence generated for ${nextOccurrence.recurrence.label}"
            } else {
                _userMessage.value = "Task marked completed"
            }
        }
    }

    fun snoozeTask(task: TaskItem, durationMillis: Long = 3600_000L) {
        viewModelScope.launch {
            repository.snoozeTask(task, durationMillis)
            val updated = repository.getTaskById(task.id)
            if (updated != null) {
                alarmScheduler.scheduleTaskReminders(updated)
            }
            _userMessage.value = "Task snoozed"
        }
    }

    fun moveToTrash(task: TaskItem) {
        viewModelScope.launch {
            _lastDeletedTask.value = task
            repository.moveToTrash(task)
            alarmScheduler.cancelTaskReminders(task.id)
            _userMessage.value = "Task moved to trash"
        }
    }

    fun undoDelete() {
        val last = _lastDeletedTask.value ?: return
        viewModelScope.launch {
            repository.restoreFromTrash(last)
            alarmScheduler.scheduleTaskReminders(last)
            _lastDeletedTask.value = null
            _userMessage.value = "Task restored"
        }
    }

    fun restoreTask(task: TaskItem) {
        viewModelScope.launch {
            repository.restoreFromTrash(task)
            alarmScheduler.scheduleTaskReminders(task)
            _userMessage.value = "Task restored"
        }
    }

    fun permanentlyDelete(id: Long) {
        viewModelScope.launch {
            repository.permanentlyDelete(id)
            alarmScheduler.cancelTaskReminders(id)
            _userMessage.value = "Task permanently removed"
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val count = repository.emptyTrash()
            _userMessage.value = "Emptied trash ($count tasks removed)"
        }
    }

    fun archiveTask(task: TaskItem) {
        viewModelScope.launch {
            repository.archiveTask(task)
            alarmScheduler.cancelTaskReminders(task.id)
            _userMessage.value = "Task archived"
        }
    }

    fun unarchiveTask(task: TaskItem) {
        viewModelScope.launch {
            repository.unarchiveTask(task)
            alarmScheduler.scheduleTaskReminders(task)
            _userMessage.value = "Task moved to active"
        }
    }

    fun toggleSubtask(taskId: Long, subtaskId: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            val updatedSubtasks = task.subtasks.map {
                if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it
            }
            repository.updateTask(task.copy(subtasks = updatedSubtasks))
        }
    }

    fun bulkComplete() {
        val selected = _selectedTaskIds.value
        val tasks = uiState.value.activeTasks.filter { selected.contains(it.id) }
        viewModelScope.launch {
            repository.bulkComplete(tasks)
            clearSelection()
            _userMessage.value = "Marked ${tasks.size} tasks complete"
        }
    }

    fun bulkDelete() {
        val selected = _selectedTaskIds.value
        val tasks = uiState.value.activeTasks.filter { selected.contains(it.id) }
        viewModelScope.launch {
            repository.bulkDelete(tasks)
            clearSelection()
            _userMessage.value = "Moved ${tasks.size} tasks to trash"
        }
    }

    fun bulkUpdateCriticality(criticality: Criticality) {
        val selected = _selectedTaskIds.value
        val tasks = uiState.value.activeTasks.filter { selected.contains(it.id) }
        viewModelScope.launch {
            repository.bulkUpdateCriticality(tasks, criticality)
            clearSelection()
            _userMessage.value = "Updated ${tasks.size} tasks to ${criticality.label}"
        }
    }

    fun bulkUpdateDeadline(newDeadline: Long) {
        val selected = _selectedTaskIds.value
        val tasks = uiState.value.activeTasks.filter { selected.contains(it.id) }
        viewModelScope.launch {
            repository.bulkUpdateDeadline(tasks, newDeadline)
            tasks.forEach { alarmScheduler.scheduleTaskReminders(it.copy(completionDateTime = newDeadline)) }
            clearSelection()
            _userMessage.value = "Updated deadline for ${tasks.size} tasks"
        }
    }

    fun checkPotentialDuplicates(title: String, currentTaskId: Long = 0): List<TaskItem> {
        return duplicateDetector.findPotentialDuplicates(title, uiState.value.activeTasks, currentTaskId)
    }

    fun exportCsv(context: Context, customList: List<TaskItem>? = null): File? {
        val tasksToExport = customList ?: uiState.value.filteredTasks
        val file = sheetsExportManager.exportAndShareCsv(context, tasksToExport)
        if (file != null) {
            _userMessage.value = "Exported ${tasksToExport.size} tasks to CSV"
        }
        return file
    }

    fun exportJsonBackup(): String {
        return runBlockingOrScopeExport()
    }

    private fun runBlockingOrScopeExport(): String {
        var result = ""
        viewModelScope.launch {
            result = repository.exportToJson(uiState.value.activeTasks)
        }
        return result
    }

    fun importJsonBackup(jsonString: String) {
        viewModelScope.launch {
            try {
                val count = repository.importFromJson(jsonString)
                _userMessage.value = "Successfully imported $count tasks"
            } catch (e: Exception) {
                _userMessage.value = "Failed to import tasks: ${e.localizedMessage}"
            }
        }
    }

    fun syncToDeviceCalendar(task: TaskItem): Boolean {
        return calendarSyncManager.createDeviceCalendarEvent(task)
    }

    fun setNextNDays(days: Int) = prefsRepository.setNextNDays(days)
    fun addCustomCategory(cat: String) = prefsRepository.addCustomCategory(cat)
    fun addCustomOrigin(origin: String) = prefsRepository.addCustomOrigin(origin)
    fun setGoogleCalendarConnected(connected: Boolean) = prefsRepository.setGoogleCalendarConnected(connected)
    fun setOutlookCalendarConnected(connected: Boolean) = prefsRepository.setOutlookCalendarConnected(connected)
    fun setOnboardingCompleted(completed: Boolean) = prefsRepository.setOnboardingCompleted(completed)
    fun setAppLockEnabled(enabled: Boolean) = prefsRepository.setAppLockEnabled(enabled)

    private fun filterAndSortTasks(
        tasks: List<TaskItem>,
        query: String,
        bucket: DateBucket?,
        criticality: Criticality?,
        action: ActionNeeded?,
        category: String?,
        status: TaskStatus?,
        origin: String?,
        tag: String?,
        sortBy: SortOption,
        nextNDays: Int
    ): List<TaskItem> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis
        val endOfToday = startOfToday + 86400_000L

        // End of this week (Sunday)
        val calWeek = Calendar.getInstance().apply {
            timeInMillis = startOfToday
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            add(Calendar.DAY_OF_YEAR, 7)
        }
        val endOfThisWeek = calWeek.timeInMillis
        val endOfNextWeek = endOfThisWeek + (7 * 86400_000L)
        val endOfNextNDays = startOfToday + (nextNDays * 86400_000L)

        val filtered = tasks.filter { task ->
            // Query match
            if (query.isNotBlank()) {
                val q = query.lowercase().trim()
                val matchTitle = task.title.lowercase().contains(q)
                val matchDesc = task.description.lowercase().contains(q)
                val matchTag = task.tags.any { it.lowercase().contains(q) }
                val matchNote = task.notes.any { it.note.lowercase().contains(q) }
                val matchAssignee = (task.assignedToName?.lowercase()?.contains(q) == true)
                if (!matchTitle && !matchDesc && !matchTag && !matchNote && !matchAssignee) {
                    return@filter false
                }
            }

            // Bucket filter
            if (bucket != null) {
                val matchesBucket = when (bucket) {
                    DateBucket.OVERDUE -> task.completionDateTime < now && !task.status.isFinished
                    DateBucket.TODAY -> task.completionDateTime in startOfToday until endOfToday
                    DateBucket.THIS_WEEK -> task.completionDateTime in startOfToday until endOfThisWeek
                    DateBucket.NEXT_WEEK -> task.completionDateTime in endOfThisWeek until endOfNextWeek
                    DateBucket.NEXT_N_DAYS -> task.completionDateTime in startOfToday until endOfNextNDays
                    DateBucket.PENDING_FOLLOW_UP -> task.actionNeeded == ActionNeeded.FOLLOW_UP_NEEDED && !task.status.isFinished
                    DateBucket.SOMEDAY -> task.completionDateTime >= endOfNextNDays
                }
                if (!matchesBucket) return@filter false
            }

            // Other filters
            if (criticality != null && task.criticality != criticality) return@filter false
            if (action != null && task.actionNeeded != action) return@filter false
            if (category != null && task.category != category) return@filter false
            if (status != null && task.status != status) return@filter false
            if (origin != null && task.taskOriginatedFrom != origin) return@filter false
            if (tag != null && !task.tags.contains(tag)) return@filter false

            true
        }

        return when (sortBy) {
            SortOption.DEADLINE -> filtered.sortedWith(
                compareByDescending<TaskItem> { it.isPinned }
                    .thenBy { it.completionDateTime }
            )
            SortOption.CRITICALITY -> filtered.sortedWith(
                compareByDescending<TaskItem> { it.isPinned }
                    .thenByDescending { it.criticality.level }
                    .thenBy { it.completionDateTime }
            )
            SortOption.CREATED_AT -> filtered.sortedWith(
                compareByDescending<TaskItem> { it.isPinned }
                    .thenByDescending { it.createdAt }
            )
            SortOption.TITLE -> filtered.sortedWith(
                compareByDescending<TaskItem> { it.isPinned }
                    .thenBy { it.title.lowercase() }
            )
        }
    }

    companion object {
        fun factory(app: TaskPulseApplication): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TaskPulseViewModel(
                    repository = app.taskRepository,
                    prefsRepository = app.userPreferencesRepository,
                    alarmScheduler = app.alarmScheduler,
                    nlpParser = app.naturalLanguageTaskParser,
                    duplicateDetector = app.duplicateDetector,
                    sheetsExportManager = app.sheetsExportManager,
                    calendarSyncManager = app.calendarSyncManager
                ) as T
            }
        }
    }
}
