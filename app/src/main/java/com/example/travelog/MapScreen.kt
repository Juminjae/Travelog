package com.example.travelog

import android.content.Context
import android.location.Geocoder
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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

class TripViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_TRIP_JSON = "trip_json"
        private const val KEY_DAYPLACES_JSON = "dayplaces_json"
    }

    var tripPlan by mutableStateOf<TripPlan?>(null)
        private set

    /* Day별 장소 목록 */
    val dayPlaces = mutableStateMapOf<Int, MutableList<DayPlace>>()

    init {
        restoreFromSavedState()
    }

    fun createTrip(start: LocalDate, end: LocalDate, destination: String) {
        tripPlan = TripPlan(start, end, destination)

        dayPlaces.clear()
        val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
        repeat(days) { idx ->
            dayPlaces[idx + 1] = mutableListOf()
        }

        persistToSavedState()
    }

    fun addPlace(day: Int, place: DayPlace) {
        val current = dayPlaces[day] ?: mutableListOf()
        dayPlaces[day] = (current + place).toMutableList()
        persistToSavedState()
    }

    fun totalDays(): Int {
        val t = tripPlan ?: return 0
        return ChronoUnit.DAYS.between(t.startDate, t.endDate).toInt() + 1
    }

    fun placesByDate(date: LocalDate): List<DayPlace> {
        val trip = tripPlan ?: return emptyList()
        val dayIndex = ChronoUnit.DAYS.between(trip.startDate, date).toInt() + 1
        if (dayIndex < 1 || dayIndex > totalDays()) return emptyList()
        return dayPlaces[dayIndex] ?: emptyList()
    }

    private fun persistToSavedState() {
        // tripPlan
        val trip = tripPlan
        if (trip != null) {
            val obj = JSONObject().apply {
                put("startDate", trip.startDate.toString()) // ISO-8601
                put("endDate", trip.endDate.toString())
                put("destination", trip.destination)
            }
            savedStateHandle[KEY_TRIP_JSON] = obj.toString()
        } else {
            savedStateHandle.remove<String>(KEY_TRIP_JSON)
        }

        // dayPlaces
        val root = JSONObject()
        dayPlaces.forEach { (day, places) ->
            val arr = JSONArray()
            places.forEach { p ->
                arr.put(
                    JSONObject().apply {
                        put("title", p.title)
                        put("lat", p.latLng.latitude)
                        put("lng", p.latLng.longitude)
                    }
                )
            }
            root.put(day.toString(), arr)
        }
        savedStateHandle[KEY_DAYPLACES_JSON] = root.toString()
    }

    private fun restoreFromSavedState() {
        // tripPlan
        val tripJson = savedStateHandle.get<String>(KEY_TRIP_JSON)
        if (!tripJson.isNullOrBlank()) {
            runCatching {
                val obj = JSONObject(tripJson)
                val start = LocalDate.parse(obj.getString("startDate"))
                val end = LocalDate.parse(obj.getString("endDate"))
                val dest = obj.getString("destination")
                tripPlan = TripPlan(start, end, dest)
            }
        }

        // dayPlaces
        val dayJson = savedStateHandle.get<String>(KEY_DAYPLACES_JSON)
        if (!dayJson.isNullOrBlank()) {
            runCatching {
                val root = JSONObject(dayJson)
                dayPlaces.clear()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val day = k.toInt()
                    val arr = root.getJSONArray(k)
                    val list = mutableListOf<DayPlace>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val title = o.getString("title")
                        val lat = o.getDouble("lat")
                        val lng = o.getDouble("lng")
                        list.add(DayPlace(title, LatLng(lat, lng)))
                    }
                    dayPlaces[day] = list
                }
            }
        }

        // tripPlan이 있는데 dayPlaces가 비어있으면(복구 중 일부만 성공 등) 기본 day 틀 만들어줌
        val t = tripPlan
        if (t != null && dayPlaces.isEmpty()) {
            val days = ChronoUnit.DAYS.between(t.startDate, t.endDate).toInt() + 1
            repeat(days) { idx ->
                dayPlaces[idx + 1] = mutableListOf()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    // ✅ 같은 Activity 범위로 ViewModel을 잡아서 화면 이동/재구성에도 데이터가 덜 날아가게
    tripViewModel: TripViewModel = run {
        val owner = (LocalContext.current as ComponentActivity)
        viewModel(viewModelStoreOwner = owner)
    }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(37.5665, 126.9780),
            10f
        )
    }

    // ✅ UI 상태도 저장(회전/프로세스 재생성 시) - 데이터 “날아감” 체감 줄이기
    var showTripBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showPlaceBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectedDay by rememberSaveable { mutableStateOf(1) }

    val trip = tripViewModel.tripPlan
    val totalDays = tripViewModel.totalDays()

    val placesForDay = tripViewModel.dayPlaces[selectedDay] ?: emptyList()

    LaunchedEffect(trip?.destination) {
        trip?.destination?.let {
            moveMapToCity(context, it, cameraPositionState)
        }
    }

    // ✅ trip이 새로 생겼는데 selectedDay가 범위 밖이면 보정
    LaunchedEffect(trip, totalDays) {
        if (trip != null && totalDays > 0 && selectedDay !in 1..totalDays) {
            selectedDay = 1
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
