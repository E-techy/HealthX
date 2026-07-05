# 🌌 HealthX Dashboard (Home Screen Module)

The Home Screen is the central orchestrator of the HealthX application. Designed with a premium, cinematic UI, it provides users with an at-a-glance overview of their vital health statistics, daily nutrition goals, active offline-first alarms, and upcoming telemedicine consultations.

Built entirely with **Jetpack Compose**, the dashboard utilizes a highly modular architecture, separating complex UI animations and business logic into isolated components to maintain a clean, scalable codebase.

---

# 📚 Table of Contents

* [Current Functionalities](#-current-functionalities)

    * [1. Cinematic & Advanced UI Core](#1-cinematic--advanced-ui-core)
    * [2. Navigation & Command Drawer (HomeDrawerContent)](#2-navigation--command-drawer-homedrawercontent)
    * [3. Modular Dashboard Widgets](#3-modular-dashboard-widgets)
* [File Structure](#-file-structure)
* [Future Implementation Roadmap (What's Next)](#-future-implementation-roadmap-whats-next)

    * [1. Data Integration & Syncing](#1-data-integration--syncing)
    * [2. QR System Expansion](#2-qr-system-expansion)
    * [3. Navigation & Routing Completion](#3-navigation--routing-completion)
    * [4. Advanced Animations](#4-advanced-animations)

---

# ✨ Current Functionalities

## 1. Cinematic & Advanced UI Core

### Cosmic Wave Background

A custom **AnimatedWaveBackground** runs a continuous, slow-moving linear gradient (**deep space, dark purple, deep blue**) using Compose `infiniteRepeatable` animations to give the app a premium, high-tech feel.

---

### Transparent App Bar

Seamlessly blends into the animated background, featuring the user's profile photo which acts as the trigger for the Navigation Drawer.

---

### Persistent Scanner FAB

A custom-styled Floating Action Button pinned to the bottom-center for instant access to the QR/Barcode Scanner.

---

## 2. Navigation & Command Drawer (HomeDrawerContent)

### Profile & Identity

Displays the user's avatar (with dynamic initial fallbacks and hashed color generation via Coil), Name, and dynamic Subscription Tier badge.

---

### QR Profile Sharing

A dedicated action button next to the profile that triggers a full-screen, scannable QR Code containing the user's payload (`HealthX_category = profile_sharing`), encoded via the ZXing library.

---

### Routing Hub

Seamlessly integrates with `MainActivity`'s `NavHost` to route users to:

* Settings & API Keys
* HealthX AI Chat
* Reminders & Advanced Alarm Manager
* Subscriptions
* Account Switching & Secure Logout

---

## 3. Modular Dashboard Widgets

The main scrollable `LazyColumn` is split into distinct, animated files.

### Health Stats Grid (`HomeHealthStats.kt`)

* Displays Heart Rate, SpO2, Blood Pressure, and Sleep data.
* Features a custom `animatedGlowingBorder` modifier and deep-tinted cards for a sleek look.

---

### Liquid Nutrition Goals (`HomeNutrition.kt`)

* Tracks Calories, Water, and Protein intake.
* Uses a custom Compose `<Canvas>` to draw mathematical sine-waves, creating a real-time fluid/liquid fill animation that rises based on the user's completion percentage.

---

### Upcoming Alarms (`HomeUpcomingAlarms.kt`)

* Reads directly from the `AlarmManagerViewModel`'s `StateFlow`.
* Displays the next 2 upcoming/running alarms.
* Dynamically changes color to Red with a warning icon if an alarm is currently ringing.

---

### Scheduled Consultations (`HomeMeetings.kt`)

Displays upcoming telemedicine/video call appointments with doctors.

---

# 📂 File Structure

```text
com.example.healthx.ui.screens.home/
│
├── HomeScreen.kt                 # Main orchestrator, layout, and background animation
├── HomeViewModel.kt              # Handles user session, auth state, and subscription status
│
└── components/                   # Isolated UI Modules
    ├── HomeDrawer.kt             # Side-menu navigation and Profile Header
    ├── HomeHealthStats.kt        # 4-grid vital signs with glowing borders
    ├── HomeNutrition.kt          # Canvas-based liquid wave animation cards
    ├── HomeUpcomingAlarms.kt     # Real-time sync with AlarmEngine Ledger
    └── HomeMeetings.kt           # Upcoming video consultation cards
```

---

# 🚀 Future Implementation Roadmap (What's Next)

While the UI and local flow are established, the following features are slated for backend integration and expansion.

---

## 1. Data Integration & Syncing

### Live Health Metrics

Replace the mocked `HealthStatsGrid` data with actual data synced from **Google Health Connect / Google Fit APIs** (or smartwatch SDKs).

---

### Nutrition API

Connect the `LiquidStatCard` percentages to a backend database that updates as the user logs food/water throughout the day.

---

### Meeting SDK Integration

Wire the `HomeMeetings.kt` card to actually launch a **WebRTC** or third-party (e.g., **Zoom/Twilio**) video call intent.

---

## 2. QR System Expansion

### Incoming QR Processing

Update the `QRScannerViewModel` to detect the `HealthX_category = profile_sharing` JSON payload. When scanned, it should automatically prompt the user to **"Add Friend/Patient"** to their network.

---

### Dynamic QR Contexts

Expand the QR generator to support:

* Medical Records sharing
* AI Chat prompt sharing

---

## 3. Navigation & Routing Completion

Currently, clicking on the Health Stats or Nutrition cards triggers a `TODO: Route` comment.

We need to build the dedicated Detailed Analytics Screens for each metric and add them to the `MainActivity NavHost`.

---

## 4. Advanced Animations

* Implement the rotating gradient logic inside the `animatedGlowingBorder` modifier (currently a static glow) using Compose `drawWithContent` and infinite rotation for an even more futuristic feel.

* Add entrance animations (`AnimatedVisibility` / `slideInVertically`) so the dashboard items cascade into view when the app launches.
