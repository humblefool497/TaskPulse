package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.TaskDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("taskpulse_preferences", Context.MODE_PRIVATE)

    private val _nextNDays = MutableStateFlow(prefs.getInt(KEY_NEXT_N_DAYS, 30))
    val nextNDays: StateFlow<Int> = _nextNDays.asStateFlow()

    private val _morningDigestTime = MutableStateFlow(
        Pair(prefs.getInt(KEY_MORNING_HOUR, 8), prefs.getInt(KEY_MORNING_MINUTE, 30))
    )
    val morningDigestTime: StateFlow<Pair<Int, Int>> = _morningDigestTime.asStateFlow()

    private val _eveningReviewTime = MutableStateFlow(
        Pair(prefs.getInt(KEY_EVENING_HOUR, 20), prefs.getInt(KEY_EVENING_MINUTE, 0))
    )
    val eveningReviewTime: StateFlow<Pair<Int, Int>> = _eveningReviewTime.asStateFlow()

    private val _weekStartDay = MutableStateFlow(prefs.getInt(KEY_WEEK_START, Calendar.MONDAY))
    val weekStartDay: StateFlow<Int> = _weekStartDay.asStateFlow()

    private val _escalatingReminders = MutableStateFlow(prefs.getBoolean(KEY_ESCALATING_REMINDERS, true))
    val escalatingReminders: StateFlow<Boolean> = _escalatingReminders.asStateFlow()

    private val _escalateIntervalMinutes = MutableStateFlow(prefs.getInt(KEY_ESCALATE_INTERVAL, 15))
    val escalateIntervalMinutes: StateFlow<Int> = _escalateIntervalMinutes.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _customCategories = MutableStateFlow(
        prefs.getStringSet(KEY_CUSTOM_CATEGORIES, TaskDefaults.DEFAULT_CATEGORIES.toSet())?.toList()
            ?: TaskDefaults.DEFAULT_CATEGORIES
    )
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    private val _customOrigins = MutableStateFlow(
        prefs.getStringSet(KEY_CUSTOM_ORIGINS, TaskDefaults.DEFAULT_ORIGINS.toSet())?.toList()
            ?: TaskDefaults.DEFAULT_ORIGINS
    )
    val customOrigins: StateFlow<List<String>> = _customOrigins.asStateFlow()

    private val _googleCalendarConnected = MutableStateFlow(prefs.getBoolean(KEY_GOOGLE_CAL_CONNECTED, false))
    val googleCalendarConnected: StateFlow<Boolean> = _googleCalendarConnected.asStateFlow()

    private val _outlookCalendarConnected = MutableStateFlow(prefs.getBoolean(KEY_OUTLOOK_CAL_CONNECTED, false))
    val outlookCalendarConnected: StateFlow<Boolean> = _outlookCalendarConnected.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    fun setNextNDays(days: Int) {
        prefs.edit().putInt(KEY_NEXT_N_DAYS, days).apply()
        _nextNDays.value = days
    }

    fun setMorningDigestTime(hour: Int, minute: Int) {
        prefs.edit().putInt(KEY_MORNING_HOUR, hour).putInt(KEY_MORNING_MINUTE, minute).apply()
        _morningDigestTime.value = Pair(hour, minute)
    }

    fun setEveningReviewTime(hour: Int, minute: Int) {
        prefs.edit().putInt(KEY_EVENING_HOUR, hour).putInt(KEY_EVENING_MINUTE, minute).apply()
        _eveningReviewTime.value = Pair(hour, minute)
    }

    fun setWeekStartDay(dayOfWeek: Int) {
        prefs.edit().putInt(KEY_WEEK_START, dayOfWeek).apply()
        _weekStartDay.value = dayOfWeek
    }

    fun setEscalatingReminders(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ESCALATING_REMINDERS, enabled).apply()
        _escalatingReminders.value = enabled
    }

    fun setEscalateIntervalMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_ESCALATE_INTERVAL, minutes).apply()
        _escalateIntervalMinutes.value = minutes
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        _appLockEnabled.value = enabled
    }

    fun addCustomCategory(category: String) {
        val current = _customCategories.value.toMutableList()
        if (!current.contains(category)) {
            current.add(category)
            prefs.edit().putStringSet(KEY_CUSTOM_CATEGORIES, current.toSet()).apply()
            _customCategories.value = current
        }
    }

    fun addCustomOrigin(origin: String) {
        val current = _customOrigins.value.toMutableList()
        if (!current.contains(origin)) {
            current.add(origin)
            prefs.edit().putStringSet(KEY_CUSTOM_ORIGINS, current.toSet()).apply()
            _customOrigins.value = current
        }
    }

    fun setGoogleCalendarConnected(connected: Boolean) {
        prefs.edit().putBoolean(KEY_GOOGLE_CAL_CONNECTED, connected).apply()
        _googleCalendarConnected.value = connected
    }

    fun setOutlookCalendarConnected(connected: Boolean) {
        prefs.edit().putBoolean(KEY_OUTLOOK_CAL_CONNECTED, connected).apply()
        _outlookCalendarConnected.value = connected
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply()
        _onboardingCompleted.value = completed
    }

    companion object {
        private const val KEY_NEXT_N_DAYS = "key_next_n_days"
        private const val KEY_MORNING_HOUR = "key_morning_hour"
        private const val KEY_MORNING_MINUTE = "key_morning_minute"
        private const val KEY_EVENING_HOUR = "key_evening_hour"
        private const val KEY_EVENING_MINUTE = "key_evening_minute"
        private const val KEY_WEEK_START = "key_week_start"
        private const val KEY_ESCALATING_REMINDERS = "key_escalating_reminders"
        private const val KEY_ESCALATE_INTERVAL = "key_escalate_interval"
        private const val KEY_APP_LOCK = "key_app_lock"
        private const val KEY_CUSTOM_CATEGORIES = "key_custom_categories"
        private const val KEY_CUSTOM_ORIGINS = "key_custom_origins"
        private const val KEY_GOOGLE_CAL_CONNECTED = "key_google_cal_connected"
        private const val KEY_OUTLOOK_CAL_CONNECTED = "key_outlook_cal_connected"
        private const val KEY_ONBOARDING_DONE = "key_onboarding_done"
    }
}
