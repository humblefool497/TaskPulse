# TaskPulse

**TaskPulse** is a professional task, reminder, and follow-up manager engineered for people who juggle demanding official and personal responsibilities. It ensures **zero task slippage** through intelligent criticality triage, action tracking, escalating exact alarms, recurring deadlines, and seamless calendar & Google Sheets integrations.

---

## 🌟 Key Features

1. **Dashboard Command Center**
   - Live greeting with dynamic date and urgent status indicator.
   - Metric summary cards: **Overdue** (red, priority), **Due Today**, **Due This Week**, **Due Next Week**, **Due in Next N Days** (configurable, default 30), and **Pending Follow-ups**.
   - **Criticality Triage Matrix**: Critical, High, Medium, and Low counts as filter chips.
   - **Needs Immediate Attention**: Pinned carousel for Critical & Immediate Action tasks.
   - **Today's Focus**: Top tasks of the day sorted by priority and deadline.
   - **Natural Language Quick Add**: Type phrases like `"Call vendor tomorrow 5pm high follow-up"` to instantly parse title, deadline, priority, action, and category.

2. **Task Classification & Model**
   - **Title**: Required, character counter (up to 120 chars).
   - **Criticality**: Critical (Red), High (Orange), Medium (Blue), Low (Green), each with distinct accessibility icons.
   - **Action Needed**: Follow-up Needed, To Act Myself, To Assign to Someone Else, Immediate Attention / Completion Needed.
   - **Task Origin**: Autocomplete dropdown remembering previous values (Boss, Client, Meeting, Email, Family, Vendor, Self).
   - **Categories**: Official, Personal, Finance/Bills, Health, Family, Education/Kids, Shopping/Errands, Travel, Home/Maintenance, Learning/Skill, Legal/Compliance, Meetings, Other (plus custom user-created categories).
   - **Subtasks / Checklist**: With interactive progress bar and item count.
   - **Reminders**: Multiple reminder offsets (at deadline, 15m, 1h, 1d, 3d before) via exact `AlarmManager` alarms.
   - **Recurrence**: None, Daily, Weekly, Monthly, Yearly, Custom Interval with automatic generation of the next occurrence upon completion.
   - **Assignee & Follow-up**: Conditional fields with contact details and follow-up alerts.
   - **Progress Log**: Timestamped activity updates.

3. **Calendar Integration**
   - **In-App Calendar**: Month view, Week view, and Agenda view with dot indicators color-coded by task criticality.
   - **Device & Cloud Calendar Sync**: Immediate zero-setup sync via Android's `CalendarContract` to Google Calendar or Outlook Calendar accounts on the device.
   - Cloud REST API support for Google Calendar API v3 and Microsoft Graph Calendar API.

4. **Google Sheets & CSV Export**
   - Export all or filtered/selected tasks directly to CSV or Google Sheets.
   - Complete columns: Title, Description, Criticality, Action Needed, Originated From, Category, Deadline, Status, Assigned To, Follow-up Date, Tags, Recurrence, Created At, Completed At.
   - Formatted header row (bold, frozen, navy background), auto-sized columns, and color-coded priority cells.
   - Shareable via Android Share Sheet.

5. **Notifications & Reminders**
   - Channels configured per criticality level (`channel_critical` with DND bypass support).
   - Notification actions: **Mark Done** and **Snooze 1h** directly from the status bar.
   - `DailyDigestWorker` via WorkManager for Morning Focus Digest and Evening Review.
   - `BootReceiver` reschedules all exact alarms upon device reboot.
   - Auto-cleanup of deleted tasks from trash after 30 days.

6. **Insights & Analytics**
   - Completion rate score ring.
   - Overdue load analysis and active in-progress counts.
   - Criticality, category, and origin breakdown bars.

---

## 🛠️ Architecture & Technologies

- **Architecture**: MVVM with Clean Architecture and Repository Pattern.
- **Language**: Kotlin 2.2 with Kotlin Coroutines & Flow.
- **UI Framework**: Jetpack Compose with Material 3 (M3).
- **Local Persistence**: Android Room 2.7 with KSP symbol processing.
- **Background Tasks**: Android WorkManager 2.10 and AlarmManager exact alarms (`SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`).
- **Navigation**: Jetpack Navigation Compose with type-safe routing.
- **Adaptive UI**: Responsive layouts supporting compact phones and wide screens/tablets (`NavigationRail`).

---

## 🔑 Setup Guide: Google & Microsoft API Credentials

### 1. Google Calendar & Google Sheets API Setup

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project named `TaskPulse`.
3. In **APIs & Services > Library**, enable:
   - `Google Calendar API`
   - `Google Sheets API`
   - `Google Drive API`
4. In **OAuth consent screen**:
   - Set User Type to **External** (or Internal for Workspace organizations).
   - Add scopes:
     - `https://www.googleapis.com/auth/calendar.events`
     - `https://www.googleapis.com/auth/spreadsheets`
     - `https://www.googleapis.com/auth/drive.file`
5. In **Credentials > Create Credentials > OAuth client ID**:
   - Application type: **Android**
   - Package name: `com.aistudio.taskpulse.wqkvtz`
   - SHA-1 certificate fingerprint: Obtain from keystore using:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```

### 2. Microsoft Outlook Calendar (MSAL / Microsoft Graph) Setup

1. Go to the [Microsoft Entra ID (Azure Portal)](https://portal.azure.com/).
2. Navigate to **App registrations > New registration**.
3. Set name to `TaskPulse` and supported account types to **Accounts in any organizational directory and personal Microsoft accounts**.
4. In **Authentication > Add a platform > Android**:
   - Package name: `com.aistudio.taskpulse.wqkvtz`
   - Signature hash: Obtain the Base64 SHA-1 hash from your keystore.
5. In **API permissions > Add a permission > Microsoft Graph**:
   - Add Delegated permissions: `Calendars.ReadWrite`, `User.Read`.

*Note: TaskPulse includes native device calendar integration out of the box, allowing instant one-tap sync to whichever Google or Microsoft account is signed into the Android device without waiting for cloud client IDs.*

---

## 📦 How to Build and Generate APK

### Debug APK Build
Run in project root:
```bash
gradle :app:assembleDebug
```
Output location:
`app/build/outputs/apk/debug/app-debug.apk`

### Unit Tests
Run local JVM unit and Robolectric tests:
```bash
gradle :app:testDebugUnitTest
```

### Release Signed APK via Android Studio
1. Open the project folder in Android Studio.
2. Select **Build > Generate Signed Bundle / APK...**
3. Choose **APK**, select the keystore path and key alias.
4. Select `release` build variant and click **Finish**.
5. The signed APK will be generated in `app/release/`.

---

## 📝 Assumptions Made
1. **Device-First Storage**: Tasks and user configurations are persisted locally in Room SQLite, ensuring complete offline functionality and instantaneous load times.
2. **Zero-Setup Native Sync**: The native Android `CalendarContract` integration is used for instant calendar sync with connected device accounts (Google, Outlook, Exchange), while cloud API payload builders are provided for direct REST sync.
3. **Escalating Reminders**: For Critical tasks, alarms trigger exact notifications with high-priority audio and vibration patterns.
4. **Automatic 30-Day Trash Purge**: Items moved to trash are automatically deleted after 30 days via `DailyDigestWorker`.
