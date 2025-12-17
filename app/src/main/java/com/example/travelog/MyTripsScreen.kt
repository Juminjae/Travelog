package com.example.travelog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import androidx.navigation.compose.rememberNavController

// ------------------------------
// 모델 (위 코드 유지)
// ------------------------------
data class Trip(
    val id: String,
    val countryEmoji: String,
    val country: String,
    val targetDateMillis: Long,
    val members: List<String>,
    val coverColor: Color = Color.White
)

fun emojiForCountry(input: String): String {
    val s = input.trim().lowercase()
    return when {
        listOf("일본", "japan", "jp", "도쿄", "오사카", "삿포로").any { s.contains(it) } -> "🇯🇵"
        listOf("영국", "uk", "united kingdom", "런던", "london").any { s.contains(it) } -> "🇬🇧"
        listOf("미국", "usa", "united states", "la", "ny", "new york").any { s.contains(it) } -> "🇺🇸"
        listOf("프랑스", "france", "파리", "paris").any { s.contains(it) } -> "🇫🇷"
        listOf("독일", "germany", "베를린", "berlin").any { s.contains(it) } -> "🇩🇪"
        else -> "🏳️"
    }
}

fun coverResForCountry(input: String): Int? {
    val s = input.trim().lowercase()
    return when {
        listOf("일본", "japan", "jp", "도쿄", "오사카", "삿포로").any { s.contains(it) } -> R.drawable.sapporo
        listOf("영국", "uk", "united kingdom", "런던", "london").any { s.contains(it) } -> R.drawable.london
        else -> null
    }
}

// ------------------------------
// ✅ TravelApp: (아래 코드 로직 적용) vm + rememberSaveable 라우팅
// ------------------------------
@Composable
fun TravelApp(vm: TripsViewModel = viewModel()) {
    MaterialTheme(colorScheme = lightColorScheme()) {

        var route by rememberSaveable { mutableStateOf("list") }
        var selectedTripId by rememberSaveable { mutableStateOf<String?>(null) }

        when (route) {
            "list" -> MyTripsScreen(
                trips = vm.trips,   // ✅ 위 코드의 trips state 대신 vm.trips 사용
                onGoBudget = { trip ->
                    selectedTripId = trip.id
                    route = "budget"
                },
                onChangeDate = { tripId, newMillis ->
                    vm.updateDate(tripId, newMillis)
                },
                onAddMember = { tripId, name ->
                    vm.addMember(tripId, name)
                },
                onCreateTrip = { country, dateMillis ->
                    vm.addTrip(country, dateMillis) // ✅ “만들기(+버튼)”에서만 vm 적용 핵심
                }
            )

            "archive" -> {
                ArchiveScreen(
                    navController = navController,
                    cityList = listOf("빈", "런던", "삿포로"),
                    onGoPlannedTrips = { route = "list" }
                )
            }

            "budget" -> {
                val selectedTrip = vm.findTrip(selectedTripId)
                if (selectedTrip == null) {
                    route = "list"
                } else {
                    TripBudgetScreen(
                        tripTitle = selectedTrip.country,
                        onBack = { route = "list" },
                        tripId = selectedTrip.id,
                        vm = vm
                    )
                }
            }
        }
    }
}

// ------------------------------
// 화면 1: 내 여행 리스트 (+ 버튼으로 여행 생성)
// ✅ UI 템플릿은 “맨 위 코드” 그대로 유지
// ------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTripsScreen(
    trips: List<Trip>,
    onGoArchive: () -> Unit,
    onGoBudget: (Trip) -> Unit,
    onChangeDate: (String, Long) -> Unit,
    onAddMember: (String, String) -> Unit,
    onCreateTrip: (country: String, dateMillis: Long) -> String,
) {
    var showCreateTrip by remember { mutableStateOf(false) }

    var pendingAddMemberTripId by remember { mutableStateOf<String?>(null) }
    var showAddMemberPopup by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }

    val zone = ZoneId.systemDefault()
    val todayMillis = remember { LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli() }

    var createCountry by remember { mutableStateOf("") }
    var createDateMillis by remember { mutableStateOf(todayMillis) }
    var showCreateDatePicker by remember { mutableStateOf(false) }

    val createDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = createDateMillis
    )

