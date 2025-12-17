package com.example.travelog

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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import androidx.navigation.compose.rememberNavController

// ------------------------------
// 모델
// ------------------------------
data class Trip(
    val id: String,
    val countryEmoji: String,
    val country: String,
    val targetDateMillis: Long,
    val members: List<String>,
    val coverColor: Color = Color(0xFFE8F0FE)
)

// ✅ 처음엔 아무 카드도 안 보이게
fun demoTrips(): List<Trip> = emptyList()

private fun emojiForCountry(input: String): String {
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

// ------------------------------
// 루트 라우팅
// ------------------------------
@Composable
fun TravelApp() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        val navController = rememberNavController()
        var route by remember { mutableStateOf("list") }
        var selectedTripId by remember { mutableStateOf<String?>(null) }

        var trips by remember { mutableStateOf(demoTrips()) }

        fun updateTrip(id: String, updater: (Trip) -> Trip) {
            trips = trips.map { if (it.id == id) updater(it) else it }
        }

        fun addTrip(country: String, dateMillis: Long): String {
            val id = System.currentTimeMillis().toString()
            trips = trips + Trip(
                id = id,
                countryEmoji = emojiForCountry(country),
                country = country.trim(),
                targetDateMillis = dateMillis,
                members = emptyList()
            )
            return id
        }

        when (route) {
            "list" -> MyTripsScreen(
                trips = trips,
                onGoArchive = { route = "archive" },
                onGoBudget = { trip ->
                    selectedTripId = trip.id
                    route = "budget"
                },
                onChangeDate = { tripId, newMillis ->
                    updateTrip(tripId) { it.copy(targetDateMillis = newMillis) }
                },
                onAddMember = { tripId, name ->
                    updateTrip(tripId) { it.copy(members = it.members + name.trim()) }
                },
                onCreateTrip = { country, dateMillis ->
                    addTrip(country, dateMillis)
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
                val selectedTrip = trips.firstOrNull { it.id == selectedTripId }

                // ✅ 너 프로젝트에 있는 TripBudgetScreen 그대로 호출
                TripBudgetScreen(
                    tripTitle = selectedTrip?.country ?: "여행",
                    onBack = { route = "list" }
                )
            }
        }
    }
}

