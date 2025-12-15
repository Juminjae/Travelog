package com.example.travelog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelog.data.model.ChecklistItem
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState

@Composable
fun ChecklistScreen(
    checklistViewModel: ChecklistViewModel = viewModel()
) {
    val items by checklistViewModel.items.collectAsState()
    var newItemText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
    ) {
        // 상단 제목
        Text(
            text = "짐 체크리스트",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 입력 + 추가 버튼
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { text ->
                    newItemText = text },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
                placeholder = { Text(" 어떤 짐을 챙겨 볼까요?") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    checklistViewModel.addItem(newItemText)
                    newItemText = ""
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Black,
                    disabledContentColor = Color.Black
                ),
                modifier = Modifier
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        color = Color.Gray,
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Text(
                    text = "+",
                    fontSize = 35.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 체크리스트 목록
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(items, key = { it.id }) { item ->
                ChecklistRow(
                    item = item,
                    onCheckedChange = { checked ->
                        checklistViewModel.toggleChecked(item.id, checked)
                    },
                    onRemoveClick = {
                        checklistViewModel.removeItem(item.id)
                    }
                )
            }
        }
    }
}

@Composable
fun ChecklistRow(
    item: ChecklistItem,
    onCheckedChange: (Boolean) -> Unit,
    onRemoveClick: () -> Unit
) {
    // 체크 상태에 따른 색상 변경
    val bgColor = if (item.isChecked) Color(0xFFA7C7E7) else Color(0xFFE3F2FD)
    val textColor = Color.Black

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(24.dp))
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onCheckedChange
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = item.label,
                fontSize = 15.sp,
                color = textColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            // 오른쪽 정렬 삭제 아이콘
            Icon(
                painter = painterResource(id = R.drawable.icon_trash),
                contentDescription = "삭제",
                tint = Color.Black,
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = (-6).dp)
                    .clickable { onRemoveClick() }
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
}