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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.TextFieldValue

import com.example.travelog.data.model.TodaySentence
import com.example.travelog.data.model.StudyLanguage
import com.example.travelog.data.loadSentencesFromFirestore
import com.example.travelog.data.model.mapWeatherIcon
import com.example.travelog.data.network.RetrofitClient

import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    navController: NavHostController,
    weatherViewModel: WeatherViewModel = viewModel()
) {
    // 검색창
    var query by remember { mutableStateOf("") }
//    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
//        mutableStateOf(TextFieldValue(""))
//    }
//    val keyboard = LocalSoftwareKeyboardController.current

    // 수직으로 정렬
    Column(
        // Background
        modifier = Modifier
            .fillMaxSize()    // 최대 사이즈 사용
            .background(Color.White)    // 배경 색상: 흰색
            .padding(horizontal = 20.dp, vertical = 10.dp)    // 가장자리 여백
    ) {
        // Search bar + Bookmark + Notification icons 수평 정렬
        Row(
            // Row 안에 들어가는 항목들을 세로 방향(위–아래 기준)으로 가운데 정렬
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()    // 최대 width 사용
        ) {
            // Search bar
            OutlinedTextField(    // 검색창 컴포넌트
                value = query,
                // 사용자가 글자를 입력할 때마다 콜백이 호출되고 it에 새로운 텍스트가 들어옴,
                // 그걸 query에 다시 넣어서 상태를 업데이트함
                onValueChange = { query = it },

                // 아무것도 입력되지 않았을 때 안내 문구, 기본 연한 회색
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
//                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
//                keyboardActions = KeyboardActions(
//                    onSearch = {
//                        keyboard?.hide()
//                        weatherViewModel.search(query.trim())
//                    }
//                ),

                modifier = Modifier
                    .width(245.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
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
                painter = painterResource(id = R.drawable.icon_bookmark),
                contentDescription = "Bookmark Icon",
                tint = Color.Black,
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
                painter = painterResource(id = R.drawable.icon_notification),
                contentDescription = "Alert Icon",
                tint = Color.Black,
                modifier = Modifier
                    .size(56.dp)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .clickable {
                        println("Notifications clicked")
                    }
            )
        }

        Spacer(modifier = Modifier.height(20.dp)) // 검색창 아래 여백

        // D-Day & Weather Button
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ){
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .width(100.dp)
                            .padding(5.dp)
                    ){
                        Text(
                            text = "출국까지",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        Text(
                            text = "D-74",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(30.dp))

                    // composable이 처음 등장할 때 딱 1번 실행하도록 보장
                    LaunchedEffect(Unit) {
                        weatherViewModel.load("Sapporo,jp")
                    }

                    WeatherPreviewCard(
                        temperature = weatherViewModel.temperature ?: "...",
                        imageRes = R.drawable.sapporo,
                        iconRes = weatherViewModel.iconCode?.let { mapWeatherIcon(it) },
                        onClick = {
                            // 누르면 해당 페이지로 이동
                            navController.navigate("weather")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(0.dp))

                // 예정된 여행 >
                Button(
                    onClick = { navController.navigate("plans") },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .width(125.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Black
                    ),

                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(110.dp)
                    ) {
                        Text(
                            text = " 예정된 여행",
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

                Spacer(modifier = Modifier.height(3.dp))

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
                        item {
                            TravelPlanCard(
                                cityName = "삿포로",
                                flagText = "\uD83C\uDDEF\uD83C\uDDF5",
                                imageRes = R.drawable.sapporo
                            )
                        }

                        item {
                            TravelPlanCard(
                                cityName = "런던",
                                flagText = "\uD83C\uDDEC\uD83C\uDDE7",
                                imageRes = R.drawable.london
                            )
                        }

                        item {
                            TravelPlanCard(
                                cityName = "뉴욕",
                                flagText = "\uD83C\uDDFA\uD83C\uDDF8",
                                imageRes = R.drawable.newyork
                            )
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            onClick = { navController.navigate("checklist") }
                        )
                ) {
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
                                text = "\uD83D\uDCAC",
                                fontSize = 18.sp,
                                modifier = Modifier.offset(y = 1.5.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))

                        TodaySentenceSection()
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherPreviewCardWithApi(
    city: String,   // 예: "Sapporo,jp"
    imageRes: Int,
    onClick: () -> Unit
) {
    var temp by remember { mutableStateOf<String?>(null) }
    var iconCode by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // API 호출
    LaunchedEffect(city) {
        try {
            error = null
            val result = RetrofitClient.weatherApi.getCurrentWeather(
                city = city,
                apiKey = BuildConfig.WEATHER_API_KEY
            )
            temp = "${result.main.temp.toInt()}°C"
            iconCode = result.weather.firstOrNull()?.icon
        } catch (e: Exception) {
            e.printStackTrace()
            error = e.message
        }
    }

    val iconRes = iconCode?.let { mapWeatherIcon(it) }

    WeatherPreviewCard(
        temperature = when {
            error != null      -> "--°C"
            temp == null -> "..."
            else -> temp!!
        },
        imageRes = imageRes,
        iconRes = iconRes,
        onClick = onClick
    )
}

@Composable
fun WeatherPreviewCard(
    temperature: String,
    imageRes: Int,
    iconRes: Int?,
    modifier: Modifier = Modifier,
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

        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "weather background",
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp))
                .alpha(0.5f),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .wrapContentSize()
                .padding(end = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = "weather icon",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = temperature,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            )
        }
    }
}


@Composable
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
            contentScale = ContentScale.Crop
        )

        // 위에 아이콘 + 도시명/국기 오버레이
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {

            Spacer(modifier = Modifier.weight(1f))

            // 오른쪽 도시명 + 국기
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = flagText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.width(6.dp))

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
fun TodaySentenceCard(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = sentence.foreign,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = sentence.romanization,
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }

            Text(
                text = sentence.translation,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TodaySentenceSection() {
    var randomSentence by remember { mutableStateOf<TodaySentence?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val language = StudyLanguage.JAPANESE

    LaunchedEffect(language) {
        isLoading = true
        error = null
        try {
            val list = loadSentencesFromFirestore(language)
            randomSentence = if (list.isNotEmpty()) list.random() else null
        } catch (e: Exception) {
            e.printStackTrace()
            error = "문장을 불러오는 중 오류가 발생했습니다."
        } finally {
            isLoading = false
        }
    }

    Spacer(modifier = Modifier.height(5.dp))

    when {
        isLoading -> {
            Text("불러오는 중입니다...", fontSize = 12.sp, color = Color.Gray)
        }
        error != null -> {
            Text(error ?: "", fontSize = 12.sp, color = Color.Red)
        }
        randomSentence != null -> {
            TodaySentenceCard(
                sentence = randomSentence!!,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        else -> {
            Text("등록된 문장이 없습니다.", fontSize = 12.sp, color = Color.Gray)
        }
    }
}