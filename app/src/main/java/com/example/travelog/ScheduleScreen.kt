package com.example.travelog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ScheduleScreen() {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("캘린더", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        CalendarHeader(
            currentMonth = currentMonth,
            onPrev = { currentMonth = currentMonth.minusMonths(1) },
            onNext = { currentMonth = currentMonth.plusMonths(1) },
            onAddTrip = { }
        )
        Spacer(Modifier.height(12.dp))
        WeekHeader()
        Spacer(Modifier.height(8.dp))
        CalendarGrid(
            yearMonth = currentMonth,
            selectedDate = selectedDate,
            onDateClick = { selectedDate = it }
        )
        Spacer(Modifier.height(12.dp))
        TodayScheduleSection()
        Spacer(Modifier.height(20.dp))
        TodoSection()
        Spacer(Modifier.height(20.dp))
    }
}

/* 캘린더 헤더 */

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onAddTrip: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
            Text("<", fontWeight = FontWeight.Bold)
        }
        Text(
            text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
            Text(">", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = onAddTrip,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("여행 등록", fontSize = 12.sp)
        }
    }
}

/* ===============================
   요일 헤더
================================ */

@Composable
fun WeekHeader() {
    val days = listOf("일", "월", "화", "수", "목", "금", "토")
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEachIndexed { index, day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = when (index) {
                    0 -> Color.Red
                    6 -> Color.Blue
                    else -> Color.Black
                }
            )
        }
    }
}

/* ===============================
   캘린더 그리드
================================ */

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startOffset = firstDayOfMonth.dayOfWeek.value % 7
    val dates = buildList<LocalDate?> {
        repeat(startOffset) { add(null) }
        for (day in 1..daysInMonth) add(yearMonth.atDay(day))
        while (size < 42) add(null)
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        items(dates) { date ->
            CalendarDayCell(
                date = date,
                isSelected = date == selectedDate,
                onClick = { if (date != null) onDateClick(date) }
            )
        }
    }
}
@Composable
fun CalendarDayCell(
    date: LocalDate?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(enabled = date != null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (isSelected) Color(0xFFE5E5E5)
                        else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 14.sp,
                    color = when (date.dayOfWeek) {
                        DayOfWeek.SUNDAY -> Color.Red
                        DayOfWeek.SATURDAY -> Color.Blue
                        else -> Color.Black
                    }
                )
            }
        }
    }
}
/* 오늘 일정 섹션 */
@Composable
fun TodayScheduleSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("오늘 일정", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text("🗓️")
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(24.dp))
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("일정이 없습니다.", color = Color.Gray, fontSize = 14.sp)
        }
    }
}
/* To-Do List 섹션 */
@Composable
fun TodoSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("To-Do List", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text("✅")
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(24.dp))
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("일정이 없습니다.", color = Color.Gray, fontSize = 14.sp)
        }
    }
}