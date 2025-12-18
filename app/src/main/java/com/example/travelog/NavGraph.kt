package com.example.travelog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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
        modifier = modifier,
        route = "main"
    ) {
        composable(BottomNavItem.Home.route) {
            val parentEntry = remember(navController) {
                navController.getBackStackEntry("main")
            }
            val tripsVm: TripsViewModel = viewModel(parentEntry)

            HomeScreen(
                navController = navController,
                tripsVm = tripsVm)
        }
        composable(BottomNavItem.Map.route) {
            MapScreen()
        }
        composable(BottomNavItem.Archive.route) {
            ArchiveScreen(
                navController = navController,
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
            val parentEntry = remember(navController) {
                navController.getBackStackEntry("main")
            }
            val tripsVm: TripsViewModel = viewModel(parentEntry)

            WeatherScreen(tripsVm = tripsVm)
        }
        composable("checklist") {
            ChecklistScreen()
        }
        composable("plans") {
            val parentEntry = remember(navController) {
                navController.getBackStackEntry("main")
            }
            val tripsVm: TripsViewModel = viewModel(parentEntry)

            TravelApp(vm = tripsVm)
        }
    }
}