package com.example.travelog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    cityList: List<String> = listOf("빈", "런던", "삿포로")
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCity by remember { mutableStateOf(cityList.first()) }

    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
      //이전 여행지 드롭박스
        ExposedDropdownMenuBox (
            expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth(122f/402f)
                .padding(5.dp)
        ) {
          Text(
              text = selectedCity,
              modifier = Modifier
                  .menuAnchor() //앵커 표시 다시 확인하기.....
                  .padding(5.dp),
              fontSize = 18.sp
          )

            ExposedDropdownMenu(
              expanded = expanded,
              onDismissRequest = { expanded = false }
          ) {
              cityList.forEach { city -> DropdownMenuItem( text = { Text(city) },
                  onClick = {
                      selectedCity = city
                      expanded = false
                  }
              ) }
          }
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