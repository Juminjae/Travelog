package com.example.travelog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ArchiveScreen() {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
      Box (
          modifier = Modifier
              .fillMaxWidth(122f/402f)
              .padding(5.dp)
      ) {
        Text( text = "지난 여행 선택 ▼", fontSize = 18.sp, color = Color(0xFF000000))
      }

      Box (
          modifier = Modifier
              .fillMaxSize()
              .padding(
                  start = 5.dp,
                  end = 5.dp,
                  bottom = 72.dp
              )
      ){
          Text( text = "그리드 영역 (임시)", color = Color(0xFF000000))
      }
    }
}