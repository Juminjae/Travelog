package com.example.travelog

import android.annotation.SuppressLint
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    cityList: List<String> = listOf("빈", "런던", "삿포로")
) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(1) }
    var expanded by remember { mutableStateOf(false) }
    var selectedCity by remember { mutableStateOf(cityList.first()) }

    // --- overlay state ---
    var overlayOpen by remember { mutableStateOf(false) }
    var selectedPhotoId by remember { mutableStateOf<String?>(null) }
    var commentInput by remember { mutableStateOf("") }

    // 댓글(더미) — photoId별로 분리 저장
    val commentMap = remember { mutableStateMapOf<String, SnapshotStateList<PhotoComment>>() }

    fun commentsOf(photoId: String): SnapshotStateList<PhotoComment> =
        commentMap.getOrPut(photoId) { mutableStateListOf() }

    val photoMap = remember(cityList) {
        mapOf(
            "빈" to List(8) { "VIN_${it + 1}" },
            "런던" to List(6) { "LDN_${it + 1}" },
            "삿포로" to List(10) { "SPK_${it + 1}" }
        )
    }
    val photoList = photoMap[selectedCity].orEmpty()

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

            ArchiveTabs(
                selectedTabIndex = selectedTabIndex,
                onSelectTab = { selectedTabIndex = it }
            )

            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5E7EB))

            ArchiveCityDropdown(
                cityList = cityList,
                selectedCity = selectedCity,
                expanded = expanded,
                onToggleExpanded = { expanded = !expanded },
                onSelectCity = { city ->
                    selectedCity = city
                    expanded = false
                },
                onDismiss = { expanded = false }
            )

            ArchivePhotoGrid(
                photoList = photoList,
                onPhotoClick = { id ->
                    selectedPhotoId = id
                    overlayOpen = true
                },
                onPlusClick = { /* 이미지 업로드 확장 */ }
            )
        }

        // ===== Overlay =====
        val currentComments = selectedPhotoId?.let { commentsOf(it) } ?: mutableStateListOf()
        ArchivePhotoOverlay(
            visible = overlayOpen,
            // 지금은 더미 페인터(회색). 나중에 실제 이미지로 교체
            photo = ColorPainter(Color(0xFFE5E7EB)),
            comments = currentComments,
            inputText = commentInput,
            onInputTextChange = { commentInput = it },
            onSend = {
                val t = commentInput.trim()
                if (t.isNotEmpty() && selectedPhotoId != null) {
                    commentsOf(selectedPhotoId!!).add(PhotoComment(author = "me", text = t))
                    commentInput = ""
                }
            },
            onDismiss = {
                overlayOpen = false
                selectedPhotoId = null
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
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "내 여행",
            fontSize = 28.sp,
            color = Color(0xFF111827)
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onBookmarkClick) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = "저장",
                tint = Color(0xFF111827)
            )
        }
        IconButton(onClick = onNotificationClick) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "알림",
                tint = Color(0xFF111827)
            )
        }
    }
}

@Composable
private fun ArchiveTabs(
    selectedTabIndex: Int,
    onSelectTab: (Int) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        edgePadding = 0.dp,
        containerColor = Color.White,
        indicator = { tabPositions ->
            val currentTab = tabPositions[selectedTabIndex]
            val indicatorHeight = 6.dp
            val indicatorWidth = 20.dp
            val corner = 4.dp
            val indicatorColor = Color.Black
            val offsetX = currentTab.left + (currentTab.width - indicatorWidth) / 2

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.BottomStart)
                    .offset(x = offsetX, y = 0.dp)
                    .width(indicatorWidth)
                    .height(indicatorHeight)
                    .clip(RoundedCornerShape(topStart = corner, topEnd = corner))
                    .background(indicatorColor)
            )
        }
    ) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { onSelectTab(0) },
            text = { Text("예정된 여행") }
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { onSelectTab(1) },
            text = { Text("지난 여행") }
        )
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
            .padding(start = 20.dp, top = 5.dp, bottom = 15.dp)
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
                fontSize = 22.sp,
                color = Color(0xFF111827)
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
    photoList: List<String>,
    onPhotoClick: (String) -> Unit,
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
        items(photoList) { id ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                    .clickable { onPhotoClick(id) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = id, color = Color(0xFF6B7280), fontSize = 12.sp)
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                    .clickable { onPlusClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", fontSize = 28.sp, color = Color(0xFF111827))
            }
        }
    }
}