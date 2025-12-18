package com.example.travelog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha

import com.example.travelog.data.model.TodaySentence
import com.example.travelog.data.model.StudyLanguage
import com.example.travelog.data.loadSentencesFromFirestore
import com.example.travelog.data.model.mapWeatherIcon

import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(
    navController: NavHostController,   // 화면 이동
    weatherViewModel: WeatherViewModel = viewModel(),   // 날씨 관리하는 ViewModel (없으면 만들기)
    tripsVm: TripsViewModel     // 여행 (추가된 여행) 목록 ViewModel
) {
    // 검색창 입력 문자열
    // 리컴포지션 되어도 값 유지되게
    // mutableStateOf = 값이 바뀌면 UI가 자동 갱신되는
    // by -> 바로 문자열 적을 수 있게
    var query by remember { mutableStateOf("") }

    // 세로로 정렬
    Column(
        // Background
        modifier = Modifier
            .fillMaxSize()    // 최대 사이즈 사용
            .background(Color.White)    // 배경 색상: 흰색
            .padding(horizontal = 20.dp, vertical = 10.dp)    // 여백
    ) {
        // Search bar + Bookmark + Notification icons 가로로 정렬
        Row(
            // Row 안에 들어가는 항목들을 세로 방향(위–아래 기준)으로 가운데 정렬
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()    // 최대 width 사용
        ) {
            // Search bar
            OutlinedTextField(    // 검색창 컴포넌트
                // textfield에 표시될 값
                value = query,

                // 사용자가 글자를 입력할 때마다 콜백이 호출되고 it에 새로운 텍스트가 들어옴,
                // 그걸 query에 다시 넣어서 상태를 업데이트함
                onValueChange = { query = it },

                // 아무것도 입력되지 않았을 때 안내 문구
                placeholder = { Text("검색어를 입력하세요.") },

                // 텍스트 필드 왼쪽(앞쪽)에 들어갈 아이콘 지정
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "검색 아이콘",
                        tint = Color.DarkGray
                    )
                },

                // 입력이 한 줄만 가능하게 함, 엔터를 쳐도 줄바꿈 X 한 줄에 계속 입력
                singleLine = true,

                // 검색창 디자인 설정
                modifier = Modifier
                    .width(245.dp)
                    .height(56.dp),

                // 둥근 모서리
                shape = RoundedCornerShape(20.dp),

                // 색상 설정
                colors = TextFieldDefaults.colors(
                    // 선택되었을 때 배경색
                    focusedContainerColor = Color(0xFFF2F2F2),
                    // 선택 안 되었을 때 배경색
                    unfocusedContainerColor = Color(0xFFF2F2F2),
                    // 비활성화 상태 배경색
                    disabledContainerColor = Color(0xFFF2F2F2),

                    // 밑줄을 모두 투명으로 해서 보이지 않게 만듦
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            // space between Search bar & Bookmark Button
            Spacer(modifier = Modifier.width(10.dp))

            // Bookmark Button
            Icon(
                // 북마크 벡터 아이콘 가지고 오기
                painter = painterResource(id = R.drawable.icon_bookmark),
                contentDescription = "Bookmark Icon",

                // 버튼 색상
                tint = Color.Black,

                // 버튼 디자인
                modifier = Modifier
                    .size(56.dp)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .clickable {    // Icon 요소를 클릭할 수 있게 만들어 줌
                        println("Bookmark clicked")
                    }
            )

            Spacer(modifier = Modifier.width(1.dp))

            // Notifications Button
            Icon(
                // 알림 벡터 아이콘 가지고 오기
                painter = painterResource(id = R.drawable.icon_notification),
                contentDescription = "Alert Icon",

                // 버튼 색상
                tint = Color.Black,

                // 버튼 디자인
                modifier = Modifier
                    .size(56.dp)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .clickable {    // Icon 요소를 클릭할 수 있게 만들어 줌
                        println("Notifications clicked")
                    }
            )
        }

        // 검색창 아래 여백
        Spacer(modifier = Modifier.height(20.dp))

        // 시간대 가져오기 (remember -> 재계산 방지)
        val zone = remember { ZoneId.systemDefault() }

        val nearestTrip by remember {
            // 내부에서 사용하는 값이 바뀌면 자동으로 다시 계산해 줌
            derivedStateOf {
                val today = LocalDate.now()     // 오늘 날짜

                // 각 여행에 대해 D-Day 계산
                tripsVm.trips
                    .map { trip ->
                        // D-Day 계산
                        val targetDate = Instant.ofEpochMilli(trip.targetDateMillis)
                            .atZone(zone)
                            .toLocalDate()
                        // 남은 일수 계산
                        trip to ChronoUnit.DAYS.between(today, targetDate).toInt()
                    }
                    // 이미 지난 여행은 제외
                    .filter { (_, diffDays) -> diffDays >= 0 }
                    // D-Day 가장 작은 것 (== 출국일이 가장 빠른 여행 설정)
                    .minByOrNull { (_, diffDays) -> diffDays }
            }
        }

        // D-Day & Weather Button
        Box(
            // 최대 사이즈
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                // 세로 사이즈 설정 및 구조 설정
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    // 디데이 + 날씨 카드
                    // 가로 사이즈 설정 및 구조 설정
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ){
                    Column(
                        // 디데이
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .width(100.dp)
                            .padding(5.dp)
                    ){
                        // 위에서 받은 nearestTrip이 존재하면 (최소 1개 존재)
                        if (nearestTrip != null) {
                            Text(
                                // 위에 출국까지 텍스트 존재
                                text = "출국까지",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )

                            // D-Day 문자열 만들기
                            // nearestTrip?.second = (Trip, Int) 형태의 쌍으로
                            // .second로 남은 일수 갖고 오기
                            val dText = nearestTrip?.second?.let { "D-$it" } ?: "-"
                            Text(
                                text = dText,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        } else {
                            // 여행이 없을 때, 기본 문구 수정
                            Text(
                                text = "여행을 \n추가해 주세요! 😎",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                        }
                    }

                    // 공백
                    Spacer(modifier = Modifier.width(30.dp))

                    // 날씨 API에 보낼 도시 문자열 만들기
                    val weatherQuery = nearestTrip?.first?.country?.let { cityToWeatherQuery(it) }

                    // 날씨 API 호출
                    // weatherQuery가 처음 생기거나 변경될 때만 이 블록이 실행됨
                    LaunchedEffect(weatherQuery) {
                        if (weatherQuery != null) {
                            weatherViewModel.load(weatherQuery)
                        }
                    }

                    // 날씨 카드 배경 이미지 결정
                    val weatherImageRes =
                        nearestTrip?.first?.country?.let { coverResForCountry(it) } ?: R.drawable.default_weather

                    // 날씨 미리보기 카드 UI 렌더링
                    WeatherPreviewCard(
                        // API 응답 시 ?: 응답 전 표시
                        temperature = weatherViewModel.temperature ?: "...",

                        // 계산한 배경 이미지 사용
                        imageRes = weatherImageRes,

                        // 날씨 별 아이콘 가지고 오기
                        iconRes = weatherViewModel.iconCode?.let { mapWeatherIcon(it) },

                        // 누르면 날씨 페이지로 이동
                        onClick = { navController.navigate("weather") }
                    )
                }

                // 공백
                Spacer(modifier = Modifier.height(0.dp))

                // "예정된 여행 >" 버튼
                Button(
                    // 누르면 여행 등록 및 관리 페이지로 이동
                    onClick = { navController.navigate("plans") },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .width(125.dp),

                    // 버튼 색상
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Black
                    ),

                    // 내부 패딩 제거
                    contentPadding = PaddingValues(0.dp)
                ) {
                    // 버튼 안의 내용
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(110.dp)
                    ) {
                        // 버튼 텍스트
                        Text(
                            text = " 예정된 여행",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // 공백
                        Spacer(modifier = Modifier.width(3.dp))

                        // 화살표 아이콘
                        Icon(
                            imageVector = Icons.Rounded.ArrowForwardIos,
                            contentDescription = "Arrow Icon",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(20.dp)
                                .offset(y = 1.dp)
                        )
                    }
                }

                // 공백
                Spacer(modifier = Modifier.height(3.dp))

                // TripsViewModel이 관리하는 여행 리스트
                val trips = tripsVm.trips

                // 홈 화면 예정된 여행 카드 영역
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { navController.navigate("plans") }
                        .background(Color(0xFFF5F5F5))
                        .padding(10.dp)
                ) {
                    // 스크롤 가능
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 여행이 하나도 없을 때 "예정된 여행이 없습니다." 출력
                        if (trips.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(270.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "예정된 여행이 없습니다.",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        } else {    // 여행이 하나 이상 있을 때
                            items(trips) { trip ->
                                // 여행 카드 표시
                                TravelPlanCard(
                                    cityName = trip.country,
                                    flagText = trip.countryEmoji,
                                    imageRes = coverResForCountry(trip.country) ?: R.drawable.default_weather
                                )
                            }
                        }
                    }
                }

                // 짐 체크리스트 >
                Button(
                    onClick = { navController.navigate("checklist") },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .width(140.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Black
                    ),

                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(137.dp)
                    ) {
                        Text(
                            text = "  짐 체크리스트",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.width(3.dp))

                        Icon(
                            imageVector = Icons.Rounded.ArrowForwardIos,
                            contentDescription = "Arrow Icon",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(20.dp)
                                .offset(y = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(1.dp))

                // 짐 체크리스트 미리보기 카드
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            // 체크리스트 화면으로 넘어감
                            onClick = { navController.navigate("checklist") }
                        )
                ) {
                    // checklist 카드 표시
                    // 기본 안내 문구만 표시
                    ChecklistHintCard(
                        text = "빠진 짐은 없는지 확인해 볼까요?",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 오늘의 문장
                Box(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "  오늘의 문장",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(5.dp))

                            Text(
                                // 말풍선 이모지
                                text = "\uD83D\uDCAC",
                                fontSize = 18.sp,
                                modifier = Modifier.offset(y = 1.5.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))

                        // 오늘의 문장 카드 표시
                        TodaySentenceSection()
                    }
                }
            }
        }
    }
}

