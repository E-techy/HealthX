package com.example.healthx.notification_manager

/**
 * Defines all top-level notification categories processed by the HealthX app.
 */
enum class NotificationCategory {
    OTP,
    NEW_DEVICE_REGISTERED,
    NEW_AI_CHAT_RECEIVED,
    ADVERTISEMENT,
    REMINDER,
    SUBSCRIPTION,
    PAYMENT,
    DATA_SYNCING,
    DOWNLOADING_MEDIA,
    FEEDBACK_REVIEW,
    ACCOUNT_DELETION,
    ACCOUNT_LOGGED_OUT
}

/**
 * Defines all specific subtypes for the REMINDER notification category.
 * These map exactly to the Mongoose discriminators in the backend schema.
 */
enum class ReminderCategory {
    MEDICATION,
    SUPPLEMENT,
    REFILL,
    CONSULTATION,
    CHECKUP,
    LAB_TEST,
    THERAPY,
    VACCINATION,
    HYDRATION,
    NUTRITION,
    SLEEP,
    FITNESS,
    MINDFULNESS,
    HABIT,
    VITALS,
    SYMPTOM,
    CYCLE,
    MATERNITY,
    ELDER_CARE,
    RECOVERY,
    CUSTOM
}