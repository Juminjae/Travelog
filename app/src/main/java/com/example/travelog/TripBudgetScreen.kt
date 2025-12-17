package com.example.travelog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max






enum class Category(val label: String) {
    FLIGHT("항공권"),
    HOTEL("숙소비"),
    FOOD("식비"),
    SOUVENIR("기념품"),
    OTHER("기타")
}

enum class PayMethod(val label: String) {
    CARD("카드"),
    CASH("현금")
}

data class Expense(
    val dateMillis: Long,
    val category: Category,
    val pay: PayMethod,
    val amount: Int
)

// ==============================
// Screen
// ==============================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripBudgetScreen(
    tripTitle: String,
    onBack: () -> Unit,
) {
    // ✅ 총 예산 입력 (초기값 0 = 빈칸)
    var totalBudgetText by remember { mutableStateOf("") }
    val totalBudget = totalBudgetText.filter { it.isDigit() }.toIntOrNull() ?: 0

    // ✅ 지출 상태(초기 빈 리스트)
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }

    // ✅ 사용/잔액
    val used = remember(expenses) { expenses.sumOf { it.amount } }
    val remaining = totalBudget - used

    // ✅ 카테고리 합계(파이차트)
    val categoryTotals = remember(expenses) {
        Category.entries.associateWith { c ->
            expenses.filter { it.category == c }.sumOf { it.amount }
        }
    }

    // ✅ 날짜별(하루) 카테고리 합계(막대그래프)
    val dailyCategory = remember(expenses) { buildDailyCategoryTotals(expenses) }
    val dailyDates = remember(dailyCategory) { dailyCategory.keys.sorted() }
    val maxDaily = remember(dailyCategory) {
        dailyCategory.values.maxOfOrNull { map -> map.values.sum() } ?: 0
    }

    // ✅ 결제수단 합계(카드/현금)
    val cardSum = remember(expenses) { expenses.filter { it.pay == PayMethod.CARD }.sumOf { it.amount } }
    val cashSum = remember(expenses) { expenses.filter { it.pay == PayMethod.CASH }.sumOf { it.amount } }

    // ✅ 지출 추가 다이얼로그 상태
    var showAdd by remember { mutableStateOf(false) }

    if (showAdd) {
        AddExpenseDialog(
            onDismiss = { showAdd = false },
            onSave = { e ->
                expenses = expenses + e
                showAdd = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                title = { Text("여행 비용", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            )
        },
//        bottomBar = { BottomNavBar() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // [0] 총예산 입력
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("총 예산", fontWeight = FontWeight.SemiBold)
                        TextField(
                            value = totalBudgetText,
                            onValueChange = { totalBudgetText = it },
                            singleLine = true,
                            placeholder = { Text("예: 1500000") },
                            supportingText = { Text("숫자만 입력해주세요") }
                        )
                    }
                }
            }

            // [1] 예산 카드 + 파이차트 (지출 추가 시 변동)
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            StatRow("총 예산", totalBudget)
                            StatRow("사용 금액", used)
                            StatRow("잔액", remaining)
                        }
                        PieChart(
                            data = Category.entries.map { (categoryTotals[it] ?: 0).toFloat() },
                            labels = Category.entries.map { it.label },
                            size = 120.dp,
                            colors = Category.entries.map { categoryColor(it) }
                        )
                    }
                }
            }

            // [2] 환율 정보 (일단 고정)
            item {
                ExpandableSection(title = "환율 정보") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text("현재 환율: 1 JPY ≈ 9.2 KRW")
                    }
                }
            }

            // [3] 지출 내역 (지출 추가 버튼 + 막대그래프)
            item {
                ExpandableSection(title = "지출 내역") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = { showAdd = true }) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("지출 추가")
                            }
                        }

                        if (expenses.isEmpty()) {
                            Text(
                                "아직 지출 내역이 없어요.\n'지출 추가' 버튼으로 입력해주세요",
                                color = Color(0xFF777777)
                            )
                        } else {
                            // 지출 리스트
                            expenses
                                .sortedBy { it.dateMillis }
                                .forEach { ExpenseRow(it) }

                            Spacer(Modifier.height(8.dp))

                            // 날짜별 사용량 막대그래프 (카테고리 스택)
                            DailyStackedBarChart(
                                dates = dailyDates,
                                totalsByDate = dailyCategory,
                                maxY = max(maxDaily, 1),
                                colors = Category.entries.associateWith { categoryColor(it) }
                            )
                        }
                    }
                }
            }

            // [4] 결제 수단 (카드/현금 비율 그래프, 최대는 총 예산)
            item {
                ExpandableSection(title = "결제 수단") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (totalBudget <= 0) {
                            Text("총 예산을 먼저 입력해주세요", color = Color(0xFF777777))
                        }
                        PaymentRatioBar(
                            card = cardSum,
                            cash = cashSum,
                            maxBudget = max(totalBudget, 1)
                        )
                    }
                }
            }
        }
    }
}

