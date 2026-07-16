package com.example.healthx.shareable_data_manager.data

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String
)

object AccessPermissions {
    val availablePermissions = listOf(
        // ----------------------------------------------------
        // NUTRITION & MEALS
        // ----------------------------------------------------
        PermissionItem(
            id = "SEE_NUTRITION",
            title = "View Nutrition Logs",
            description = "Allows the user to view your logged meals, food images, and daily calorie/nutrient intake."
        ),
        PermissionItem(
            id = "EDIT_NUTRITION",
            title = "Log & Edit Nutrition",
            description = "Allows the user to analyze food images and log new meals on your behalf."
        ),

        // ----------------------------------------------------
        // GOALS
        // ----------------------------------------------------
        PermissionItem(
            id = "SEE_GOALS",
            title = "View Nutrition Goals",
            description = "Allows the user to see your active and past nutrition targets and progress."
        ),
        PermissionItem(
            id = "EDIT_GOALS",
            title = "Manage Goals",
            description = "Allows the user to create, modify, or complete your nutrition goals."
        ),

        // ----------------------------------------------------
        // REMINDERS
        // ----------------------------------------------------
        PermissionItem(
            id = "SEE_REMINDERS",
            title = "View Reminders",
            description = "Allows the user to view your scheduled health, hydration, and medication reminders."
        ),
        PermissionItem(
            id = "EDIT_REMINDERS",
            title = "Manage Reminders",
            description = "Allows the user to create, update, sync, and delete your reminders."
        ),

        // ----------------------------------------------------
        // SETTINGS
        // ----------------------------------------------------
        PermissionItem(
            id = "SEE_SETTINGS",
            title = "View Profile Settings",
            description = "Allows the user to view your app preferences and basic profile settings."
        ),
        PermissionItem(
            id = "EDIT_SETTINGS",
            title = "Edit Settings",
            description = "Allows the user to modify your app preferences and profile configurations."
        ),

        // ----------------------------------------------------
        // SUBSCRIPTIONS (READ-ONLY)
        // ----------------------------------------------------
        PermissionItem(
            id = "SEE_SUBSCRIPTION",
            title = "View Subscription Status",
            description = "Allows the user to see if you are on a FREE, PRO, or ULTRA tier."
        ),

        // ----------------------------------------------------
        // WILDCARD (MASTER ACCESS)
        // ----------------------------------------------------
        PermissionItem(
            id = "ALL",
            title = "Full Access",
            description = "Grants complete view and edit access to all supported health and settings data."
        )
    )
}