@Composable
// 날씨 카드 만들기
fun WeatherPreviewCard(
    // 카드에 표시할 것들 설정
    temperature: String,
    imageRes: Int,
    iconRes: Int?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .background(Color(0xFFF5F5F5))
    ) {

        // 카드 배경 이미지 가지고 오기
        Image(
            // 전달받은 도시 이미지 리소스 로드
            painter = painterResource(id = imageRes),
            contentDescription = "weather background",
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp))
                .alpha(0.5f),
            contentScale = ContentScale.Crop
        )

        // 정보 표시 (날씨 아이콘 + 온도)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .wrapContentSize()
                .padding(end = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // iconRes가 없는 경우
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = "weather icon",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(40.dp)
                )
            }

            // 온도 텍스트
            Text(
                text = temperature,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            )
        }
    }
}

// 앱에서 사용하는 도시 이름 -> 날씨 API에서 요구하는 도시 형식(영문 + 국가코드)
// API가 한글을 인식하지 못하기 때문에 영어로 변경
private fun cityToWeatherQuery(city: String): String =
    // city.trim() = 문자열 앞뒤 공백 제거
    // 도시 이름에 따라 다른 문자열 반환
    when (city.trim()) {
        "삿포로" -> "Sapporo,jp"
        "런던" -> "London,uk"
        "뉴욕" -> "New York,us"
        "빈" -> "Vienna,at"
        else -> city.trim()
    }

