package com.example.travelog

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelog.data.model.DailyWeatherUi
import com.example.travelog.data.model.HourlyWeatherUi
import com.example.travelog.data.model.mapWeatherIcon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.travelog.data.network.RetrofitClient
import com.example.travelog.data.WeatherRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
// 날씨 화면
fun WeatherScreen(
    // 여행(추가한 여행들) 목록을 가져오기 위함
    tripsVm: TripsViewModel,
    weatherViewModel: WeatherViewModel = viewModel()
) {

    // 화면에서 사용될 상태 선언
    var temperature by remember { mutableStateOf<String?>(null) }
    var iconCode by remember { mutableStateOf<String?>(null) }
    var hourlyList by remember { mutableStateOf<List<HourlyWeatherUi>>(emptyList()) }
    var dailyList by remember { mutableStateOf<List<DailyWeatherUi>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 기본 시간대 가져오기
    // remember 사용해서 리컴포지션 때마다 매번 새로 만들지 않게
    val zone = remember { ZoneId.systemDefault() }

    // 출국일이 가장 가까운 여행 계산
    val nearestTrip by remember(zone, tripsVm.trips) {
        derivedStateOf {
            val today = LocalDate.now()
            tripsVm.trips
                .map { trip ->
                    val targetDate = Instant.ofEpochMilli(trip.targetDateMillis)
                        .atZone(zone).toLocalDate()
                    trip to ChronoUnit.DAYS.between(today, targetDate).toInt()
                }
                .filter { (_, diff) -> diff >= 0 }
                .minByOrNull { (_, diff) -> diff }
        }
    }

    // 여행지 탭에 넣은 API 관련 이름
    val myTripApi = nearestTrip?.first?.country?.let { toWeatherQuery(it) } ?: "Seoul,kr"
    val myTripDisplay = nearestTrip?.first?.country ?: "서울"

    // 각 탭을 Triple(탭이름, apiCity, 화면표시이름) 형태로 저장
    val tabCities = listOf(
        Triple("내 여행지", myTripApi, myTripDisplay),
        Triple("서울", "Seoul,kr", "서울"),
        Triple("일본", "Tokyo,jp", "도쿄"),
        Triple("영국", "London,gb", "런던"),
        Triple("미국", "New York,us", "뉴욕"),
        Triple("중국", "Shanghai,cn", "상하이")
    )

    // 현재 선택된 탭, API 도시 상태 (초기값 0)
    var selectedTab by remember { mutableStateOf(0) }
    var apiCity by remember { mutableStateOf(tabCities[selectedTab].second) }

    // 탭이 바뀌면 apiCity도 바꾸기
    LaunchedEffect(selectedTab) {
        apiCity = tabCities[selectedTab].second
    }

    // apiCity가 바뀌면 날씨를 로드
    LaunchedEffect(apiCity) {
        try {
            // 이전 사용 에러 초기화
            errorMessage = null

            // 빌드 설정에 저장된 API Key 사용
            val apiKey = BuildConfig.WEATHER_API_KEY

            // 현재 날씨 호출
            val response = RetrofitClient.weatherApi.getCurrentWeather(
                city = apiCity,
                apiKey = apiKey
            )

            temperature = "${response.main.temp.toInt()}°C"
            iconCode = response.weather.firstOrNull()?.icon

            val (hourly, daily) = WeatherRepository.loadHourlyAndDaily(apiCity)
            hourlyList = hourly
            dailyList = daily

        } catch (e: Exception) { // 네트워크 에러 시 에러메시지 세팅
            e.printStackTrace()
            errorMessage = e.message ?: "날씨 정보를 불러오지 못했습니다."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 0.dp, vertical = 10.dp)
    ) {
        // 상단 타이틀 + 아이콘
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .width(265.dp)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "여행지 날씨 정보",
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                painter = painterResource(id = R.drawable.icon_bookmark),
                contentDescription = "Bookmark Icon",
                tint = Color.Black,
                modifier = Modifier
                    .size(56.dp)
                    .padding(10.dp)
                    .clickable { println("Bookmark clicked") }
            )

            Spacer(modifier = Modifier.width(1.dp))

            Icon(
                painter = painterResource(id = R.drawable.icon_notification),
                contentDescription = "Alert Icon",
                tint = Color.Black,
                modifier = Modifier
                    .size(56.dp)
                    .padding(10.dp)
                    .clickable { println("Notifications clicked") }
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // WeatherCountryTabs 호출
            WeatherCountryTabs(
                // .first만 뽑아서 탭에 표시할 텍스트 목록을 만들기
                tabs = tabCities.map { it.first },
                selectedTab = selectedTab,
                onSelect = { index ->
                    selectedTab = index
                    apiCity = tabCities[index].second
                    weatherViewModel.displayCityName = tabCities[index].third
                }
            )
        }

        // 탭 및 구분선
        Box(
            modifier = Modifier
                .offset(x = 0.dp, y = (-10).dp)
                .fillMaxWidth()
                .background(Color.White)
                .drawBehind {
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ▼ 중앙 도시 + 아이콘 + 큰 온도
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // 화면에 표시할 도시 이름
                val displayName = weatherViewModel.displayCityName

                // 도시명 출력
                Text(
                    text = displayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(5.dp))

                // 날씨 아이콘 리소스 매핑 + 출력
                // API에서 받은 문자열 코드
                val iconRes: Int? = iconCode?.let { mapWeatherIcon(it) }

                if (iconRes != null) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = "현재 날씨 아이콘",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(
                                elevation = 50.dp,
                                shape = RoundedCornerShape(20.dp),
                                clip = false
                            )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when {
                    // 로딩 중일 때
                    temperature == null && errorMessage == null -> {
                        Text(
                            text = "날씨 불러오는 중...",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    // 에러 발생 시
                    errorMessage != null -> {
                        Text(
                            text = errorMessage!!,
                            fontSize = 14.sp,
                            color = Color.Red
                        )
                    }
                    // 날씨를 정상적으로 받았을 때
                    else -> {
                        Text(
                            text = temperature ?: "0°C",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 오늘의 날씨 카드 (시간별) 호출
        TodayHourlyWeatherCard(items = hourlyList)

        Spacer(modifier = Modifier.height(25.dp))

        // 요일별 날씨 카드 호출
        WeeklyWeatherCard(items = dailyList)
    }
}

// 여행지(나라/도시 이름) → 날씨 API에서 사용하는 도시 쿼리 문자열
private fun toWeatherQuery(countryOrCity: String): String {
    val s = countryOrCity.trim().lowercase()
    return when {
        listOf("한국", "seoul").any { s.contains(it) } -> "Seoul,kr"
        listOf("삿포로", "sapporo").any { s.contains(it) } -> "Sapporo,jp"
        listOf("도쿄", "tokyo").any { s.contains(it) } -> "Tokyo,jp"
        listOf("런던", "london").any { s.contains(it) } -> "London,gb"
        listOf("뉴욕", "new york", "nyc").any { s.contains(it) } -> "New York,us"
        listOf("상하이", "shanghai").any { s.contains(it) } -> "Shanghai,cn"
        else -> "Seoul,kr" // 매핑 실패하면 서울
    }
}

@Composable
// 날씨 상단 탭 UI
fun WeatherCountryTabs(
    tabs: List<String>,
    selectedTab: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 탭 목록 반복 렌더링
        tabs.forEachIndexed { index, title ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = if (selectedTab == index) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (selectedTab == index) Color.Black else Color.Gray
                )

                Spacer(modifier = Modifier.height(3.dp))

                // 선택된 탭만 하단 인디케이터 표시
                if (selectedTab == index) {
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(22.dp)
                            .background(
                                Color.Black,
                                shape = RoundedCornerShape(
                                    topStart = 5.dp,
                                    topEnd = 5.dp,
                                    bottomStart = 0.dp,
                                    bottomEnd = 0.dp
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
// 오늘 날씨 시간대별 카드
fun TodayHourlyWeatherCard(
    items: List<HourlyWeatherUi>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(140.dp)
            .background(Color(0xFFE5E5E5), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                "오늘의 날씨",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Divider(
                color = Color.LightGray,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 시간별 날씨 리스트 (스크롤 가능)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 시간별 날씨 아이템 반복 렌더링
                items(items) { item ->
                    // 개별 시간 카드
                    Column(
                        modifier = Modifier.width(30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 시간 표시
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 날씨 아이콘 불러오기 + 표시
                        val iconRes = mapWeatherIcon(item.iconCode) ?: R.drawable.icon_sun_small

                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 온도
                        Text(
                            text = item.tempText,
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// 최저 - 최고 기온 범위 막대로 표시
private fun String.toTempInt(): Int {
    val num = Regex("-?\\d+").find(this)?.value ?: "0"
    return num.toInt()
}

@Composable
// 주간 날씨 카드
fun WeeklyWeatherCard(
    items: List<DailyWeatherUi>
) {
    // 한 주의 최저 최고 온도 계산
    val weekMin = items.minOfOrNull { it.minTempText.toTempInt() } ?: 0
    val weekMax = items.maxOfOrNull { it.maxTempText.toTempInt() } ?: 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(Color(0xFFE5E5E5), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text("날씨", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(6.dp))

            Divider(
                color = Color.LightGray,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 요일 별 행 반복 렌더링
            items.forEachIndexed { index, item ->
                DayRow(
                    dayLabel = item.dayLabel,
                    minTemp = item.minTempText,
                    maxTemp = item.maxTempText,
                    iconCode = item.iconCode,
                    weekMin = weekMin,
                    weekMax = weekMax
                )
                if (index != items.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun DayRow(
    dayLabel: String,
    minTemp: String,
    maxTemp: String,
    iconCode: String?,
    weekMin: Int,
    weekMax: Int
) {
    // 해당 요일의 최저/최고 숫자 변환
    val dayMin = minTemp.toTempInt()
    val dayMax = maxTemp.toTempInt()

    // 0으로 나누기 방지 (주간 최저==최고인 특수 케이스)
    val span = (weekMax - weekMin).coerceAtLeast(1)

    // 0f~1f 사이로 보정
    val startFrac = ((dayMin - weekMin).toFloat() / span).coerceIn(0f, 1f)
    val widthFrac = ((dayMax - dayMin).toFloat() / span).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽 요일 + 아이콘
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(60.dp)
        ) {
            val iconRes = mapWeatherIcon(iconCode) ?: R.drawable.icon_sun_small
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = dayLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start
            )
        }

        Text(
            text = minTemp,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6E6E6E),
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // 가운데 온도 범위 막대
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
        ) {
            val trackShape = RoundedCornerShape(999.dp)

            // 전체 트랙
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFD6D6D6), trackShape)
            )

            // 그날 범위
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(widthFrac)
                    .offset(x = maxWidth * startFrac)
                    .background(Color(0xFFB3D9F5), trackShape)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = maxTemp,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6E6E6E),
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.End
        )
    }
}