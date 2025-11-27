package com.example.travelog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomBar(
    currentRoute: String?,                     // ✅ 현재 NavHost의 route
    onItemSelected: (BottomNavItem) -> Unit   // ✅ 눌렀을 때 이동 처리
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Map,
        BottomNavItem.Schedule,
        BottomNavItem.Archive,
        BottomNavItem.MyPage
    )

    val effectiveRoute = when (currentRoute) {
        "weather" -> BottomNavItem.Home.route   // 날씨 화면 = 홈 탭
        "checklist" -> BottomNavItem.Home.route  // 체크리스트 화면 = 맵 탭
        else -> currentRoute
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
            .background(Color.White)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val selected = item.route == effectiveRoute   // ✅ route로 선택 여부 판단
            val interactionSource = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        onItemSelected(item)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = if (selected) item.selectedIcon() else item.unselectedIcon(),
                    contentDescription = item.label,
                    tint = Color.Unspecified,  // 아이콘 원본 색 그대로 쓰고 싶으면
                    modifier = Modifier.size(35.dp)
                )
            }
        }
    }
}