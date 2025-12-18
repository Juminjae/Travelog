package com.example.travelog

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
import android.net.Uri
import android.widget.ImageView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.util.Log

//사진 오버레이(확대 보기), 댓글 영역
//배경(어두운 영역) 터치 or X 버튼 → 닫기(onDismiss)
//댓글은 최대 높이 안에서 스크롤, 댓글 추가시 최근 댓글로 스크롤
//실제 댓글 추가/저장은 ViewModel에서 관리
//아카이브스크린에서 호출
@Composable
fun ArchivePhotoOverlay(
    visible: Boolean, //오버레이 띄울지 말지
    photoUri: String?, //그리드에서 선택 안 했거나, 실패했을때 고려
    comments: List<ArchiveCommentEntity>, //ArchivePhotoEntity에서 엔티티 정의
    inputText: String,
    onInputTextChange: (String) -> Unit, //오버레이에 상태 저장이 아닌 vm에서 끌어올 예정
    onSend: () -> Unit,
    onDismiss: () -> Unit, //오버레이 닫기
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    //ui상수
    val overlayAlpha = 0.55f
    val cardCorner = 18.dp
    val photoHeight = 420.dp
    val commentsBoxHeight = 160.dp

    // 1. 오버레이 바깥 어둡게 그리고 가운데에 카드 위치
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = overlayAlpha))//바깥
            // 바깥 영역 터치 시 닫기
            .clickable(
                indication = null, //눌렀을때 물결 해결
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            )
    ) {
        //사진 카드
        Card(
            shape = RoundedCornerShape(cardCorner),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = cardCorner)
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {/* 클릭해도 아무 일 x */}
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                //2. 사진 영역 (우상단 X 포함)

                // 사진, x버튼
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(photoHeight)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                ) {
                    if (photoUri != null) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    scaleType = ImageView.ScaleType.CENTER_CROP
                                    setBackgroundColor(android.graphics.Color.LTGRAY)
                                }
                            },
                            update = { iv ->
                                try { //uri 깨져도 앱 꺼지지 않게
                                    // photoUri는 에뮬에서 업로드한 주소
                                    val uri = Uri.parse(photoUri)
                                    iv.setImageURI(uri)
                                } catch (t: Throwable) {
                                    Log.w("ArchivePhotoOverlay", "Failed to load photo uri", t)
                                    iv.setImageDrawable(null)
                                }
                            }
                        )
                    }

                    // 우상단 X
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "close overlay",
                            tint = Color.Black
                        )
                    }
                }

                //3. 댓글(새 댓글 달리면 자동 스크롤)
                val listState = rememberLazyListState() //스크롤 위치 제어하기 위한 상태

                //자동 스크롤
                LaunchedEffect(comments.size) { //댓글 개수 바뀔때
                    if (comments.isNotEmpty()) {
                        listState.animateScrollToItem(comments.lastIndex)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(commentsBoxHeight) //댓글 늘어나도 오버레이 크기 고정
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
                                // 프로필
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                )
                                Spacer(Modifier.width(10.dp))

                                Text(
                                    text = "${c.authorName}  ${c.text}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                //4. 댓글 입력 + 전송 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 10.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) { //텍스트를 오버레이에 저장이 아닌 밖에서 내려받기
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