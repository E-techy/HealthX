# FCM Token Synchronization API

## Overview

The `FcmTokenSyncManager` is a background utility in the HealthX Android app responsible for syncing the Firebase Cloud Messaging (FCM) device token with the backend Node.js server.

It ensures the server always has the correct routing address to send push notifications to the user's specific device.

---

# 1. Android Client Function Signature

```kotlin
suspend fun syncToken(
    backendUrl: String,
    jwtToken: String,
    deviceId: String,
    deviceName: String,
    syncMode: SyncMode = SyncMode.ONLINE
)
```

## Parameter Breakdown

### `backendUrl`

The full endpoint URL.

Example:

```text
https://api.healthx.com/v1/devices/token
```

---

### `jwtToken`

The active user's JWT string.

Handled as a Bearer token.

---

### `deviceId`

A unique, persistent identifier for the physical device.

---

### `deviceName`

The readable name of the hardware.

Example:

```text
Samsung Galaxy S23
```

---

### `syncMode`

Dictates the network behavior.

Supported values:

```text
ONLINE
OFFLINE
```

---

# 2. Backend API Expectations

When the Android client fires the sync request, the Node.js server must be prepared to handle the following request format.

---

## HTTP Method

```http
POST
```

---

## Content-Type

```http
application/json
```

---

## Required Headers

The server must extract the user identity from the JWT provided in the Authorization header.

```http
Authorization: Bearer <jwtToken>
```

---

## Request Body (JSON Payload)

The backend should expect these exact variable names in the `req.body`.

```json
{
  "deviceId": "string",
  "deviceName": "string",
  "fcmToken": "string"
}
```

---

## Expected Server Response

### Success (200 OK)

The server successfully saved/updated the token.

The Android client relies on a `2xx` success code to update its local cache.

---

### Failure (4xx / 5xx)

The token was not saved.

The Android client will retain its old cache and retry the next time the app opens.

---

# 3. Operational Modes

---

## SyncMode.ONLINE (Force Sync)

### Behavior

Bypasses local caching and forces an immediate HTTP POST request to the server.

### Use Case

Triggered immediately after a user successfully logs in, ensuring the new session is immediately bound to the device's push token.

---

## SyncMode.OFFLINE (Smart Sync)

### Behavior

Fetches the real FCM token and compares it to a locally stored `CLOUD_MIRRORED_FCM_TOKEN`.

It only executes the HTTP POST request if the tokens do not match (e.g., Firebase rotated the token in the background).

### Use Case

Triggered seamlessly during `MainActivity.onCreate()` every time the app is launched.

It guarantees token freshness without spamming the backend database with redundant requests.

---
