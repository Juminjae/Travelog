package com.example.travelog

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 사진 확대 + 댓글 오버레이
 * - X 버튼 또는 바깥(어두운 영역) 터치 시 닫힘
 * - 카드 내부(사진/댓글) 터치는 닫힘 트리거가 되지 않도록 이벤트를 "소비"함
 */
@Composable
fun ArchivePhotoOverlay(
    visible: Boolean,
    photo: Painter,
    comments: List<PhotoComment>,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    // 1) 전체 화면 오버레이(스크림)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            // 바깥 영역 터치 시 닫기
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            )
    ) {
        // 2) 중앙 카드(여기 터치해도 닫히지 않게 "소비")
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                // 중요: 카드 내부 클릭은 닫힘으로 전파되지 않게 이벤트 소비
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { /* consume */ }
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ===== 상단: 사진 + 닫기 버튼 =====
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 피그마 느낌: 사진이 꽤 크게(필요하면 숫자 조절)
                        .height(420.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                ) {
                    Image(
                        painter = photo,
                        contentDescription = "selected photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 우상단 X
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "close overlay",
                            tint = Color.Black
                        )
                    }
                }

                // ===== 중단: 댓글 목록(4개 높이 고정 + 초과 시 스크롤) =====
                // 4개 댓글이 보이는 높이로 고정. (필요하면 숫자만 조절하면 됨)
                val commentsBoxHeight = 160.dp
                val listState = rememberLazyListState()

                LaunchedEffect(comments.size) {
                    if (comments.isNotEmpty()) {
                        listState.animateScrollToItem(comments.lastIndex)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(commentsBoxHeight)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(comments) { c ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 프로필 동그라미(임시)
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                )
                                Spacer(Modifier.width(10.dp))

                                Text(
                                    text = "${c.author}  ${c.text}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // ===== 하단: 입력창 + 전송 버튼 =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 10.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        placeholder = { Text("댓글 입력") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "send comment"
                        )
                    }
                }
            }
        }
    }
}

data class PhotoComment(
    val author: String,
    val text: String,
)