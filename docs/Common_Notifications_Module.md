# HealthX: Common Notifications Module

This document outlines the architecture, data flow, and file structure for the Common Notifications module in the HealthX app.

This system is designed to handle informational, non-action-critical push notifications (like OTPs, Alerts, AI Chats, and Advertisements) using a robust offline-first, Room-backed architecture.

---

# 1. High-Level Architecture & Data Flow

The notification system follows an **Offline-First MVVM (Model-View-ViewModel) architecture**.

## Standard Notification Lifecycle

### 1. Server Push

The backend sends an FCM (Firebase Cloud Messaging) data payload.

↓

### 2. Interception (FCM Service)

The Android app receives the payload silently in the background.

↓

### 3. Persistence (Repository)

Before any UI is shown, the payload is parsed and saved to the local Room Database.

↓

### 4. System Tray (Display Manager)

A physical pop-up is built and pushed to the Android notification tray, utilizing **Group Keys** to stack similar notifications.

↓

### 5. User Interaction (UI)

When the user taps the physical notification, they are routed to the `CommonNotificationActivity`.

↓

### 6. Data Observation (ViewModel)

The Activity observes the Room Database via a Kotlin Flow.

The UI instantly updates with unread counts, search filters, and read/unread states.

---

# 2. File Structure & Responsibilities

All core notification logic is contained within the `notification_manager` package to ensure domain isolation, alongside the central database and UI files.

---

## A. Core Definitions

### `app/src/main/java/com/example/healthx/notification_manager/NotificationCategories.kt`

**Role:**
Defines the Kotlin enum class for all possible notification types.

This mirrors the Mongoose backend discriminators.

Examples:

```text
OTP
NEW_AI_CHAT_RECEIVED
ADVERTISEMENT
```

---

## B. Database & Persistence Layer

### `app/src/main/java/com/example/healthx/notification_manager/NotificationEntity.kt`

**Role:**
The Room `@Entity` data model representing a single notification row in the local SQLite database.

---

### `app/src/main/java/com/example/healthx/notification_manager/NotificationDao.kt`

**Role:**
The Data Access Object.

Contains the SQL queries to:

* Insert notifications
* Fetch active ones (excluding expired)
* Count unread messages
* Delete items

Returns data as:

```kotlin
Flow<List<NotificationEntity>>
```

---

### `app/src/main/java/com/example/healthx/data/local/AppDatabase.kt`

**Role:**
The central Room database container holding both:

* `ReminderEntity`
* `NotificationEntity`

---

## C. Processing & Routing Layer

### `app/src/main/java/com/example/healthx/notification_manager/NotificationRepository.kt`

**Role:**
Acts as the mediator between the incoming FCM payload and the local database.

Contains:

```kotlin
saveCommonNotification()
```

Runs on an IO dispatcher to safely write to disk before triggering UI pop-ups.

---

### `app/src/main/java/com/example/healthx/notification_manager/NotificationDisplayManager.kt`

**Role:**
Manages the Android `NotificationManager`.

Responsibilities:

* Translates database entities into visually grouped system tray pop-ups
* Uses `NotificationCompat.Builder`
* Handles asynchronous image downloading via:

```kotlin
URL().openConnection()
```

* Assigns `PendingIntent` to route taps to the specific app screen

---

## D. Presentation Layer (UI)

### `app/src/main/java/com/example/healthx/notification_manager/NotificationViewModel.kt`

**Role:**
Holds the UI state.

Uses Kotlin `combine` to merge:

* Active database flow
* Dynamic search query

Provides:

* Filtered notifications
* Real-time updates

Handles user intents:

```kotlin
markAsRead()
deleteNotification()
```

---

### `app/src/main/java/com/example/healthx/ui/CommonNotificationActivity.kt`

**Role:**
The Jetpack Compose Activity.

Contains visual layouts:

```text
NotificationScreenContainer
NotificationCard
NotificationDetailScreen
```

Implements:

* Custom `BackHandler`

Navigation flow:

```text
Detail → List → MainActivity
```

Integrates:

```text
SwipeToDismissBox
```

for fluid deletion.

---

# 3. Key Technical Concepts

---

## A. Database-Driven UI (Single Source of Truth)

By saving the FCM payload directly to Room before showing the pop-up, we guarantee that no data is lost if the user accidentally swipes away the notification without opening it.

The Compose UI never reads data directly from an Intent.

It only ever reads from the Room database.

---

## B. Notification Grouping

Inside `NotificationDisplayManager.kt`, notifications are assigned a `GROUP_KEY`.

Example:

```text
GROUP_AI_CHAT
```

If the user receives 5 AI chat messages, Android automatically stacks them into a single expandable bundle in the system tray.

This prevents status bar clutter.

---

## C. Reactive Search Filtering

Inside `NotificationViewModel`, search functionality is performed locally on the device.

No network calls are required.

The system:

* Observes a `StateFlow`
* Tracks user keystrokes
* Filters `Flow<List<NotificationEntity>>`
* Ignores case sensitivity

---

## D. Image Handling

### System Tray

`NotificationDisplayManager` uses a Coroutine to download the image synchronously before building the `BigPictureStyle` notification.

---

### In-App UI

`CommonNotificationActivity` delegates image loading to the Coil library.

Uses:

```text
AsyncImage
```

Features:

* Memory caching
* Asynchronous loading
* Smooth scrolling

---

# 4. Required Dependencies

To ensure this module compiles, the following libraries must be present in `build.gradle.kts`.

---

## Jetpack Compose

Used for the UI layer.

---

## Room (KSP)

Required dependencies:

```text
androidx.room:room-runtime
androidx.room:room-ktx
androidx.room:room-compiler
```

Used via:

```text
KSP
```

Provides local database persistence.

---

## Coil

Dependency:

```text
io.coil-kt:coil-compose
```

Used for rendering remote image URLs inside the detailed notification screen.

---

## Coroutines

Used for:

* Background processing
* Reactive data streams (`Flow`)

---

## Firebase Messaging

Dependency:

```text
com.google.firebase:firebase-messaging
```

Used for receiving the initial server push.
