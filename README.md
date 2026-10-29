# TaskPulse ⚡

[![Android CI](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room-SQLite%202.7-F80000.svg?style=flat)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**TaskPulse** is a professional task, reminder, and follow-up manager engineered for professionals who juggle demanding official responsibilities and personal commitments. Built with zero compromises on performance and reliability, TaskPulse prevents task slippage through intelligent criticality triage, deadline bucketing, escalating exact alarms, background digestion, and seamless integrations with Google Calendar, Outlook, and Google Sheets.

---

## 📸 Highlights & Screenshots

- **Day & Night Themes**: Automatically adapts to your phone's system light/dark theme, with support for Material You dynamic wallpaper colors on Android 12+.
- **Command Center Dashboard**: Triage overdue tasks, view today's focus, tap quick-add natural language input, and inspect criticality chips.
- **Action Matrix**: Categorize tasks by *To Act Myself*, *Follow-up Needed*, *To Assign to Someone Else*, or *Immediate Attention*.
- **Calendar & Agenda**: Visual dot indicators color-coded by urgency with native Android `CalendarContract` sync.
- **Export & Backup**: One-tap formatted Google Sheets and CSV export, plus complete JSON backup and restore.

---

## 🌟 Core Features

### 1. Dashboard Command Center
- **Smart Greeting**: Displays dynamic local date and real-time urgency metrics (e.g. `2 overdue tasks require immediate action`).
- **Deadline Bucket Cards**: Horizontally scrollable summary cards for **Overdue** (pulsing red priority), **Due Today**, **Due This Week**, **Due Next Week**, **Next N Days** (user-configurable, default 30), and **Pending Follow-ups**.
- **Criticality Triage Matrix**: Symmetrical 4-card priority matrix (Critical, High, Medium, Low) featuring perfectly centered notification badges and one-tap filtering.
- **Needs Immediate Attention**: Pinned spotlight section for Critical tasks and immediate action items.
- **Natural Language Quick Add**: Type phrases like `"Call vendor tomorrow 5pm high follow-up"` to instantly parse title, deadline, priority, action, and category without opening forms.

### 2. Task Model & Field Architecture
Every task contains complete, production-grade tracking attributes:
- **Title**: Required, with character limits and counters (up to 120 chars).
- **Description**: Multi-line markdown notes.
- **Criticality / Importance**:
  - 🔴 **Critical** (Level 4)
  - 🟠 **High** (Level 3)
  - 🔵 **Medium** (Level 2)
  - 🟢 **Low** (Level 1)
- **Action Needed**:
  - *Immediate Attention / Completion Needed*
  - *To Act Myself*
  - *Follow-up Needed* (reveals dedicated Follow-up Date and custom alarm)
  - *To Assign to Someone Else* (reveals Assignee name/contact with device contact picker)
- **Origin Tracking**: Autocomplete history remembering where tasks originated (e.g., Boss, Client, Meeting, Email, Family, Vendor, Self).
- **Taxonomy / Categories**: Official, Personal, Finance/Bills, Health, Family, Education/Kids, Shopping/Errands, Travel, Home/Maintenance, Learning/Skill, Legal/Compliance, Meetings, Other (plus user-created custom categories).
- **Subtasks & Checklists**: Progress bar with live completion tracking.
- **Recurrence Engine**: None, Daily, Weekly, Monthly, Yearly, and Custom Intervals with auto-generation of next occurrences on task completion.
- **Timestamped Activity Logs**: Chronological progress notes (e.g., `"Left voicemail with vendor at 10:15 AM"`).
- **Pinned / Starred Flags**: Elevates priority tasks to the top of all views.

### 3. Day & Night System Theming
- **Adaptive Mode**: Follows your Android device's day/night theme automatically.
- **Theme Lock**: Choose **System Default**, **Light Mode**, or **Dark Mode** in Settings.
- **Material You Dynamic Colors**: On Android 12+ (API 31+), matches accent colors directly to the user's wallpaper.
- **Accessibility & Contrast**: Conforms to WCAG AA contrast standards with custom semantic tokens for both light and dark backgrounds.

### 4. Background Alarms & WorkManager
- **Exact Alarms**: High-precision `AlarmManager` reminders configured with `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`.
- **Pre-deadline Offsets**: Multiple reminders per task (at deadline, 15m, 1h, 1d, 3d before, or custom).
- **Notification Channels**: Specialized channels per criticality level with lockscreen visibility and quick actions:
  - ✅ **Complete**: Marks task complete directly from the status bar.
  - ⏰ **Snooze 1h**: Postpones alarm by 60 minutes with one tap.
- **Daily Digest Worker**: Periodic WorkManager worker for Morning Focus (8:30 AM) and Evening Review (8:00 PM).
- **Boot Recovery**: `BootReceiver` automatically reschedules pending alarms upon device restart.
- **Trash Auto-Purge**: Permanently clears items deleted more than 30 days ago.

### 5. Calendar & Google Sheets Sync
- **Device Calendar Sync**: Direct zero-latency synchronization with Google Calendar, Microsoft Outlook, or Exchange via `CalendarContract`.
- **Google Sheets / CSV Export**: Formatted tables with header styling, colored priority tags, and complete metadata ready for executive reporting.
- **JSON Backup & Restore**: Export full database snapshots as portable JSON files to share or restore anytime.

---

## 🏛️ Project Architecture

TaskPulse follows **Clean Architecture** with the **MVVM** pattern:

```
app/src/main/java/com/example/
├── TaskPulseApplication.kt          # Application lifecycle, dependency initialization
├── MainActivity.kt                  # Edge-to-edge entry point, theme binding, navigation
├── data/
│   ├── TaskPulseDatabase.kt        # Room database definition with type converters
│   ├── TaskDao.kt                  # Optimized SQLite queries with Kotlin Flow
│   ├── TaskRepository.kt           # Central data source for CRUD and exports
│   └── UserPreferencesRepository.kt# Preferences (theme, digest times, custom categories)
├── engine/
│   ├── NaturalLanguageTaskParser.kt# Heuristic NLP parser for dates, priorities, actions
│   ├── RecurrenceEngine.kt         # Calculation of next recurring deadlines
│   ├── CalendarSyncManager.kt      # Android CalendarContract integration
│   ├── SheetsExportManager.kt      # CSV and Google Sheets format generator
│   └── TaskDuplicateDetector.kt    # Levenshtein similarity detection
├── model/
│   ├── TaskItem.kt                 # Core task entity (Room @Entity)
│   ├── TaskEnums.kt                # Criticality, ActionNeeded, Status, Recurrence
│   └── TaskDefaults.kt             # Preset templates, origins, and categories
├── notification/
│   ├── NotificationHelper.kt       # Notification channels and notification builder
│   └── TaskPulseAlarmScheduler.kt  # Exact alarm scheduling via AlarmManager
├── receiver/
│   ├── AlarmReceiver.kt            # Broadcast receiver for reminder events
│   └── BootReceiver.kt             # Device restart boot receiver
├── worker/
│   └── DailyDigestWorker.kt        # WorkManager worker for daily digests and cleanup
└── ui/
    ├── TaskPulseViewModel.kt       # Unified state management with StateFlow
    ├── navigation/Screen.kt        # Navigation routes and bottom bar destinations
    ├── theme/                      # Day/Night Color schemes, Type, Theme.kt
    ├── components/                 # TaskCard, QuickAddBar, TemplatePicker, DatePickers
    └── screens/
        ├── dashboard/              # Command Center, metric cards, triage matrix
        ├── tasks/                  # Task list, search, multi-filter, bulk selection
        ├── editor/                 # Full-featured task editor & creator
        ├── calendar/               # Month, week, agenda views with dot badges
        ├── insights/               # Productivity analytics & completion graphs
        └── settings/               # Appearance, calendar sync, backup & restore
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug (2024.2) or newer recommended
- **JDK**: Java 17 or Java 21
- **Android SDK**: Compile SDK 36, Minimum SDK 26 (Android 8.0 Oreo)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/your-username/TaskPulse.git
cd TaskPulse

# Run local JVM unit & Robolectric tests
gradle :app:testDebugUnitTest

# Assemble Debug APK
gradle :app:assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 Testing

TaskPulse includes automated test suites covering data models, recurrence calculations, and NLP parsing:

```bash
# Execute unit tests
gradle test

# Execute app unit tests specifically
gradle :app:testDebugUnitTest
```

Key test files:
- `ExampleUnitTest.kt`: Unit tests for NLP parsing, recurrence calculation, and criticality levels.
- `ExampleRobolectricTest.kt`: Robolectric tests validating UI components and database interactions without requiring an emulator.

---

## 📄 License

```
Copyright 2026 TaskPulse Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