// ------------------------------
// 화면 1: 내 여행 리스트 (+ 버튼으로 여행 생성)
// ------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTripsScreen(
    trips: List<Trip>,
    onGoArchive: () -> Unit,
    onGoBudget: (Trip) -> Unit,
    onChangeDate: (String, Long) -> Unit,
    onAddMember: (String, String) -> Unit,
    onCreateTrip: (country: String, dateMillis: Long) -> String, // return newTripId
) {
    // 여행 추가 다이얼로그
    var showCreateTrip by remember { mutableStateOf(false) }

    // 여행 추가 직후 사람 추가 팝업 자동
    var pendingAddMemberTripId by remember { mutableStateOf<String?>(null) }
    var showAddMemberPopup by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }

    // “여행 생성” 다이얼로그 내부 상태
    val zone = ZoneId.systemDefault()
    val todayMillis = remember {
        LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
    }
    var createCountry by remember { mutableStateOf("") }
    var createDateMillis by remember { mutableStateOf(todayMillis) }
    var showCreateDatePicker by remember { mutableStateOf(false) }
    val createDatePickerState = rememberDatePickerState(initialSelectedDateMillis = createDateMillis)

    if (showCreateDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showCreateDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val sel = createDatePickerState.selectedDateMillis
                    if (sel != null) createDateMillis = sel
                    showCreateDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showCreateDatePicker = false }) { Text("취소") } }
        ) {
            DatePicker(state = createDatePickerState)
        }
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

    // 생성 직후 멤버 추가 팝업
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
        Spacer(modifier = Modifier.width(180.dp))


        Icon(
            painter = painterResource(id = R.drawable.icon_bookmark),
            contentDescription = "저장",
            tint = Color.Black,
            modifier = Modifier
                .size(56.dp)
                .padding(10.dp)
                .clickable { println("Bookmark clicked") }
        )

        Spacer(modifier = Modifier.width(1.dp))

        Icon(
            painter = painterResource(id = R.drawable.icon_notification),
            contentDescription = "알림",
            tint = Color.Black,
            modifier = Modifier
                .size(56.dp)
                .padding(10.dp)
                .clickable { println("Bookmark clicked") }
        )
    }

    Scaffold() {
        Column(
            modifier = Modifier
                .padding(horizontal = 0.dp, vertical = 0.dp)
                .fillMaxSize()
        ) {
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
                Spacer(modifier = Modifier.width(180.dp))


                Icon(
                    painter = painterResource(id = R.drawable.icon_bookmark),
                    contentDescription = "저장",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(56.dp)
                        .padding(10.dp)
                        .clickable { println("Bookmark clicked") }
                )

                Spacer(modifier = Modifier.width(1.dp))

                Icon(
                    painter = painterResource(id = R.drawable.icon_notification),
                    contentDescription = "알림",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(56.dp)
                        .padding(10.dp)
                        .clickable { println("Bookmark clicked") }
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
                // ✅ trips가 비어있으면 카드 0개 => +만 보임
                items(trips) { trip ->
                    TripCard(
                        trip = trip,
                        onGoBudget = { onGoBudget(trip) },
                        onChangeDate = { millis -> onChangeDate(trip.id, millis) },
                        onAddMember = { name -> onAddMember(trip.id, name) }
                    )
                }

                // ✅ 첫번째 사진처럼 맨 아래 “+ 버튼만 있는 카드”
                item {
                    AddTripCard(onClick = { showCreateTrip = true })
                }
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
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = trip.targetDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) onChangeDate(selected)
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("취소") } }
        ) {
            DatePicker(state = datePickerState)
        }
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
            dismissButton = {
                TextButton(onClick = {
                    newMemberName = ""
                    showAddMember = false
                }) { Text("취소") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(trip.coverColor)
        ) {


            // 왼쪽 위 날짜
            Text(
                text = dateLabel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 16.dp),
                fontSize = 12.sp,
                color = Color(0xFF777777)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(14.dp) // ✅ japan ↕ D-day 간격
                ) {
                    // 나라/도시
                    Text(
                        text = "${trip.countryEmoji}  ${trip.country}",
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.8f)
                    )

                    // D-day (오른쪽, 눌러서 날짜 변경)
                    Text(
                        text = if (diffDays >= 0) "D-$diffDays" else "D+${abs(diffDays)}",
                        modifier = Modifier.padding(top = 12.dp).clickable { showDatePicker = true },
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black.copy(alpha = 0.85f)
                    )
                }
            }



            // 아래: 사람들(이름 전체 표시) + 여행비용 버튼
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

                OutlinedButton(
                    onClick = onGoBudget,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("여행 비용")
                }
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

//@Composable
//fun BottomNavBar() {
//    NavigationBar {
//        NavigationBarItem(
//            selected = true,
//            onClick = { },
//            icon = { Icon(Icons.Filled.Home, contentDescription = "홈") }
//        )
//        NavigationBarItem(
//            selected = false,
//            onClick = { },
//            icon = { Icon(Icons.Filled.List, contentDescription = "리스트") }
//        )
//        NavigationBarItem(
//            selected = false,
//            onClick = { },
//            icon = { Icon(Icons.Filled.MoreVert, contentDescription = "더보기") }
//        )
//    }
//}

// ------------------------------
// Preview
// ------------------------------
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewMyTrips() {
    MaterialTheme {
        MyTripsScreen(
            trips = demoTrips(), // ✅ emptyList()
            onGoArchive = {},
            onGoBudget = {},
            onChangeDate = { _, _ -> },
            onAddMember = { _, _ -> },
            onCreateTrip = { _, _ -> "temp" }
        )
    }
}
