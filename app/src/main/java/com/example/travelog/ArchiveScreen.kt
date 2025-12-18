package com.example.travelog

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.io.File

/* UI 파일
 1. ViewModel에서 state 가져오기
 2. 상단 UI(탭, 드롭다운)
 3. 사진 그리드
 4. 사진 클릭 시 오버레이 연결
 */
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    navController: NavController, //화면 이동
    cityList: List<String> = listOf("빈", "런던", "삿포로"),
    onGoPlannedTrips: () -> Unit = {} //예정된 여행 이동시 nav 거쳐서 이동
) {
    //화면 내부에서 쓰는 UI의 state
    // (Compose의 remember로 유지되는 값)
    val selectedTabIndex = 1 //예정된 여행 말고 지난 여행을 기본값
    var expanded by remember { mutableStateOf(false) } //드롭다운 열림,닫힘

    // 1. ViewModel 연결 (사진/댓글/선택도시)
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: ArchiveViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )

    // 2. ViewModel 상태 수집 (Flow/StateFlow -> Compose State)
    val selectedCity by vm.selectedCity.collectAsState() //값이 바뀌면
    val photoList by vm.photos.collectAsState() // 자동으로 ui 변경-collectasstate

    // 그리드에 보여줄 사진만 골라서 사용 - 빈 데이터 사용 방지
    val displayPhotoList = remember(photoList) {
        photoList.filter { it.internalPath.isNotBlank() }
    }

    // 최초 진입할 떄, 선택 도시가 비어 있으면 첫 도시로 기본값 세팅
    LaunchedEffect(cityList) {
        if (cityList.isNotEmpty() && selectedCity.isBlank()) {
            vm.setSelectedCity(cityList.first())
        }
    }

    // 3. 오버레이의 UI 상태
    var overlayOpen by remember { mutableStateOf(false) }
    var selectedPhotoId by remember { mutableStateOf<Long?>(null) }
    var commentInput by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }

    // 선택된 사진의 댓글 목록 / ViewModel에서 가져오기
    val comments by vm.comments.collectAsState()

    // 4. 사진 추가: 포토피커 런처 (에뮬에서 사진 받아오기)
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent() //에뮬에서 선택한 사진 uri 받아오기
    ) { uri: Uri? ->
        if (uri != null) vm.addPickedPhoto( uri) //받아온 uri vm에 넘기기
    }

    // 5. 메인 UI 구성
    //topbar - tab - divider - dropdown - grid
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            ArchiveTopBar(
                onBookmarkClick = { /* 저장 */ },
                onNotificationClick = { /* 알림 */ }
            )

            Spacer(modifier = Modifier.height(2.dp))

            ArchiveTabs(
                selectedTabIndex = selectedTabIndex,
                onSelectTab = { index ->
                    if (index == 0) { //0=예정된 여행, 1=지난여행
                        onGoPlannedTrips() //0만 다른 화면 이동 처리
                    }
                }
            )

            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5E7EB))

            Spacer(modifier = Modifier.height(14.dp))

            ArchiveCityDropdown(
                cityList = cityList,
                selectedCity = selectedCity,
                expanded = expanded,
                onToggleExpanded = { expanded = !expanded },
                onSelectCity = { city ->
                    vm.setSelectedCity(city)
                    expanded = false
                },
                onDismiss = { expanded = false }
            )

            // 사진 3열 그리드 (+ 버튼 까지)
            ArchivePhotoGrid(
                photoList = displayPhotoList,
                onPhotoClick = { item ->
                    selectedPhotoId = item.id.toLong()
                    vm.setSelectedPhoto(item.id.toLong()) //댓글 로딩 트리서

                    // 오버레이는 내부 파일 path로 넘김
                    selectedPhotoUri = item.internalPath

                    overlayOpen = true //오버레이 표시
                },
                onPlusClick = {
                    pickImageLauncher.launch("image/*") //이미지 선택만 허용
                }
            )
        }

        // 6. 사진 오버레이 (visible=true일 때만 보이게 처리) 정의는 오버레이 파일
        ArchivePhotoOverlay(
            visible = overlayOpen,
            photoUri = selectedPhotoUri, //오버레이 이미지 소스
            comments = comments,
            inputText = commentInput,
            onInputTextChange = { commentInput = it },
            onSend = { //빈 문자열 방지 후 vm에 저장
                val t = commentInput.trim()
                if (t.isNotEmpty()) {
                    vm.addComment(authorName = "me", text = t)
                    commentInput = ""
                }
            },
            onDismiss = { //닫을때 오버레이 상태 초기화
                overlayOpen = false
                selectedPhotoId = null
                selectedPhotoUri = null
                commentInput = ""
            }
        )
    }
}

