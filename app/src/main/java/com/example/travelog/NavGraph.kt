package com.example.travelog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(BottomNavItem.Map.route) {
            MapScreen()
        }
        composable(BottomNavItem.Archive.route) {
            ArchiveScreen(
                navController=navController,
                onGoPlannedTrips = { navController.navigate("plans") }
            )
        }
        composable(BottomNavItem.Schedule.route) {
            ScheduleScreen()
        }
        composable(BottomNavItem.MyPage.route) {
            MyPageScreen()
        }
        composable("weather") {
            WeatherScreen()
        }
        composable("checklist") {
            ChecklistScreen()
        }
        composable("plans") {
            TravelApp()
        }
//        composable("budget") {
//            TripBudgetScreen()
//        }
    }
}