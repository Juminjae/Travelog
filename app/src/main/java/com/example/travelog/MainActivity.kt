package com.example.travelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.travelog.ui.theme.TravelogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TravelogTheme {

                // ✅ 초기값이 Home 이라서 절대 null 아님
                var selectedItem by remember {
                    mutableStateOf<BottomNavItem>(BottomNavItem.Home)
                }

                Scaffold(
                    containerColor = Color.White,
                    bottomBar = {
                        BottomBar(
                            selectedItem = selectedItem,
                            onItemSelected = { item ->
                                selectedItem = item
                            }
                        )
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        color = Color.White
                    ) {
                        when (selectedItem) {
                            BottomNavItem.Home -> HomeScreen()
                            BottomNavItem.Map -> MapScreen()
                            BottomNavItem.Archive -> ArchiveScreen()
                            BottomNavItem.Schedule -> ScheduleScreen()
                            BottomNavItem.MyPage -> MyPageScreen()
                        }
                    }
                }
            }
        }
    }
}