//상단 타이틀, 저장/알림 아이콘
@Composable
private fun ArchiveTopBar(
    onBookmarkClick: () -> Unit,
    onNotificationClick: () -> Unit,
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
                .clickable { onBookmarkClick() }
        )

        Spacer(modifier = Modifier.width(1.dp))

        Icon(
            painter = painterResource(id = R.drawable.icon_notification),
            contentDescription = "알림",
            tint = Color.Black,
            modifier = Modifier
                .size(56.dp)
                .padding(10.dp)
                .clickable { onNotificationClick() }
        )
    }
}

// 예정된 여행 / 지난 여행 탭 영역
// 이 함수는 "탭 클릭 이벤트"만 올려보내고, 실제 화면 이동은 nav에서 처리
@Composable
private fun ArchiveTabs(
    selectedTabIndex: Int,
    onSelectTab: (Int) -> Unit,
) {
    val tabs = listOf("예정된 여행", "지난 여행")

    val indicatorHeight = 8.dp
    val indicatorWidth = 22.dp
    val corner = 5.dp

    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        edgePadding = 0.dp,
        containerColor = Color.White,
        divider = {},
        indicator = { tabPositions ->
            val current = tabPositions[selectedTabIndex]
            val offsetX = current.left + (current.width - indicatorWidth) / 2

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.BottomStart)
                    .offset(x = offsetX)
                    .width(indicatorWidth)
                    .height(indicatorHeight)
                    .clip(RoundedCornerShape(topStart = corner, topEnd = corner))
                    .background(Color.Black)
            )
        },
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = {
                    if (selectedTabIndex != index) onSelectTab(index)
                },
                text = {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = if (selectedTabIndex == index) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (selectedTabIndex == index) Color.Black else Color.Gray
                    )
                }
            )
        }
    }
}

//도시 선택 드롭다운
//선택된 도시(selectedCity)에 따라 사진 목록이 바뀌는 구조
@Composable
private fun ArchiveCityDropdown(
    cityList: List<String>,
    selectedCity: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectCity: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(start = 25.dp, top = 5.dp, bottom = 15.dp)
            .wrapContentSize(Alignment.TopStart)
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth(Alignment.Start)
                .clickable { onToggleExpanded() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedCity,
                fontSize = 18.sp,
                color = Color.Black,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = "드롭다운",
                tint = Color(0xFF111827),
                modifier = Modifier.size(22.dp)
            )
        }

        DropdownMenu( //드롭다운 열림,닫힘
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            cityList.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city) },
                    onClick = { onSelectCity(city) }
                )
            }
        }
    }
}

//사진 3열 그리드
//마지막 칸은 '+' 버튼(사진 추가 기능)
@Composable
private fun ArchivePhotoGrid(
    photoList: List<ArchivePhotoEntity>,
    onPhotoClick: (ArchivePhotoEntity) -> Unit,
    onPlusClick: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3), //3열 그리드
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(photoList) { item ->
            val showPath = item.internalPath

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF3F4F6))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                    .clickable { onPhotoClick(item) },
                contentAlignment = Alignment.Center
            ) {
                if (showPath.isNotBlank()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { c ->
                            ImageView(c).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                setBackgroundColor(android.graphics.Color.LTGRAY)
                            }
                        },
                        update = { iv ->
                            try {//내부 파일 경로인 file uri로드
                                iv.setImageURI(Uri.fromFile(File(showPath)))
                            } catch (t: Throwable) {
                                Log.w("ArchiveScreen", "Failed to load path=$showPath", t)
                                iv.setImageDrawable(null)
                            }
                        }
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(15.dp))
                    .clickable { onPlusClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", fontSize = 28.sp, color = Color(0xFF111827))
            }
        }
    }
}