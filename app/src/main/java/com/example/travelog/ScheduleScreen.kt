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
        Spacer(Modifier.height(30.dp))
        Text("캘린더", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        CalendarHeader(
            currentMonth = currentMonth,
            onPrev = { currentMonth = currentMonth.minusMonths(1) },
            onNext = { currentMonth = currentMonth.plusMonths(1) },
            onAddTrip = { /* 여행 등록 로직 추가 */ }
        )
        Spacer(Modifier.height(12.dp))
        WeekHeader()
        Spacer(Modifier.height(8.dp))
        CalendarGrid(
            yearMonth = currentMonth,
            selectedDate = selectedDate,
            onDateClick = { selectedDate = it }
        )
    }
}

/* 상단 헤더 (월 + 방향키) */
@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onAddTrip: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPrev,
            modifier = Modifier.size(32.dp)
        ) {
            Text("<", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(32.dp)
        ) {
            Text(
                ">",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onAddTrip,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                "여행 등록",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
/* 요일 헤더 (일요일 시작) */
@Composable
fun WeekHeader() {
    val days = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Text(
                text = when (day) {
                    DayOfWeek.SUNDAY -> "일"
                    DayOfWeek.MONDAY -> "월"
                    DayOfWeek.TUESDAY -> "화"
                    DayOfWeek.WEDNESDAY -> "수"
                    DayOfWeek.THURSDAY -> "목"
                    DayOfWeek.FRIDAY -> "금"
                    DayOfWeek.SATURDAY -> "토"
                },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = when (day) {
                    DayOfWeek.SUNDAY -> Color.Red
                    DayOfWeek.SATURDAY -> Color.Blue
                    else -> Color.Black
                }
            )
        }
    }
}

/* 캘린더 그리드 (6주 고정) */
@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    //일요일 시작 보정
    val startOffset = firstDayOfMonth.dayOfWeek.value % 7
    val dates = buildList<LocalDate?> {
        repeat(startOffset) { add(null) }
        for (day in 1..daysInMonth) {
            add(yearMonth.atDay(day))
        }
        while (size < 42) add(null) // 6주 고정
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
/* 날짜 셀 */
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