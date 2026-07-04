# 🧬 HealthX Alarm Engine

> **High-Reliability Offline-First Alarm Scheduling Engine**

The HealthX Alarm Engine is a high-reliability, offline-first scheduling system designed to handle thousands of exact-time health alerts without hitting Android OS limits (**500 maximum exact alarms**).

It uses a **Rolling Schedule** pattern backed by a local **SQLite/Room** database, features a custom **Foreground Service** for complex audio playback (Local, TTS, Network), and includes an asynchronous **Cloud Sync Manager**.

---

# 📚 Table of Contents

* [Overview](#-overview)
* [Core Components](#-core-components)
* [Data Models (Database Schema)](#-data-models-database-schema)
* [End-to-End Workflows](#️-end-to-end-workflows)

    * [1. Scheduling Flow](#1-the-scheduling-flow-app-or-cloud--database)
    * [2. Execution Flow](#2-the-execution-flow-ringing-the-alarm)
    * [3. Snooze and Stop Flow](#3-snooze-and-stop-flow)
    * [4. Cloud Sync Manager Flow](#4-cloud-sync-manager-flow)
    * [5. Reboot Flow](#5-the-reboot-flow-device-restart)
* [Required Android Permissions](#️-required-android-permissions)

---

# 📖 Overview

The HealthX Alarm Engine is a high-reliability, offline-first scheduling system designed to handle thousands of exact-time health alerts without hitting Android OS limits (**500 maximum exact alarms**).

It uses a **Rolling Schedule** pattern backed by a local **SQLite/Room** database, features a custom **Foreground Service** for complex audio playback (**Local, TTS, Network**), and includes an asynchronous **Cloud Sync Manager**.

---

# 🏗️ Core Components

### Local Database (Room)

The absolute source of truth.

Stores all alarms for the next **10+ years**.

---

### Rolling Schedule Engine

A utility that queries the database and only feeds the next **24 hours** of alarms to the Android AlarmManager.

---

### Trigger Receiver (BroadcastReceiver)

Wakes up exactly on time and instantly launches the Foreground Service.

---

### Media Foreground Service

Keeps the CPU awake, handles complex audio (**Local, TTS, Stream**), and launches the Notification.

---

### Full-Screen Notification

The UI that pops up over the lock screen allowing the user to **Stop** or **Snooze**.

---

### Boot Receiver

Re-initializes the Rolling Schedule when the phone is turned on.

---

### Sync Manager

Syncs the local database with the cloud via App Open or Firebase Cloud Messaging (FCM).

---

# 💾 Data Models (Database Schema)

To make this work, your Room Database needs an `AlarmEntity` table with at least the following variables:

```kotlin
@Entity(tableName = "alarms")
data class AlarmEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,                    // Used as the PendingIntent Request Code

    val remoteId: String?,              // The ID from your cloud database (UUID)

    val triggerTimeMillis: Long,        // Exact UTC millisecond timestamp

    val audioPlaybackType: String,      // Enum: LOCAL_FILE, TTS, SERVER_STREAM

    val localAudioUri: String?,         // Path to file if type is LOCAL_FILE

    val ttsContent: String?,            // Text to read if type is TTS

    val status: String,                 // Enum: PENDING, COMPLETED, CANCELLED

    val isSnoozed: Boolean = false,     // True if currently in a snooze cycle

    val title: String,

    val description: String,

    // Interval Scheduling

    val isRecurring: Boolean = false,

    val recurrenceType: String?,        // DAILY, WEEKLY, MONTHLY

    val recurrenceInterval: Int?,       // Every X days/weeks/months

    val recurrenceStartDate: Long?,     // UTC start date

    val recurrenceEndDate: Long?        // UTC end date
)
```

---

## Interval Scheduling Logic

Some alarms are **one-time alarms**, while others repeat over a defined duration.

Example:

* Every day
* Start Date: **1 July**
* End Date: **5 July**
* Time: **8:00 AM**

The alarm will automatically trigger:

* 1 July
* 2 July
* 3 July
* 4 July
* 5 July

all at **8:00 AM**, after which it will automatically stop scheduling.

For non-recurring alarms:

```text
isRecurring = false
```

The recurrence fields remain `null`.

The Rolling Schedule Engine must evaluate these recurrence fields while generating the next 24-hour schedule.

---

# ⚙️ End-to-End Workflows

## 1. The Scheduling Flow (App or Cloud -> Database)

User creates an alarm OR the Sync Manager pulls a new alarm from the server.

The alarm is saved as **PENDING** in the Room Database.

The app calls:

```text
RollingScheduleEngine.updateOSAlarms()
```

This engine queries:

```sql
SELECT *
FROM alarms
WHERE status = 'PENDING'
AND triggerTimeMillis < (currentTime + 24_HOURS)
```

For recurring alarms, the engine additionally validates:

* Current date is greater than or equal to `recurrenceStartDate`
* Current date is less than or equal to `recurrenceEndDate`
* The recurrence rule matches today's date

It iterates through the results and schedules them with:

```text
AlarmManager.setExactAndAllowWhileIdle()
```

---

## 2. The Execution Flow (Ringing the Alarm)

OS reaches the exact time and triggers `AlarmReceiver`.

`AlarmReceiver` receives the `id` and instantly starts `MediaForegroundService`.

`MediaForegroundService` fetches the alarm details from the database.

### Audio Logic

#### SERVER_STREAM

Attempts network call.

Falls back to TTS if no internet.

---

#### TTS

Initializes Text-To-Speech and reads `ttsContent`.

---

#### LOCAL_FILE

Plays `localAudioUri` via `MediaPlayer`.

---

### Notification

The service fires a High-Priority Notification with a `setFullScreenIntent`.

This forces the screen to wake up and display a popup Activity (even if locked) with:

* STOP
* SNOOZE

buttons.

---

## 3. Snooze and Stop Flow

### If User clicks STOP

* MediaForegroundService is stopped (audio stops).
* Alarm status in the database is updated to **COMPLETED**.
* App calls:

```text
RollingScheduleEngine.updateOSAlarms()
```

to queue the next batch.

---

### If User clicks SNOOZE (5 mins)

* MediaForegroundService is stopped (audio stops).
* A new one-off exact alarm is scheduled directly with the OS for:

```text
currentTime + 5 Minutes
```

* Database `isSnoozed` flag is set to **true**.

---

# 4. Cloud Sync Manager Flow

The Sync Manager ensures the local DB and the Server are identical.

It is triggered in two ways.

---

## Trigger A (App Open)

In your `MainActivity.onCreate()`, launch a Kotlin Coroutine to fetch:

```text
/api/alarms/sync
```

---

## Trigger B (Server Push)

The server sends an FCM (Firebase Cloud Messaging) **Data Message** (not a notification message, must be a silent data payload).

---

### Execution

FCM triggers `MyFirebaseMessagingService`.

It passes the sync task to Android WorkManager (to ensure it runs reliably in the background).

WorkManager fetches the latest JSON from the server.

It performs an **Upsert (Update/Insert)** on the Room Database using the `remoteId`.

It deletes any local alarms that the server marked as cancelled.

Finally, it calls:

```text
RollingScheduleEngine.updateOSAlarms()
```

to apply the changes to the Android OS.

---

# 5. The Reboot Flow (Device Restart)

User restarts the phone (wiping the OS AlarmManager memory).

Android broadcasts:

```text
BOOT_COMPLETED
```

BootReceiver intercepts this broadcast.

BootReceiver calls:

```text
RollingScheduleEngine.updateOSAlarms()
```

The local database repopulates the OS with the upcoming 24 hours of alarms.

No alarms are missed.

---

# 🛡️ Required Android Permissions

Add these to the `AndroidManifest.xml` for the architecture to function.

```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

<uses-permission android:name="android.permission.USE_EXACT_ALARM" />

<uses-permission android:name="android.permission.WAKE_LOCK" />

<uses-permission android:name="android.permission.DISABLE_KEYGUARD" />

<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />

<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<uses-permission android:name="android.permission.INTERNET" />

<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```
