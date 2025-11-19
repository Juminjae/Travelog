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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha

import com.example.travelog.model.TodaySentence
import com.example.travelog.model.StudyLanguage
import com.example.travelog.data.loadSentencesFromFirestore

@Composable
fun HomeScreen(
    navController: NavHostController
) {
    // 검색창
    var query by remember { mutableStateOf("") }

    Column(
        // Background
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)    // 배경 색상: 흰색
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        // Search bar + Bookmark + Notification icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("검색어를 입력하세요.") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "검색 아이콘",
                        tint = Color.DarkGray
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .width(245.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF2F2F2),
                    unfocusedContainerColor = Color(0xFFF2F2F2),
                    disabledContainerColor = Color(0xFFF2F2F2),
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

                    WeatherPreviewCard(
                        temperature = "14℃",
                        imageRes = R.drawable.sapporo,
                        onClick = {
                            navController.navigate("weather")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(0.dp))

                // 예정된 여행 >
                Button(
                    onClick = { navController.navigate("plans") },
                    modifier = Modifier
                        .fillMaxWidth(),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Black
                    ),

                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "예정된 여행",
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
                                .offset(y = 1.5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                TravelPlanCard(
                    cityName = "삿포로",
                    flagText = "\uD83C\uDDEF\uD83C\uDDF5",          // 🇯🇵
                    imageRes = R.drawable.sapporo
                )

                Spacer(modifier = Modifier.height(5.dp))

                TravelPlanCard(
                    cityName = "런던",
                    flagText = "\uD83C\uDDEC\uD83C\uDDE7",          // 🇬🇧
                    imageRes = R.drawable.london
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 짐 체크리스트 >
                Button(
                    onClick = { navController.navigate("checklist") },
                    modifier = Modifier
                        .fillMaxWidth(),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Black
                    ),

                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "짐 체크리스트",
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

                ChecklistHintCard(
                    text = "빠진 짐은 없는지 확인해 볼까요?",
                    modifier = Modifier
                        .padding(top = 4.dp)
                )

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
                                text = "오늘의 문장",
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
fun WeatherPreviewCard(
    temperature: String,
    imageRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit   // 버튼처럼 동작하게!
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }   // 🔥 버튼 기능 추가
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.WbSunny,
                contentDescription = "sun icon",
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = temperature,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

@Composable
fun TravelPlanCard(
    cityName: String,
    flagText: String,
    imageRes: Int,              // 배경 이미지
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(125.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF5F5F5))
    ) {
        // 🔹 배경 이미지
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