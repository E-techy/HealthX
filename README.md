# 🩺 HealthX (Android)

HealthX is a comprehensive health and wellness application designed to help users monitor their daily habits, track their nutrition, and manage premium health services.

This repository contains the Android client application, featuring a highly customized user interface, reliable local scheduling, and secure payment integrations.

---

# 📚 Table of Contents

* [Key Features](#-key-features)
* [Tech Stack](#-tech-stack)
* [Installation and Setup](#️-installation-and-setup)
* [Usage Guide](#-usage-guide)

---

# 🚀 Key Features

## Advanced Nutrition Tracking

A robust module that allows users to log and monitor their daily dietary intake. It provides detailed insights into caloric consumption, macronutrient distribution, and overall nutritional balance to help users hit their specific health goals.

---

## Smart Reminders & Scheduling

Utilizing Android's native `AlarmManager`, the app schedules precise local alerts. This ensures users never miss a medication dose, water intake reminder, or planned workout routine, even if the app is closed or the device is asleep.

---

## Premium Subscriptions

Users can upgrade to access exclusive features and personalized health plans.

The subscription lifecycle is managed seamlessly and securely using the Razorpay API, handling diverse payment methods and recurring billing.

---

## Real-Time Push Notifications

Integrated with Firebase Cloud Messaging (FCM) to deliver critical updates directly to the user's device.

This includes subscription status changes, personalized daily health tips, and system alerts.

---

## Custom Aesthetic

The app features a polished, professional visual identity, complete with bespoke app icons and a cohesive design language tailored for an optimal, distraction-free user experience.

---

# 🛠 Tech Stack

| Component          | Technology / Service |
| ------------------ | -------------------- |
| Platform           | Android              |
| Payment Gateway    | Razorpay Android SDK |
| Push Notifications | Firebase (FCM)       |
| Background Tasks   | AlarmManager API     |

---

# ⚙️ Installation and Setup

To run the HealthX Android app locally for development and testing:

## 1. Clone the Repository

```bash
git clone https://github.com/E-techy/HealthX.git
```

---

## 2. Open in Android Studio

Launch Android Studio and select **Open an existing project**, then navigate to the cloned directory.

---

## 3. Configure Firebase

* Go to the Firebase Console and create/select your project.
* Download the `google-services.json` file.
* Place the `google-services.json` file inside the `app/` directory of the project.

---

## 4. Configure Razorpay

Locate the `gradle.properties` or your secure keys file.

Add your Razorpay test API key:

```properties
RAZORPAY_KEY="rzp_test_your_key_here"
```

---

## 5. Build and Run

Sync the Gradle files and click the **Run** button to launch the app on an emulator or a connected physical device.

---

# 📱 Usage Guide

## Onboarding

Upon first launch, users will be greeted with the custom app icon and a streamlined onboarding flow.

---

## Logging Nutrition

Navigate to the **Nutrition** tab to log meals, scan items, or view daily macros.

---

## Setting Alarms

Access the **Reminders** section to configure daily alerts using the custom AlarmManager setup.

---

## Upgrading

Users can visit their **Profile** to view subscription tiers.

Selecting a plan will invoke the Razorpay checkout overlay.

---

## Permissions

The app will prompt for **Notification** permissions (Android 13+) to ensure FCM and local alarms function correctly.
