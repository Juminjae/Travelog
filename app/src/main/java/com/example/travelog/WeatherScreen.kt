package com.example.travelog

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
fun WeatherScreen(
    weatherViewModel: WeatherViewModel = viewModel()
) {
    // ViewModel 상태 읽기
    var temperature = weatherViewModel.temperature
    var iconCode = weatherViewModel.iconCode
    var hourlyList = weatherViewModel.hourlyList
    var dailyList = weatherViewModel.dailyList
    var errorMessage = weatherViewModel.errorMessage
    var isLoading = weatherViewModel.isLoading

    val tabCities = listOf(
        Triple("내 여행지", "Sapporo,jp", "삿포로"),
        Triple("일본", "Tokyo,jp", "도쿄"),
        Triple("영국", "London,gb", "런던"),
        Triple("미국", "New York,us", "뉴욕"),
        Triple("중국", "Shanghai,cn", "상하이")
    )

    var selectedTab by remember { mutableStateOf(0) }
    var apiCity by remember { mutableStateOf(tabCities[selectedTab].second) }

    LaunchedEffect(selectedTab) {
        apiCity = tabCities[selectedTab].second
    }

    // 홈을 안 거치고 바로 들어온 경우 대비해서 한 번 로드
    LaunchedEffect(apiCity) {
        try {
            errorMessage = null

            val apiKey = BuildConfig.WEATHER_API_KEY

            val response = RetrofitClient.weatherApi.getCurrentWeather(
                city = apiCity,
                apiKey = apiKey
            )

            temperature = "${response.main.temp.toInt()}°C"
            iconCode = response.weather.firstOrNull()?.icon

            val (hourly, daily) = WeatherRepository.loadHourlyAndDaily(apiCity)
            hourlyList = hourly
            dailyList = daily

        } catch (e: Exception) {
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
            WeatherCountryTabs(
                tabs = tabCities.map { it.first },
                selectedTab = selectedTab,
                onSelect = { index ->
                    selectedTab = index
                    val api   = tabCities[index].second  // "Sapporo,jp" ...
                    val displayCity = tabCities[index].third
                    weatherViewModel.load(apiCity = api, display = displayCity)
                }
            )
        }

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

                val displayName = weatherViewModel.displayCityName

                Text(
                    text = displayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(5.dp))

                // 날씨 아이콘
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
                    temperature == null && errorMessage == null -> {
                        Text(
                            text = "날씨 불러오는 중...",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage!!,
                            fontSize = 14.sp,
                            color = Color.Red
                        )
                    }
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

        // 오늘의 날씨 카드 (시간별)
        TodayHourlyWeatherCard(items = hourlyList)

        Spacer(modifier = Modifier.height(25.dp))

        // 요일별 날씨 카드
        WeeklyWeatherCard(items = dailyList)
    }
}

@Composable
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
        tabs.forEachIndexed { index, title ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = if (selectedTab == index) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (selectedTab == index) Color.Black else Color.Gray
                )

                Spacer(modifier = Modifier.height(3.dp))

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

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(items) { item ->
                    Column(
                        modifier = Modifier.width(30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val iconRes = mapWeatherIcon(item.iconCode) ?: R.drawable.icon_sun_small
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified
                        )

                        Spacer(modifier = Modifier.height(4.dp))

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

private fun String.toTempInt(): Int {
    val num = Regex("-?\\d+").find(this)?.value ?: "0"
    return num.toInt()
}

@Composable
fun WeeklyWeatherCard(
    items: List<DailyWeatherUi>
) {
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

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun DayRow(
    dayLabel: String,
    minTemp: String,
    maxTemp: String,
    iconCode: String?,
    weekMin: Int,
    weekMax: Int
) {
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

    // ✅ 바는 남은 공간을 다 쓰게 weight(1f)만 여기 한 번 사용
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)   // 날씨앱처럼 얇게(원하면 8.dp로)
        ) {
            val trackShape = RoundedCornerShape(999.dp)

            // 전체 트랙(회색)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFD6D6D6), trackShape)
            )

            // 그날 범위(파란색): 시작 위치 + 길이
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