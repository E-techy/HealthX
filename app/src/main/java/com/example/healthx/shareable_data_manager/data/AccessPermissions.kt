package com.example.healthx.shareable_data_manager.data

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String
)

object AccessPermissions {
    val availablePermissions = listOf(
        PermissionItem(
            id = "SEE_NUTRITION",
            title = "View Nutrition Log",
            description = "Allows the user to view your daily meals and caloric intake."
        ),
        PermissionItem(
            id = "ADD_NUTRITION",
            title = "Add Nutrition Data",
            description = "Allows the user to log meals on your behalf."
        ),
        PermissionItem(
            id = "SEE_GOALS",
            title = "View Health Goals",
            description = "Allows the user to track your progress on health goals."
        ),
        PermissionItem(
            id = "SET_GOALS",
            title = "Create Health Goals",
            description = "Allows the user to assign new fitness or diet goals to you."
        )
    )
}