// ==============================
// Add Expense Dialog (카테고리 + 결제수단 + 캘린더)
// ==============================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    var category by remember { mutableStateOf(Category.FOOD) }
    var pay by remember { mutableStateOf(PayMethod.CARD) }
    var amountText by remember { mutableStateOf("") }

    // 날짜: 캘린더 선택
    val zone = ZoneId.systemDefault()
    val todayMillis = remember {
        LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
    }
    var selectedDateMillis by remember { mutableStateOf(todayMillis) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val sel = datePickerState.selectedDateMillis
                    if (sel != null) selectedDateMillis = sel
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val dateLabel = remember(selectedDateMillis) {
        val d = Instant.ofEpochMilli(selectedDateMillis).atZone(zone).toLocalDate()
        "%02d/%02d".format(d.monthValue, d.dayOfMonth)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("지출 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // 날짜 선택
                Text("날짜", fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(dateLabel)
                }

                // 카테고리
                Text("카테고리", fontWeight = FontWeight.SemiBold)
                ChipRow(
                    items = Category.entries,
                    selected = category,
                    label = { it.label },
                    onSelect = { category = it }
                )

                // 결제 수단
                Text("결제 수단", fontWeight = FontWeight.SemiBold)
                ChipRow(
                    items = PayMethod.entries,
                    selected = pay,
                    label = { it.label },
                    onSelect = { pay = it }
                )

                // 금액
                Text("금액(원)", fontWeight = FontWeight.SemiBold)
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text("예: 12500") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amountText.filter { it.isDigit() }.toIntOrNull() ?: 0
                if (amt > 0) {
                    onSave(
                        Expense(
                            dateMillis = selectedDateMillis,
                            category = category,
                            pay = pay,
                            amount = amt
                        )
                    )
                }
            }) { Text("저장") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@Composable
private fun <T> ChipRow(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    val isSelected = item == selected
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) Color(0xFF3A6FF7) else Color(0xFFE9ECF3))
                            .clickable { onSelect(item) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label(item),
                            color = if (isSelected) Color.White else Color.Black,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// ==============================
// UI helpers
// ==============================

@Composable
private fun StatRow(label: String, amount: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Medium)
        Text("%,d원".format(amount), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ExpenseRow(item: Expense) {
    val zone = ZoneId.systemDefault()
    val d = Instant.ofEpochMilli(item.dateMillis).atZone(zone).toLocalDate()
    val date = "%02d/%02d".format(d.monthValue, d.dayOfMonth)

    val icon = when (item.category) {
        Category.FLIGHT -> Icons.Filled.Flight
        Category.HOTEL -> Icons.Filled.Hotel
        Category.FOOD -> Icons.Filled.Restaurant
        Category.SOUVENIR -> Icons.Filled.Redeem
        Category.OTHER -> Icons.Filled.CreditCard
    }

    val payLabel = item.pay.label

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(date, modifier = Modifier.width(56.dp), color = Color(0xFF666666))
            Spacer(Modifier.width(8.dp))
            Icon(icon, contentDescription = null, tint = categoryColor(item.category))
            Spacer(Modifier.width(8.dp))
            Text(item.category.label)
            Spacer(Modifier.width(8.dp))
            Text("($payLabel)", color = Color(0xFF777777), fontSize = 12.sp)
        }
        Text("%,d원".format(item.amount), textAlign = TextAlign.End)
    }
}

@Composable
fun ExpandableSection(
    title: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    Card(shape = RoundedCornerShape(16.dp)) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }
            if (expanded) content()
        }
    }
}

// ==============================
// Charts
// ==============================

private fun categoryColor(c: Category): Color = when (c) {
    Category.FLIGHT -> Color(0xFF4D8AF0)   // 파랑
    Category.HOTEL -> Color(0xFF67C587)    // 초록
    Category.FOOD -> Color(0xFFF6C152)     // 노랑
    Category.SOUVENIR -> Color(0xFFB06BF0) // 보라
    Category.OTHER -> Color(0xFFDA6E6E)    // 빨강
}