// createDateMillis가 바뀌면 DatePickerState도 따라가게
    LaunchedEffect(createDateMillis) {
        createDatePickerState.selectedDateMillis = createDateMillis
    }

    if (showCreateDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showCreateDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    createDatePickerState.selectedDateMillis?.let { createDateMillis = it }
                    showCreateDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showCreateDatePicker = false }) { Text("취소") } }
        ) { DatePicker(state = createDatePickerState) }
    }

    val createDateLabel = remember(createDateMillis) {
        val d = Instant.ofEpochMilli(createDateMillis).atZone(zone).toLocalDate()
        "%04d.%02d.%02d".format(d.year, d.monthValue, d.dayOfMonth)
    }

    if (showCreateTrip) {
        AlertDialog(
            onDismissRequest = { showCreateTrip = false },
            title = { Text("여행 추가") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("날짜 선택", fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { showCreateDatePicker = true }) { Text(createDateLabel) }

                    Text("나라/도시 입력", fontWeight = FontWeight.SemiBold)
                    TextField(
                        value = createCountry,
                        onValueChange = { createCountry = it },
                        singleLine = true,
                        placeholder = { Text("예: 삿포로 / 런던 / 일본") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val country = createCountry.trim()
                    if (country.isNotEmpty()) {
                        // ✅ 여기서만 “아래 코드(vm)” 방식으로 실제 저장
                        val newId = onCreateTrip(country, createDateMillis)
                        pendingAddMemberTripId = newId
                        showAddMemberPopup = true

                        createCountry = ""
                        showCreateTrip = false
                    }
                }) { Text("만들기") }
            },
            dismissButton = {
                TextButton(onClick = {
                    createCountry = ""
                    showCreateTrip = false
                }) { Text("취소") }
            }
        )
    }

    if (showAddMemberPopup) {
        AlertDialog(
            onDismissRequest = {
                showAddMemberPopup = false
                pendingAddMemberTripId = null
                newMemberName = ""
            },
            title = { Text("사람 추가") },
            text = {
                TextField(
                    value = newMemberName,
                    onValueChange = { newMemberName = it },
                    singleLine = true,
                    placeholder = { Text("이름 입력") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = pendingAddMemberTripId
                    val name = newMemberName.trim()
                    if (id != null && name.isNotEmpty()) {
                        onAddMember(id, name)
                        newMemberName = ""
                    }
                }) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddMemberPopup = false
                    pendingAddMemberTripId = null
                    newMemberName = ""
                }) { Text("닫기") }
            }
        )
    }

    // ✅ 위 코드 템플릿(상단 Row + 아이콘 + TabRowLike + 리스트 + 맨 아래 + 카드) 유지
    Scaffold( containerColor = Color.White ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(horizontal = 0.dp, vertical = 0.dp)
                .fillMaxSize()
        ) {

            // (위 코드에 있던 상단 Row 1개만 남김 — 중복이었음)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "내 여행",
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(181.dp))

                Icon(
                    painter = painterResource(id = R.drawable.icon_bookmark),
                    contentDescription = "저장",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(56.dp)
                        .padding(10.dp)
                )

                Icon(
                    painter = painterResource(id = R.drawable.icon_notification),
                    contentDescription = "알림",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(56.dp)
                        .padding(10.dp)
                )
            }

            TabRowLike(
                tabs = listOf("예정된 여행", "지난 여행"),
                selected = 0,
                onSelect = { index ->
                    if (index == 1) {
                        onGoArchive()
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(trips) { trip ->
                    TripCard(
                        trip = trip,
                        onGoBudget = { onGoBudget(trip) },
                        onChangeDate = { millis -> onChangeDate(trip.id, millis) },
                        onAddMember = { name -> onAddMember(trip.id, name) }
                    )
                }

                item { AddTripCard(onClick = { showCreateTrip = true }) }
            }
        }
    }
}

@Composable
private fun TabRowLike(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val active = index == selected
            Text(
                text = title,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .padding(vertical = 8.dp)
                    .clickable{ onSelect(index) },
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                color = if (active) Color.Black else Color(0xFF777777)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripCard(
    trip: Trip,
    onGoBudget: () -> Unit,
    onChangeDate: (Long) -> Unit,
    onAddMember: (String) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val targetDate = Instant.ofEpochMilli(trip.targetDateMillis).atZone(zone).toLocalDate()
    val diffDays = ChronoUnit.DAYS.between(today, targetDate).toInt()
    val dateLabel = "%04d.%02d.%02d".format(targetDate.year, targetDate.monthValue, targetDate.dayOfMonth)

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = trip.targetDateMillis
    )

// trip의 날짜가 바뀌면 DatePickerState도 따라가게
    LaunchedEffect(trip.targetDateMillis) {
        datePickerState.selectedDateMillis = trip.targetDateMillis
    }


    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onChangeDate(it) }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("취소") } }
        ) { DatePicker(state = datePickerState) }
    }

    var showAddMember by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }

    if (showAddMember) {
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text("사람 추가") },
            text = {
                TextField(
                    value = newMemberName,
                    onValueChange = { newMemberName = it },
                    singleLine = true,
                    placeholder = { Text("이름 입력") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newMemberName.trim()
                    if (name.isNotEmpty()) {
                        onAddMember(name)
                        newMemberName = ""
                        showAddMember = false
                    }
                }) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { showAddMember = false }) { Text("취소") } }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        val coverRes = remember(trip.country) { coverResForCountry(trip.country) }

        Box(modifier = Modifier.fillMaxSize()) {

            if (coverRes != null) {
                Image(
                    painter = painterResource(id = coverRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.5f
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(trip.coverColor)
                )
            }

            Text(
                text = dateLabel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 16.dp),
                fontSize = 12.sp,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "${trip.countryEmoji}  ${trip.country}",
                        fontSize = 14.sp,
                        color = Color.White
                    )

                    Text(
                        text = if (diffDays >= 0) "D-$diffDays" else "D+${abs(diffDays)}",
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clickable { showDatePicker = true },
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    trip.members.forEach { name ->
                        MemberPill(name = name)
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEDEDED))
                            .clickable { showAddMember = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "추가")
                    }
                }

                Button(
                    onClick = onGoBudget,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.8f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp
                    )
                ) { Text("여행 비용") }
            }
        }
    }
}


@Composable
private fun MemberPill(name: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEDEDED))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF151515))
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.trim().take(1),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(name.trim(), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AddTripCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .border(2.dp, Color.Black, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewMyTrips() {
    MaterialTheme {
        MyTripsScreen(
            trips = emptyList(),
            onGoBudget = {},
            onChangeDate = { _, _ -> },
            onAddMember = { _, _ -> },
            onCreateTrip = { _, _ -> "temp" }
        )
    }
}
