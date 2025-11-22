package com.example.travelog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelog.data.WeatherRepository
import com.example.travelog.data.network.RetrofitClient
import kotlin.collections.forEachIndexed
import kotlin.collections.lastIndex
import com.example.travelog.data.model.DailyWeatherUi
import com.example.travelog.data.model.HourlyWeatherUi

@Composable
fun WeatherScreen() {
    // 상태 값들
    var temperature by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf<String?>(null) }
    var cityName by remember { mutableStateOf("Seoul") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hourlyList by remember { mutableStateOf<List<HourlyWeatherUi>>(emptyList()) }
    var dailyList by remember { mutableStateOf<List<DailyWeatherUi>>(emptyList()) }

    // 첫 진입 시 한 번만 호출
    LaunchedEffect(cityName) {
        try {
            val apiKey = "450a019012f76c52e4d95ec2531fa8c7"
            // 1) 현재 날씨
            val response = RetrofitClient.weatherApi.getCurrentWeather(
                city = cityName,
                apiKey = apiKey
            )
            temperature = "${response.main.temp.toInt()}°C"
            description = response.weather.firstOrNull()?.description ?: ""
            errorMessage = null

            // 2) 시간별 + 일별 예보
            val (hourly, daily) = WeatherRepository.loadHourlyAndDaily(cityName)
            hourlyList = hourly
            dailyList = daily

        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "날씨 정보를 불러오지 못했습니다."
        }
    }

    Column(
        // Background
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)    // 배경 색상: 흰색
            .padding(horizontal = 0.dp, vertical = 10.dp)
    ) {
        // Text + Bookmark + Notification icons
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
            // space between Search bar & Bookmark Button
            Spacer(modifier = Modifier.width(10.dp))

            // Bookmark Button
            Icon(
                painter = painterResource(id = R.drawable.icon_bookmark),
                contentDescription = "Bookmark Icon",
                tint = Color.Black,
                modifier = Modifier
                    .size(56.dp)
                    .padding(10.dp)
                    .clickable {
                        println("Bookmark clicked")
                    }
            )

            Spacer(modifier = Modifier.width(1.dp))

            // Notifications Button
            Icon(
                painter = painterResource(id = R.drawable.icon_notification),
                contentDescription = "Alert Icon",
                tint = Color.Black,
                modifier = Modifier
                    .size(56.dp)
                    .padding(10.dp)
                    .clickable {
                        println("Notifications clicked")
                    }
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            WeatherCountryTabs()
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

        //
        Spacer(modifier = Modifier.height(16.dp))

        // ▼ 여기서부터 중앙 큰 온도 + 카드들
        // 도시 이름 + 현재 기온
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(
                    text = "삿포로",          // TODO: 나중에 cityName으로 바꿔도 됨
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                when {
                    errorMessage != null -> {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 14.sp,
                            color = Color.Red
                        )
                    }
                    temperature == null -> {
                        Text(
                            text = "날씨 불러오는 중...",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    else -> {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = temperature ?: "0°C",
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.offset(y = (-6).dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 오늘의 날씨 카드 (시간별)
        TodayHourlyWeatherCard(items = hourlyList)

        Spacer(modifier = Modifier.height(16.dp))

        // 요일별 날씨 카드
        WeeklyWeatherCard(items = dailyList)
    }
}

@Composable
fun WeatherCountryTabs() {
    val tabs = listOf("내 여행지", "일본", "영국", "미국", "중국")
    var selectedTab by remember { mutableStateOf(0) }

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
                    .padding(end = 18.dp)
                    .clickable { selectedTab = index }
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
            .height(120.dp)
            .background(Color(0xFFE5E5E5), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text("오늘의 날씨", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = item.label, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.icon_sun_small),
                            contentDescription = null,
                            tint = Color(0xFFB28C5E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = item.tempText, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyWeatherCard(
    items: List<DailyWeatherUi>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(Color(0xFFE5E5E5), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text("날씨", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            items.forEachIndexed { index, item ->
                DayRow(
                    dayLabel = item.dayLabel,
                    minTemp = item.minTempText,
                    maxTemp = item.maxTempText
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
    maxTemp: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽 요일 + 아이콘
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.icon_snow_small), // 임시 아이콘
                contentDescription = null,
                tint = Color(0xFFB0B0B0),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = dayLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 가운데 바(온도 범위 표시용 더미)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .background(Color(0xFFD6D6D6), RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)   // 더미 비율
                    .background(Color(0xFFB3D9F5), RoundedCornerShape(999.dp))
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 오른쪽 최저/최고
        Text(
            text = maxTemp,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