@Composable
// 여행 카드 UI 그리기
fun TravelPlanCard(
    cityName: String,
    flagText: String,
    imageRes: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(360.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF5F5F5))
    ) {
        // 배경 이미지
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "$cityName image",
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(20.dp))
                .alpha(0.5f),
            // 이미지 비율 유지하면서 꽉 차게 자르기
            contentScale = ContentScale.Crop
        )

        // 위에 아이콘 + 도시명/국기 이미지 위에 오버레이
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {

            Spacer(modifier = Modifier.weight(1f))

            // 오른쪽에 도시명 + 국기
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 국기
                Text(
                    text = flagText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.width(6.dp))

                // 도시명
                Text(
                    text = cityName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
// 짐 체크리스트 안내 화면 (초기 화면 = 항상 일정한 상태)
fun ChecklistHintCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF2F2F2)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF424242)
        )
    }
}

@Composable
// 오늘의 문장 카드 UI
fun TodaySentenceCard(
    // 문장 한 개만 보여 줌
    sentence: TodaySentence,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF5F5F5))
    ) {
        Row(
            // 외국어 + 번역 가로 배치
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                // 외국어
                Text(
                    text = sentence.foreign,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // 발음 (알파벳으로)
                Text(
                    text = sentence.romanization,
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }

            // 한국어 번역
            Text(
                text = sentence.translation,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
// 파이어베이스 데이터 로드
fun TodaySentenceSection() {
    var randomSentence by remember { mutableStateOf<TodaySentence?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // 일본어 고정
    val language = StudyLanguage.JAPANESE

    // 데이터 로딩
    LaunchedEffect(language) {
        isLoading = true
        error = null

        try {   // 문장 가져오기
            val list = loadSentencesFromFirestore(language)
            randomSentence = if (list.isNotEmpty()) list.random() else null
        } catch (e: Exception) {    // 에러 처리
            e.printStackTrace()
            error = "문장을 불러오는 중 오류가 발생했습니다."
        } finally {     // 로딩 종료
            isLoading = false
        }
    }

    Spacer(modifier = Modifier.height(5.dp))

    // UI 상태 분기
    when {
        // 로딩 중
        isLoading -> {
            Text("불러오는 중입니다...", fontSize = 12.sp, color = Color.Gray)
        }
        // 에러 발생
        error != null -> {
            Text(error ?: "", fontSize = 12.sp, color = Color.Red)
        }
        // 정상 로딩 성공 시
        randomSentence != null -> {
            TodaySentenceCard(
                sentence = randomSentence!!,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        // 데이터 없을 때
        else -> {
            Text("등록된 문장이 없습니다.", fontSize = 12.sp, color = Color.Gray)
        }
    }
}