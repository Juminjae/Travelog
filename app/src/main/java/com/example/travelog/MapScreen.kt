package com.example.travelog

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

data class TripPlan(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val destination: String
)
data class DayPlace(
    val title: String,
    val latLng: LatLng
)

class TripViewModel : ViewModel() {
    var tripPlan by mutableStateOf<TripPlan?>(null)
        private set
    /* Day별 장소 목록 */
    val dayPlaces = mutableStateMapOf<Int, MutableList<DayPlace>>()
    fun createTrip(start: LocalDate, end: LocalDate, destination: String) {
        tripPlan = TripPlan(start, end, destination)
        dayPlaces.clear()
        val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
        repeat(days) {
            dayPlaces[it + 1] = mutableListOf()
        }
    }
    fun addPlace(day: Int, place: DayPlace) {
        val current = dayPlaces[day] ?: mutableListOf()
        dayPlaces[day] = (current + place).toMutableList()
    }
    fun totalDays(): Int {
        val t = tripPlan ?: return 0
        return ChronoUnit.DAYS.between(t.startDate, t.endDate).toInt() + 1
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    tripViewModel: TripViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(37.5665, 126.9780),
            10f
        )
    }
    var showTripBottomSheet by remember { mutableStateOf(false) }
    var showPlaceBottomSheet by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf(1) }

    val trip = tripViewModel.tripPlan
    val totalDays = tripViewModel.totalDays()

    val placesForDay = tripViewModel.dayPlaces[selectedDay] ?: emptyList()

    LaunchedEffect(trip?.destination) {
        trip?.destination?.let {
            moveMapToCity(context, it, cameraPositionState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("여행지도", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showTripBottomSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = 20.sp)
            }
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(430.dp)
                .border(2.dp, Color.LightGray, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // ✅ 선택된 Day의 장소들 마커 표시
                placesForDay.forEach { p ->
                    Marker(
                        state = MarkerState(p.latLng),
                        title = p.title
                    )
                }
            }
        }
        /* Day Buttons */
        if (trip != null) {
            DayButtonRow(
                totalDays = totalDays,
                selectedDay = selectedDay,
                onDayClick = { selectedDay = it }
            )
        }
        /* Day Content */
        if (trip != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Day $selectedDay", fontWeight = FontWeight.SemiBold)
                    Box(
                        modifier = Modifier
                            .size(25.dp)
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable { showPlaceBottomSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "+ 버튼을 눌러 여행 장소를 추가해보세요.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                /* 카드 리스트 */
                if (placesForDay.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(placesForDay) { index, place ->
                            DayPlaceCard(
                                order = index + 1,
                                title = place.title
                            )
                        }
                    }
                }
            }
        }
    }

    /* 여행 추가 BottomSheet */
    if (showTripBottomSheet) {
        TravelAddBottomSheet(
            onDismiss = { showTripBottomSheet = false },
            onSave = { start, end, destination ->
                tripViewModel.createTrip(start, end, destination)
                selectedDay = 1
                showTripBottomSheet = false
            }
        )
    }
    /* 장소 추가 BottomSheet (이름만 입력) */
    if (showPlaceBottomSheet) {
        PlaceAddBottomSheet(
            onDismiss = { showPlaceBottomSheet = false },
            onSave = { placeName ->
                // ✅ suspend 함수(addPlaceByName) 호출은 코루틴으로 감싸야 함
                scope.launch {
                    val added = addPlaceByName(context, placeName)
                    if (added != null) {
                        tripViewModel.addPlace(selectedDay, added)

                        // ✅ 지도 이동
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(added.latLng, 13f)
                            ),
                            durationMs = 800
                        )
                    }
                }
                showPlaceBottomSheet = false
            }
        )
    }
}

/* Components */
@Composable
fun DayButtonRow(
    totalDays: Int,
    selectedDay: Int,
    onDayClick: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(totalDays) { index ->
            val day = index + 1
            OutlinedButton(
                onClick = { onDayClick(day) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor =
                        if (day == selectedDay) Color(0xFFE5E5E5)
                        else Color.Transparent
                )
            ) {
                Text("Day $day")
            }
        }
    }
}

/* 카드 UI (기존 카드 느낌 유지, title만 표시) */
@Composable
fun DayPlaceCard(
    order: Int,
    title: String
) {
    Row(Modifier.fillMaxWidth()) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Text("$order", fontWeight = FontWeight.Bold)
            Box(
                Modifier
                    .width(2.dp)
                    .height(80.dp)
                    .background(Color.LightGray)
            )
        }

        Spacer(Modifier.width(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("지도의 마커로 위치를 확인하세요", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

/* Place Add BottomSheet - 이름만 입력 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceAddBottomSheet(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Text("장소 추가", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("예: 오사카성, 해운대, 도쿄역") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onSave(title.trim()) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("추가")
            }
        }
    }
}

/* ✅ TravelAddBottomSheet 정의 포함 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelAddBottomSheet(
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalDate, String) -> Unit
) {
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var destination by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text("여행 일정 추가", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(20.dp))
            Text("여행 기간", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                DateBox(startDate?.toSlashFormat() ?: "시작일") { showDatePicker = true }
                Text("  ~  ")
                DateBox(endDate?.toSlashFormat() ?: "종료일") { showDatePicker = true }
            }

            Spacer(Modifier.height(20.dp))
            Text("여행지", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                placeholder = { Text("예: 삿포로, 도쿄, 서울") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (startDate != null && endDate != null && destination.isNotBlank()) {
                        onSave(startDate!!, endDate!!, destination.trim())
                    }
                },
                enabled = startDate != null && endDate != null && destination.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("저장")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { s, e ->
                startDate = s
                endDate = e
                showDatePicker = false
            }
        )
    }
}

/* ✅ DateRangePickerDialog 정의 포함 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    val state = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val startMillis = state.selectedStartDateMillis
                    val endMillis = state.selectedEndDateMillis
                    if (startMillis != null && endMillis != null) {
                        onConfirm(
                            millisToLocalDate(startMillis),
                            millisToLocalDate(endMillis)
                        )
                    }
                }
            ) { Text("확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    ) {
        DateRangePicker(state = state)
    }
}

/* Utils */
@Composable
fun DateBox(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text)
    }
}

fun millisToLocalDate(millis: Long): LocalDate =
    java.time.Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

fun LocalDate.toSlashFormat(): String =
    this.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
/* 좌표 변환 */
suspend fun addPlaceByName(
    context: Context,
    placeName: String
): DayPlace? {
    val geocoder = Geocoder(context, Locale.KOREA)

    val result = withContext(Dispatchers.IO) {
        runCatching { geocoder.getFromLocationName(placeName, 1) }.getOrNull()
    }

    if (!result.isNullOrEmpty()) {
        val latLng = LatLng(result[0].latitude, result[0].longitude)
        return DayPlace(title = placeName, latLng = latLng)
    }
    return null
}
suspend fun moveMapToCity(
    context: Context,
    city: String,
    cameraPositionState: CameraPositionState
) {
    val geocoder = Geocoder(context, Locale.KOREA)
    val result = withContext(Dispatchers.IO) {
        runCatching { geocoder.getFromLocationName(city, 1) }.getOrNull()
    }
    if (!result.isNullOrEmpty()) {
        val latLng = LatLng(result[0].latitude, result[0].longitude)
        cameraPositionState.animate(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(latLng, 11f)
            ),
            durationMs = 800
        )
    }
}