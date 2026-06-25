package com.example.healthx.data.models.reminders.wellness

import com.example.healthx.data.models.reminders.core.*
import java.util.UUID

// 9. Hydration Reminders
data class HydrationReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "HYDRATION",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val targetVolumeMl: Int,
    val currentVolumeMl: Int = 0,
    val containerSizeMl: Int, // e.g., 250ml glass, 500ml bottle
    val beverageType: String = "Water", // e.g., "Water", "Electrolytes", "Green Tea"
    val isAutoLogOnAcknowledge: Boolean = true // If true, dismissing the alarm automatically adds 'containerSizeMl' to daily total
) : BaseReminder

// 10. Nutrition & Diet Reminders
data class NutritionReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "NUTRITION",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val mealType: String, // e.g., "Breakfast", "Pre-workout Snack", "Fasting Window Starts"
    val targetCalories: Int? = null,
    val targetProteinGrams: Int? = null,
    val targetCarbsGrams: Int? = null,
    val targetFatsGrams: Int? = null,

    val dietaryRestriction: String? = null, // e.g., "Keto", "Vegan", "Low Sodium"
    val promptFoodLog: Boolean = true // UI will prompt user to snap a photo of their meal
) : BaseReminder

// 11. Sleep & Rest Reminders
data class SleepReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "SLEEP",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val windDownTimeStart: Long, // Epoch time to prompt "Screen off"
    val targetWakeTime: Long,
    val targetSleepDurationMinutes: Int,

    val ambientAudioUrl: String? = null, // Cloud URL for white noise/rain sounds
    val promptDreamLogOnWake: Boolean = false,
    val promptSleepQualityLog: Boolean = true // 1-5 star rating prompt in the morning
) : BaseReminder

// 12. Fitness & Activity Reminders
data class FitnessReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "FITNESS",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val workoutType: String, // e.g., "Cardio", "Yoga", "Weightlifting"
    val targetDurationMinutes: Int,
    val intensityLevel: String? = null, // "Low", "Moderate", "HIIT"
    val location: String? = null, // "Home", "Gym", "Park"

    val targetCaloriesBurn: Int? = null,
    val referenceVideoUrl: String? = null, // YouTube link for form check
    val attachedRoutineDocument: DocumentAttachment? = null // PDF of workout plan
) : BaseReminder

// 13. Mindfulness & Mental Health Reminders
data class MindfulnessReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "MINDFULNESS",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val practiceType: String, // e.g., "Meditation", "4-7-8 Breathing", "Journaling"
    val targetDurationMinutes: Int,
    val guidedAudioUrl: String? = null, // Cloud URL for guided meditation voice

    val promptPrePracticeMood: Boolean = true,
    val promptPostPracticeMood: Boolean = true
) : BaseReminder

// 14. Habit Building Reminders
data class HabitReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "HABIT",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val habitName: String, // e.g., "Read 10 pages", "Floss"
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,

    val cueContext: String? = null, // e.g., "Immediately after brushing teeth"
    val rewardContext: String? = null, // e.g., "Then I get to drink coffee"

    val accountabilityPartnerEmail: String? = null // To send auto-emails if streak breaks
) : BaseReminder