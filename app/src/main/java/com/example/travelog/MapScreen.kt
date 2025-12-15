package com.example.travelog

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(37.5665, 126.9780), // 서울
            10f
        )
    }
    var showBottomSheet by remember { mutableStateOf(false) }
    var targetCity by remember { mutableStateOf<String?>(null) }

    var tripStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var tripEndDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedDay by remember { mutableStateOf(1) }

    /* 지도 이동 사이드이펙트 */

    LaunchedEffect(targetCity) {
        if (!targetCity.isNullOrBlank()) {
            moveMapToCity(
                context = context,
                city = targetCity!!,
                cameraPositionState = cameraPositionState
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        /* 타이틀 */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "여행지도",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, Color.Black, CircleShape)
                    .clickable { showBottomSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        /* 지도 */
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(500.dp)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            )
        }
        /* Day 버튼 (지도 아래) */
        if (tripStartDate != null && tripEndDate != null) {
            DayButtonRow(
                startDate = tripStartDate!!,
                endDate = tripEndDate!!,
                selectedDay = selectedDay,
                onDayClick = { selectedDay = it }
            )
        }
    }
    /* 여행 일정 추가 BottomSheet */
    if (showBottomSheet) {
        TravelAddBottomSheet(
            onDismiss = { showBottomSheet = false },
            onSave = { start, end, destination ->
                tripStartDate = start
                tripEndDate = end
                selectedDay = 1
                targetCity = destination
                showBottomSheet = false
            }
        )
    }
}
/* BottomSheet */
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
                DateBox(startDate?.toSlashFormat() ?: "시작일") {
                    showDatePicker = true
                }
                Text("  ~  ")
                DateBox(endDate?.toSlashFormat() ?: "종료일") {
                    showDatePicker = true
                }
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
                        onSave(startDate!!, endDate!!, destination)
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
            onConfirm = { start, end ->
                startDate = start
                endDate = end
                showDatePicker = false
            }
        )
    }
}

/* DateRangePicker Dialog */
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
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    if (start != null && end != null) {
                        onConfirm(
                            millisToLocalDate(start),
                            millisToLocalDate(end)
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
/* Day Buttons */
@Composable
fun DayButtonRow(
    startDate: LocalDate,
    endDate: LocalDate,
    selectedDay: Int,
    onDayClick: (Int) -> Unit
) {
    val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (day in 1..totalDays) {
            OutlinedButton(
                onClick = { onDayClick(day) },
                shape = RoundedCornerShape(20.dp),
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
/* Map Move Logic */
suspend fun moveMapToCity(
    context: android.content.Context,
    city: String,
    cameraPositionState: CameraPositionState
) {
    val geocoder = Geocoder(context, Locale.KOREA)
    val result = withContext(Dispatchers.IO) {
        geocoder.getFromLocationName(city, 1)
    }
    if (!result.isNullOrEmpty()) {
        val latLng = LatLng(result[0].latitude, result[0].longitude)

        cameraPositionState.animate(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(latLng, 11f)
            ),
            durationMs = 1000
        )
    }
}
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