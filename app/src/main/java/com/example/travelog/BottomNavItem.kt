package com.example.travelog

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: @Composable () -> Painter,
    val unselectedIcon: @Composable () -> Painter
) {
    object Home : BottomNavItem(
        route = "home",
        label = "홈",
        selectedIcon = { painterResource(id = R.drawable.icon_home_selected) },
        unselectedIcon = { painterResource(id = R.drawable.icon_home_unselected) }
    )

    object Map : BottomNavItem(
        route = "map",
        label = "지도",
        selectedIcon = { painterResource(id = R.drawable.icon_map_selected) },
        unselectedIcon = { painterResource(id = R.drawable.icon_map_unselected) }
    )

    object Archive : BottomNavItem(
        route = "archive",
        label = "아카이브",
        selectedIcon = { painterResource(id = R.drawable.icon_archive_selected) },
        unselectedIcon = { painterResource(id = R.drawable.icon_archive_unselected) }
    )

    object Schedule : BottomNavItem(
        route = "schedule",
        label = "일정",
        selectedIcon = { painterResource(id = R.drawable.icon_calendar_selected) },
        unselectedIcon = { painterResource(id = R.drawable.icon_calendar_unselected) }
    )

    object MyPage : BottomNavItem(
        route = "mypage",
        label = "마이",
        selectedIcon = { painterResource(id = R.drawable.icon_mypage_selected) },
        unselectedIcon = { painterResource(id = R.drawable.icon_mypage_unselected) }
    )

    companion object {
        val items: List<BottomNavItem> = listOf(
            Home,
            Map,
            Schedule,
            Archive,
            MyPage
        )
    }
}