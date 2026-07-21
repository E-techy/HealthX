<div align="center">

# 🩺 HealthX Android

### Smart • AI Powered • Offline First • Secure

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white">
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white">
<img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge">
<img src="https://img.shields.io/badge/Firebase-FCM-FFCA28?style=for-the-badge&logo=firebase&logoColor=black">
<img src="https://img.shields.io/badge/Razorpay-Payments-0C2451?style=for-the-badge">
<img src="https://img.shields.io/badge/Offline-First-success?style=for-the-badge">

The official Android client for the **HealthX Healthcare Ecosystem**.

</div>

---

# 📖 Overview

HealthX Android is a comprehensive healthcare and wellness application that helps users manage every aspect of their daily health from a single, unified platform.

Built with a modern Android architecture, the application combines AI-powered nutrition tracking, intelligent reminders, secure medical document management, delegated health-data sharing, premium subscriptions, offline-first synchronization, and beautiful Jetpack Compose interfaces to provide a complete digital healthcare experience.

The application is designed around reliability, security, performance, and an exceptional user experience, ensuring that important healthcare features continue to function even without an internet connection.

---

# ✨ Core Features

- 🔐 Secure Authentication
- 👤 User Profile Management
- 🍏 AI Nutrition Analysis
- 📊 Daily Nutrition Tracking
- 🎯 Personalized Nutrition Goals
- 💧 Water Intake Tracking
- 💊 Medicine Reminders
- ⏰ Smart Reminder System
- 📅 Health Checkup Scheduling
- 📁 Secure Medical Documents
- 🌍 Public Document Sharing
- 🔒 Password Protected Documents
- 👥 Shared Health Records
- 🩺 QR Based Health Sharing
- 🤝 Delegated Access
- 🔔 Local Notifications
- 📲 Firebase Push Notifications
- 💳 Premium Subscriptions
- ⚙ Personalized Settings
- 🌙 Beautiful Material Design UI
- 🔄 Offline First Synchronization

---

# 🏗 Application Architecture

```text
                    HealthX Android
                           │
          Jetpack Compose UI + ViewModels
                           │
                Repository Layer
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
      Local Storage    REST APIs     Background Tasks
          │                │                │
          ▼                ▼                ▼
      Room Database   HealthX Backend   AlarmManager
                           │
                    Firebase Cloud Messaging
```

---

# 📱 Application Modules

## 🔐 Authentication

Secure account management for HealthX users.

### Features

- User Registration
- Secure Login
- Email Verification
- Password Reset
- Automatic Session Management
- JWT Authentication

---

## 🍏 AI Nutrition Tracking

An advanced nutrition system powered by AI that allows users to analyze meals using food images or manually log their nutrition.

### Features

- AI Food Recognition
- Camera Based Food Analysis
- Gallery Image Analysis
- Manual Meal Logging
- Daily Nutrition Dashboard
- Macronutrient Tracking
- Micronutrient Tracking
- Personalized Nutrition Goals
- Nutrition History
- Water Intake Tracking

### AI Workflow

```text
Capture Image
       │
       ▼
HealthX Backend
       │
       ▼
Google Gemini Vision
       │
       ▼
Nutrition Analysis
       │
       ▼
Meal Confirmation
       │
       ▼
Daily Nutrition Log
```

---

## 💊 Smart Reminder System

An offline-first reminder engine that continues working even when the application is closed.

### Reminder Categories

- Medication
- Supplements
- Water Intake
- Meals
- Exercise
- Sleep
- Vaccinations
- Health Checkups
- Therapy
- Recovery
- Elder Care
- Custom Reminders

### Features

- AlarmManager Scheduling
- Exact Alarm Support
- Recurring Reminders
- Snooze Support
- Reminder History
- Offline Scheduling
- Voice Reminders
- Notification Actions

---

## 📁 Secure Medical Documents

Store and organize important healthcare documents securely.

### Features

- Upload Medical Records
- View Documents
- Categorize Records
- Password Protection
- Public Share Links
- Share With Friends & Family
- Download Documents
- Secure Storage

---

## 🩺 Health Sharing

Securely share healthcare information with trusted HealthX users.

### Features

- QR Code Based Sharing
- Friend Connections
- Delegated Access
- Permission Management
- Temporary Access
- Health Record Sharing
- Privacy Controls
- Block Users

---

## 📲 Notification System

HealthX keeps users informed using both local notifications and cloud messaging.

### Features

- Reminder Notifications
- Medication Alerts
- Subscription Updates
- Health Tips
- System Announcements
- Document Sharing Notifications
- Reminder Completion Actions
- Firebase Cloud Messaging Integration

