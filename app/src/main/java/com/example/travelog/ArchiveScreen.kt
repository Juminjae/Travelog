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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelog.ArchivePhotoEntity
import java.io.File
import androidx.navigation.NavController

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    navController: NavController,
    cityList: List<String> = listOf("빈", "런던", "삿포로"),
    onGoPlannedTrips: () -> Unit = {}
) {
    val selectedTabIndex = 1
    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: ArchiveViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )


    val selectedCity by vm.selectedCity.collectAsState()
    val photoList by vm.photos.collectAsState()

    val displayPhotoList = remember(photoList) {
        photoList.filter { item ->
            !item.uriString.isNullOrBlank() || !item.localResName.isNullOrBlank()
        }
    }

    LaunchedEffect(cityList) {
        if (cityList.isNotEmpty() && selectedCity.isBlank()) {
            vm.setSelectedCity(cityList.first())
        }
    }

    // 오버레아 상태
    var overlayOpen by remember { mutableStateOf(false) }
    var selectedPhotoId by remember { mutableStateOf<Long?>(null) }
    var commentInput by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }

    val comments by vm.comments.collectAsState()

    // 갤러리(사진) 선택
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val m = vm::class.java.methods.firstOrNull { it.name == "addUriPhoto" && it.parameterTypes.size == 1 && it.parameterTypes[0] == Uri::class.java }
                if (m != null) {
                    m.invoke(vm, uri)
                } else {
                    vm.addUriPhoto(uri.toString())
                }
            } catch (t: Throwable) {
                vm.addUriPhoto(uri.toString())
            }
        }
    }

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
                onBookmarkClick = { /* 저장 연결 */ },
                onNotificationClick = { /* 알림 연결 */ }
            )

            Spacer(modifier = Modifier.height(2.dp))

            ArchiveTabs(
                selectedTabIndex = selectedTabIndex,
                onSelectTab = { index ->
                    if (index == 0) { //0=예정된 여행, 1=지난여행
                        onGoPlannedTrips()
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

            ArchivePhotoGrid(
                photoList = displayPhotoList,
                onPhotoClick = { item ->
                    selectedPhotoId = item.id.toLong()
                    vm.setSelectedPhoto(item.id.toLong())

                    // 오버레이는 String URI로 통일해서 넘김
                    selectedPhotoUri = when {
                        !item.uriString.isNullOrBlank() -> {
                            val s = item.uriString!!
                            when {
                                // 내부 저장소 absolute path면 그대로 넘김 (Overlay에서 File로 처리)
                                s.startsWith("/") -> s
                                // Photo Picker 임시 URI는 재실행 시 권한이 사라질 수 있어 크래시 유발 → 차단
                                s.startsWith("content://media/picker_get_content") -> null
                                else -> s
                            }
                        }
                        !item.localResName.isNullOrBlank() -> {
                            val resId = context.resources.getIdentifier(
                                item.localResName,
                                "drawable",
                                context.packageName
                            )
                            if (resId != 0) {
                                "android.resource://${context.packageName}/$resId"
                            } else null
                        }
                        else -> null
                    }

                    overlayOpen = true
                },
                onPlusClick = {
                    pickImageLauncher.launch("image/*")
                }
            )
        }

        // 오버레이
        ArchivePhotoOverlay(
            visible = overlayOpen,
            photoUri = selectedPhotoUri,
            comments = comments,
            inputText = commentInput,
            onInputTextChange = { commentInput = it },
            onSend = {
                val t = commentInput.trim()
                if (t.isNotEmpty()) {
                    vm.addComment(authorName = "me", text = t)
                    commentInput = ""
                }
            },
            onDismiss = {
                overlayOpen = false
                selectedPhotoId = null
                selectedPhotoUri = null
                commentInput = ""
            }
        )
    }
}

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

        DropdownMenu(
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

@Composable
private fun ArchivePhotoGrid(
    photoList: List<ArchivePhotoEntity>,
    onPhotoClick: (ArchivePhotoEntity) -> Unit,
    onPlusClick: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(photoList) { item ->
            val ctx = LocalContext.current
            val showUri: String? = when {
                !item.uriString.isNullOrBlank() -> {
                    val s = item.uriString!!
                    when {
                        s.startsWith("/") -> s // internal file absolute path
                        s.startsWith("content://media/picker_get_content") -> null // block temp picker uri
                        else -> s
                    }
                }
                !item.localResName.isNullOrBlank() -> {
                    val resId = ctx.resources.getIdentifier(item.localResName, "drawable", ctx.packageName)
                    if (resId != 0) "android.resource://${ctx.packageName}/$resId" else null
                }
                else -> null
            }

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
                if (showUri != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { c ->
                            ImageView(c).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                setBackgroundColor(android.graphics.Color.LTGRAY)
                            }
                        },
                        update = { iv ->
                            try {
                                if (showUri.startsWith("android.resource://")) {
                                    val resId = showUri.substringAfterLast('/').toIntOrNull()
                                    if (resId != null) {
                                        iv.setImageResource(resId)
                                    } else {
                                        iv.setImageDrawable(null)
                                    }
                                } else {
                                    val uri = if (showUri.startsWith("/")) {
                                        Uri.fromFile(File(showUri))
                                    } else {
                                        Uri.parse(showUri)
                                    }
                                    iv.setImageURI(uri)
                                }
                            } catch (se: SecurityException) {
                                Log.w("ArchiveScreen", "No permission to open uri=$showUri", se)
                                iv.setImageDrawable(null)
                            } catch (t: Throwable) {
                                Log.w("ArchiveScreen", "Failed to load uri=$showUri", t)
                                iv.setImageDrawable(null)
                            }
                        }
                    )
                } else { }
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