@Composable
fun PieChart(
    data: List<Float>,
    labels: List<String>,
    size: androidx.compose.ui.unit.Dp,
    colors: List<Color>
) {
    val total = data.sum().coerceAtLeast(1f)
    val sweepAngles = data.map { it / total * 360f }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(size)) {
            var startAngle = -90f
            sweepAngles.forEachIndexed { index, sweep ->
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    size = Size(size.toPx(), size.toPx())
                )
                startAngle += sweep
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(horizontalAlignment = Alignment.Start) {
            labels.forEachIndexed { index, label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(colors[index % colors.size], CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(label, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun buildDailyCategoryTotals(expenses: List<Expense>): Map<LocalDate, Map<Category, Int>> {
    if (expenses.isEmpty()) return emptyMap()
    val zone = ZoneId.systemDefault()

    val grouped = expenses.groupBy { e ->
        Instant.ofEpochMilli(e.dateMillis).atZone(zone).toLocalDate()
    }

    return grouped.mapValues { (_, list) ->
        Category.entries.associateWith { c -> list.filter { it.category == c }.sumOf { it.amount } }
    }
}

@Composable
private fun DailyStackedBarChart(
    dates: List<LocalDate>,
    totalsByDate: Map<LocalDate, Map<Category, Int>>,
    maxY: Int,
    colors: Map<Category, Color>
) {
    if (dates.isEmpty()) return

    val formatter = remember { DateTimeFormatter.ofPattern("MM/dd") }

    Card(shape = RoundedCornerShape(12.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 그래프 + 날짜 라벨을 같은 폭 계산으로 맞추려고
            val gapDp = 10.dp

            // ✅ 막대 그래프(Canvas)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val w = size.width
                val h = size.height

                val barCount = dates.size
                val gap = gapDp.toPx()
                val barWidth = ((w - gap * (barCount - 1)) / barCount).coerceAtLeast(6f)

                dates.forEachIndexed { idx, date ->
                    val totals = totalsByDate[date] ?: emptyMap()
                    val dayTotal = totals.values.sum().coerceAtLeast(0)

                    val xLeft = idx * (barWidth + gap)

                    var yBottom = h
                    Category.entries.forEach { cat ->
                        val v = totals[cat] ?: 0
                        if (v <= 0) return@forEach
                        val barH = (h * (v.toFloat() / maxY.toFloat())).coerceAtLeast(0f)
                        val yTop = yBottom - barH
                        drawRect(
                            color = colors[cat] ?: Color.Gray,
                            topLeft = Offset(xLeft, yTop),
                            size = Size(barWidth, barH)
                        )
                        yBottom = yTop
                    }

                    // 0원인 날은 얇게
                    if (dayTotal == 0) {
                        drawRect(
                            color = Color(0xFFE0E0E0),
                            topLeft = Offset(xLeft, h - 2f),
                            size = Size(barWidth, 2f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ✅ 각 막대 아래 날짜 라벨 (색/크기 기존 동일)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(gapDp)
            ) {
                dates.forEach { d ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            d.format(formatter),
                            fontSize = 12.sp,
                            color = Color(0xFF777777)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 범례
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Category.entries.forEach { cat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(colors[cat] ?: Color.Gray, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(cat.label, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}


@Composable
private fun PaymentRatioBar(
    card: Int,
    cash: Int,
    maxBudget: Int
) {
    val total = (card + cash).coerceAtLeast(1)
    val cardRatio = (card.toFloat() / maxBudget.toFloat()).coerceIn(0f, 1f)
    val cashRatio = (cash.toFloat() / maxBudget.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("카드", fontWeight = FontWeight.Medium)
            Text("%,d원".format(card))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE9ECF3))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(cardRatio)
                    .background(Color(0xFF3A6FF7))
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("현금", fontWeight = FontWeight.Medium)
            Text("%,d원".format(cash))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE9ECF3))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(cashRatio)
                    .background(Color(0xFF67C587))
            )
        }

        Text(
            "그래프 최대값 = 총 예산(%,d원)".format(maxBudget),
            fontSize = 12.sp,
            color = Color(0xFF777777)
        )
    }
}

// ==============================
// NOTE: 아래 BottomNavBar()는 네 프로젝트에 이미 있는 걸 그대로 쓰면 됨.
// 여기 파일에서 BottomNavBar()가 없으면, 기존 파일에 있는 걸 import/같은 패키지로 두면 됨.
// ==============================

// ------------------------------
// Preview
// ------------------------------

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewBudgetScreen() {
    MaterialTheme {
        TripBudgetScreen(
            tripTitle = "삿포로",
            onBack = {}
        )
    }
}

