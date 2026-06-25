package com.example.healthx.ui.screens.reminders

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.healthx.ui.screens.reminders.forms.*

@Composable
fun RemindersNavGraph(navController: NavHostController = rememberNavController()) {

    NavHost(navController = navController, startDestination = "home") {

        // --- LEVEL 1: Main Home Screen ---
        composable("home") {
            RemindersHomeScreen(
                onCategoryClick = { categoryId ->
                    // If it's custom, go straight to the form. Otherwise, open the sub-menu.
                    if (categoryId == "custom") {
                        navController.navigate("form_custom")
                    } else {
                        navController.navigate("subcategory/$categoryId")
                    }
                }
            )
        }

        // --- LEVEL 2: Sub-Category Menu ---
        composable(
            route = "subcategory/{categoryId}",
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: "pharmacy"
            SubCategoryScreen(
                categoryId = categoryId,
                onBack = { navController.popBackStack() },
                onNavigateToForm = { formRoute -> navController.navigate(formRoute) }
            )
        }

        // --- LEVEL 3: The Specific Forms ---
        val popBack = { navController.popBackStack() }

        // Pharmacy Domain
        composable("form_medication") { MedicationReminderScreen(onBack = { popBack() }) }
        composable("form_supplement") { SupplementReminderScreen(onBack = { popBack() }) }
        composable("form_refill") { RefillReminderScreen(onBack = { popBack() }) }

        // Visits & Diagnostics Domain
        composable("form_consultation") { ConsultationReminderScreen(onBack = { popBack() }) }
        composable("form_checkup") { CheckupReminderScreen(onBack = { popBack() }) }
        composable("form_lab") { LabTestReminderScreen(onBack = { popBack() }) }
        composable("form_therapy") { TherapyReminderScreen(onBack = { popBack() }) }
        composable("form_vaccine") { VaccinationReminderScreen(onBack = { popBack() }) }

        // Wellness Domain
        composable("form_hydration") { HydrationReminderScreen(onBack = { popBack() }) }
        composable("form_nutrition") { NutritionReminderScreen(onBack = { popBack() }) }
        composable("form_sleep") { SleepReminderScreen(onBack = { popBack() }) }
        composable("form_fitness") { FitnessReminderScreen(onBack = { popBack() }) }
        composable("form_mindfulness") { MindfulnessReminderScreen(onBack = { popBack() }) }
        composable("form_habit") { HabitReminderScreen(onBack = { popBack() }) }

        // Tracking Domain
        composable("form_vitals") { VitalsReminderScreen(onBack = { popBack() }) }
        composable("form_symptom") { SymptomReminderScreen(onBack = { popBack() }) }
        composable("form_cycle") { CycleReminderScreen(onBack = { popBack() }) }

        // Care Domain
        composable("form_maternity") { MaternityReminderScreen(onBack = { popBack() }) }
        composable("form_elder") { ElderCareReminderScreen(onBack = { popBack() }) }
        composable("form_recovery") { RecoveryReminderScreen(onBack = { popBack() }) }

        // Custom Domain
        composable("form_custom") { CustomReminderScreen(onBack = { popBack() }) }
    }
}