---

## 💳 Premium Membership

Unlock advanced HealthX features through premium subscriptions.

### Features

- Subscription Plans
- Razorpay Integration
- Secure Checkout
- Payment Verification
- Subscription Management
- Premium Feature Access

---

## ⚙ User Settings

Personalize the HealthX experience.

### Settings

- Profile Information
- Height
- Weight
- Gender
- Daily Goals
- Nutrition Preferences
- API Keys
- Theme
- Notification Preferences
- Language
- Country
- Units

---

# 🎨 User Experience

HealthX is designed with a strong focus on usability and accessibility.

### Highlights

- Jetpack Compose UI
- Material Design 3
- Smooth Animations
- Responsive Layouts
- Dark Theme Support
- Custom Icons
- Modern Navigation
- Offline Experience
- Beautiful Health Dashboards
- Consistent Design Language

---

# 🛠 Technology Stack

| Layer | Technology |
|--------|------------|
| Platform | Android |
| Language | Kotlin |
| UI Toolkit | Jetpack Compose |
| Architecture | MVVM |
| Local Database | Room |
| Networking | Retrofit |
| JSON | Gson / Moshi |
| Background Tasks | AlarmManager |
| Authentication | JWT |
| Push Notifications | Firebase Cloud Messaging |
| AI | Google Gemini (Backend) |
| Payments | Razorpay Android SDK |
| Image Loading | Coil |
| Async Programming | Kotlin Coroutines |

---

# 📁 Project Structure

```text
HealthX/

├── app/
│
├── ui/
│   ├── screens/
│   ├── components/
│   ├── navigation/
│   ├── theme/
│   └── animations/
│
├── viewmodels/
│
├── repositories/
│
├── network/
│
├── database/
│
├── models/
│
├── workers/
│
├── alarms/
│
├── notifications/
│
├── utils/
│
├── res/
│
├── AndroidManifest.xml
│
├── build.gradle
│
└── README.md
```

---

# ⚙ Installation

## 1. Clone the Repository

```bash
git clone https://github.com/E-techy/HealthX.git
```

---

## 2. Open in Android Studio

Open the project using the latest version of Android Studio.

Allow Gradle to synchronize all project dependencies.

---

## 3. Configure Firebase

Download your project's `google-services.json` file from Firebase Console.

Place it inside:

```text
app/google-services.json
```

---

## 4. Configure Razorpay

Add your Razorpay API key inside your secure configuration.

Example:

```properties
RAZORPAY_KEY=rzp_test_your_key
```

---

## 5. Configure Backend

Update the application's base API URL to point to your running HealthX Backend server.

Example:

```text
http://YOUR_LOCAL_IP:5001/api
```

---

## 6. Build & Run

Sync Gradle.

Run the application on either:

- Android Emulator
- Physical Android Device

---

# 📱 Using HealthX

## Create an Account

Register using your email address and verify your account using the OTP sent to your inbox.

---

## Complete Your Profile

Provide your personal health information to receive personalized recommendations.

---

## Track Nutrition

Capture meal images or manually enter foods to monitor calories, macronutrients, and daily nutrition.

---

## Manage Medical Records

Upload important healthcare documents and securely share them with trusted users whenever needed.

---

## Create Reminders

Schedule medication, hydration, workout, sleep, and health checkup reminders.

HealthX ensures reminders continue functioning even when the application is closed.

---

## Share Health Information

Generate or scan HealthX QR codes to securely share selected healthcare information with friends, family members, or caregivers.

---

## Upgrade to Premium

Browse available subscription plans and complete payments securely through Razorpay.

---

## Stay Updated

Receive instant notifications for reminders, subscription updates, document sharing events, and personalized health recommendations.

---

# 🔒 Permissions

HealthX requests only the permissions required for its functionality.

- Camera
- Storage / Photos
- Notifications
- Internet
- Network State
- Exact Alarm Permission
- Wake Lock
- Vibration

---

# 🚀 Design Principles

- Offline First
- AI Assisted
- Privacy Focused
- Secure By Default
- Modern Android Architecture
- Modular Design
- Responsive UI
- Material Design 3
- Accessibility Focused
- Performance Optimized

---

# ❤️ HealthX Android

HealthX Android is a modern healthcare application built using **Kotlin**, **Jetpack Compose**, **Room**, **Firebase Cloud Messaging**, **AlarmManager**, **Google Gemini**, and **Razorpay**.

It delivers AI-powered nutrition tracking, intelligent reminders, secure document management, delegated health-data sharing, premium subscriptions, offline synchronization, and a beautiful native Android experience for the complete HealthX ecosystem.