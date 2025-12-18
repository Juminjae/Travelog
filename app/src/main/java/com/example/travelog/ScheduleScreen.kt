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
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import androidx.compose.ui.text.style.TextDecoration
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

data class TodoItem(
    val id: Int,
    val text: String,
    val isDone: Boolean = false
)

@Composable
fun ScheduleScreen(
    tripViewModel: TripViewModel = run {
        val owner = (LocalContext.current as ComponentActivity)
        viewModel(viewModelStoreOwner = owner)
    }
) {
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
            onAddTrip = {
                val trip = tripViewModel.tripPlan
                if (trip != null) {
                    // 여행 시작일로 날짜 이동
                    selectedDate = trip.startDate
                    currentMonth = YearMonth.from(trip.startDate)
                } }
        )
        Spacer(Modifier.height(12.dp))
        WeekHeader()
        Spacer(Modifier.height(8.dp))
        CalendarGrid(
            yearMonth = currentMonth,
            selectedDate = selectedDate,
            tripViewModel = tripViewModel,
            onDateClick = { selectedDate = it }
        )
        Spacer(Modifier.height(12.dp))
        TodayScheduleSection(
            selectedDate = selectedDate,
            tripViewModel = tripViewModel
        )
        Spacer(Modifier.height(20.dp))
        TodoSection()
        Spacer(Modifier.height(20.dp))
    }
}
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
   CALENDAR GRID (여행 기간 점 표시)
================================ */

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    tripViewModel: TripViewModel,
    onDateClick: (LocalDate) -> Unit
) {
    val trip = tripViewModel.tripPlan
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
            val showDot =
                date != null &&
                        trip != null &&
                        !date.isBefore(trip.startDate) &&
                        !date.isAfter(trip.endDate)

            CalendarDayCell(
                date = date,
                isSelected = date == selectedDate,
                showDot = showDot,
                onClick = { if (date != null) onDateClick(date) }
            )
        }
    }
}
@Composable
fun CalendarDayCell(
    date: LocalDate?,
    isSelected: Boolean,
    showDot: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(enabled = date != null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                // 여행 기간 점 표시
                if (showDot) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(Color.Black, CircleShape)
                    )
                }
            }
        }
    }
}
@Composable
fun TodayScheduleSection(
    selectedDate: LocalDate,
    tripViewModel: TripViewModel
) {
    val trip = tripViewModel.tripPlan

    val dayIndex = remember(selectedDate, trip) {
        if (trip == null) return@remember null
        val diff =
            ChronoUnit.DAYS.between(trip.startDate, selectedDate).toInt() + 1
        if (diff in 1..tripViewModel.totalDays()) diff else null
    }

    val places = dayIndex?.let { tripViewModel.dayPlaces[it] } ?: emptyList()

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
                .padding(16.dp)
        ) {
            when {
                trip == null ->
                    Text("여행이 등록되지 않았습니다.", color = Color.Gray)
                dayIndex == null ->
                    Text("여행 일정이 없는 날짜입니다.", color = Color.Gray)
                places.isEmpty() ->
                    Text("이 날의 여행 일정이 없습니다.", color = Color.Gray)
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Day $dayIndex 일정", fontWeight = FontWeight.SemiBold)
                        places.forEach { place ->
                            Text("• ${place.title}", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun TodoSection() {
    var todoList by remember { mutableStateOf(listOf<TodoItem>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("To-Do List", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text("✅")
            Spacer(Modifier.weight(1f))

            IconButton(onClick = { showAddDialog = true }) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            if (todoList.isEmpty()) {
                Text(
                    "할 일이 없습니다.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column {
                    todoList.forEach { todo ->
                        TodoRow(
                            todo = todo,
                            onCheckedChange = { checked ->
                                todoList = todoList.map {
                                    if (it.id == todo.id) it.copy(isDone = checked)
                                    else it
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            todoList = todoList + TodoItem(
                                id = todoList.size + 1,
                                text = inputText
                            )
                            inputText = ""
                            showAddDialog = false
                        }
                    }
                ) { Text("추가") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("취소") }
            },
            title = { Text("할 일 추가") },
            text = {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("할 일을 입력하세요") },
                    singleLine = true
                )
            }
        )
    }
}
@Composable
fun TodoRow(
    todo: TodoItem,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Checkbox(
            checked = todo.isDone,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = todo.text,
            fontSize = 14.sp,
            color = if (todo.isDone) Color.Gray else Color.Black,
            textDecoration =
                if (todo.isDone) TextDecoration.LineThrough
                else TextDecoration.None
        )
    }
}