package com.example.travelog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

// ==============================
// Colors (화이트톤)
// ==============================
private val AppBg = Color(0xFFF7F8FA)
private val CardBg = Color.White
private val CardBorder = Color(0xFFE7EAF0)
private val TextPrimary = Color(0xFF111827)
private val TextSub = Color(0xFF6B7280)
private val FieldBg = Color(0xFFF9FAFB)
private val ChipBg = Color(0xFFEFF2F7)
private val TrackBg = Color(0xFFEFF2F7)
private val PrimaryBlue = Color(0xFF3A6FF7)

private val CardShape = RoundedCornerShape(18.dp)
private val InnerCardShape = RoundedCornerShape(14.dp)

// ==============================
// Data
// ==============================
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
// Reusable Card
// ==============================
@Composable
private fun AppCard(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun SoftDivider(modifier: Modifier = Modifier) {
    Divider(modifier = modifier, thickness = 1.dp, color = Color(0xFFF0F2F6))
}

// ==============================
// Screen
// ==============================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripBudgetScreen(
    tripId: String,
    tripTitle: String,
    onBack: () -> Unit,
    vm: TripsViewModel
) {
    // ✅ VM에서 tripId별 상태 가져오기 (나갔다 와도 유지)
    val st = vm.budgetState(tripId)
    val totalBudgetText = st.totalBudgetText
    val expenses = st.expenses

    val totalBudget = totalBudgetText.filter { it.isDigit() }.toIntOrNull() ?: 0

    // ✅ expenses.add(...) 하면 즉시 그래프/합계 갱신
    val used by remember { derivedStateOf { expenses.sumOf { it.amount } } }
    val remaining by remember { derivedStateOf { totalBudget - used } }

    val categoryTotals by remember {
        derivedStateOf {
            Category.entries.associateWith { c ->
                expenses.filter { it.category == c }.sumOf { it.amount }
            }
        }
    }

    val dailyCategory by remember { derivedStateOf { buildDailyCategoryTotals(expenses.toList()) } }
    val dailyDates by remember { derivedStateOf { dailyCategory.keys.sorted() } }
    val maxDaily by remember {
        derivedStateOf { dailyCategory.values.maxOfOrNull { map -> map.values.sum() } ?: 0 }
    }

    val cardSum by remember { derivedStateOf { expenses.filter { it.pay == PayMethod.CARD }.sumOf { it.amount } } }
    val cashSum by remember { derivedStateOf { expenses.filter { it.pay == PayMethod.CASH }.sumOf { it.amount } } }

    var showAdd by remember { mutableStateOf(false) }

    if (showAdd) {
        AddExpenseDialog(
            onDismiss = { showAdd = false },
            onSave = { e ->
                vm.addExpense(tripId, e) // ✅ VM에 저장
                showAdd = false
            }
        )
    }

    Scaffold(
        containerColor = AppBg,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBg,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                title = { Text("여행 비용", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(AppBg),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // [0] 총예산 입력
            item {
                AppCard {
                    Text("총 예산", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    TextField(
                        value = totalBudgetText,
                        onValueChange = { vm.setTotalBudgetText(tripId, it) }, // ✅ VM에 저장
                        singleLine = true,
                        placeholder = { Text("예: 1500000", color = TextSub) },
                        supportingText = { Text("숫자만 입력해주세요", color = TextSub) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = FieldBg,
                            unfocusedContainerColor = FieldBg,
                            disabledContainerColor = FieldBg,
                            focusedIndicatorColor = PrimaryBlue,
                            unfocusedIndicatorColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryBlue
                        )
                    )
                }
            }

            // [1] 예산 카드 + 파이차트
            item {
                AppCard(innerPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // [2] 환율 정보
            item {
                ExpandableSection(title = "환율 정보") {
                    Text("현재 환율: 1 JPY ≈ 9.2 KRW", color = TextPrimary)
                }
            }

            // [3] 지출 내역
            item {
                ExpandableSection(title = "지출 내역") {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { showAdd = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("지출 추가")
                            }
                        }

                        if (expenses.isEmpty()) {
                            Text(
                                "아직 지출 내역이 없어요.\n'지출 추가' 버튼으로 입력해주세요",
                                color = TextSub
                            )
                        } else {
                            val sorted = expenses.sortedBy { it.dateMillis }
                            sorted.forEachIndexed { idx, e ->
                                ExpenseRow(e)
                                if (idx != sorted.lastIndex) {
                                    SoftDivider(Modifier.padding(vertical = 8.dp))
                                }
                            }

                            Spacer(Modifier.height(8.dp))

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

            // [4] 결제 수단
            item {
                ExpandableSection(title = "결제 수단") {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (totalBudget <= 0) {
                            Text("총 예산을 먼저 입력해주세요", color = TextSub)
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
// Add Expense Dialog
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

    val zone = ZoneId.systemDefault()
    val todayMillis = remember { LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli() }
    var selectedDateMillis by remember { mutableStateOf(todayMillis) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("취소") } }
        ) { DatePicker(state = datePickerState) }
    }

    val dateLabel = remember(selectedDateMillis) {
        val d = Instant.ofEpochMilli(selectedDateMillis).atZone(zone).toLocalDate()
        "%02d/%02d".format(d.monthValue, d.dayOfMonth)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("지출 추가", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("날짜", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) { Text(dateLabel) }

                Text("카테고리", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                ChipRow(
                    items = Category.entries,
                    selected = category,
                    label = { it.label },
                    onSelect = { category = it }
                )

                Text("결제 수단", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                ChipRow(
                    items = PayMethod.entries,
                    selected = pay,
                    label = { it.label },
                    onSelect = { pay = it }
                )

                Text("금액(원)", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text("예: 12500", color = TextSub) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FieldBg,
                        unfocusedContainerColor = FieldBg,
                        disabledContainerColor = FieldBg,
                        focusedIndicatorColor = PrimaryBlue,
                        unfocusedIndicatorColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryBlue
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amountText.filter { it.isDigit() }.toIntOrNull() ?: 0
                if (amt > 0) onSave(Expense(selectedDateMillis, category, pay, amt))
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
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
                            .background(if (isSelected) PrimaryBlue else ChipBg)
                            .clickable { onSelect(item) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label(item),
                            color = if (isSelected) Color.White else TextPrimary,
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
// UI helpers / Charts
// ==============================

@Composable
private fun StatRow(label: String, amount: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSub, fontWeight = FontWeight.Medium)
        Text("%,d원".format(amount), color = TextPrimary, fontWeight = FontWeight.SemiBold)
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

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(date, modifier = Modifier.width(56.dp), color = TextSub)
            Spacer(Modifier.width(8.dp))
            Icon(icon, contentDescription = null, tint = categoryColor(item.category))
            Spacer(Modifier.width(8.dp))
            Text(item.category.label, color = TextPrimary)
            Spacer(Modifier.width(8.dp))
            Text("(${item.pay.label})", color = TextSub, fontSize = 12.sp)
        }
        Text("%,d원".format(item.amount), textAlign = TextAlign.End, color = TextPrimary)
    }
}

@Composable
fun ExpandableSection(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = TextSub
                )
            }

            if (expanded) {
                SoftDivider()
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) { content() }
            }
        }
    }
}

private fun categoryColor(c: Category): Color = when (c) {
    Category.FLIGHT -> Color(0xFF4D8AF0)
    Category.HOTEL -> Color(0xFF67C587)
    Category.FOOD -> Color(0xFFF6C152)
    Category.SOUVENIR -> Color(0xFFB06BF0)
    Category.OTHER -> Color(0xFFDA6E6E)
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
                    Box(Modifier.size(10.dp).background(colors[index % colors.size], CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(label, fontSize = 12.sp, color = TextSub)
                }
            }
        }
    }
}

private fun buildDailyCategoryTotals(expenses: List<Expense>): Map<LocalDate, Map<Category, Int>> {
    if (expenses.isEmpty()) return emptyMap()
    val zone = ZoneId.systemDefault()
    val grouped = expenses.groupBy { Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate() }
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
    val gapDp = 10.dp

    Card(
        shape = InnerCardShape,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Canvas(
                modifier = Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 8.dp)
            ) {
                val w = size.width
                val h = size.height

                val barCount = dates.size
                val gap = gapDp.toPx()
                val barWidth = ((w - gap * (barCount - 1)) / barCount).coerceAtLeast(6f)

                dates.forEachIndexed { idx, date ->
                    val totals = totalsByDate[date] ?: emptyMap()
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
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(gapDp)
            ) {
                dates.forEach { d ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(d.format(formatter), fontSize = 12.sp, color = TextSub)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentRatioBar(card: Int, cash: Int, maxBudget: Int) {
    val cardRatio = (card.toFloat() / maxBudget.toFloat()).coerceIn(0f, 1f)
    val cashRatio = (cash.toFloat() / maxBudget.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("카드", fontWeight = FontWeight.Medium, color = TextPrimary)
            Text("%,d원".format(card), color = TextPrimary)
        }
        Row(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(TrackBg)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(cardRatio).background(PrimaryBlue))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("현금", fontWeight = FontWeight.Medium, color = TextPrimary)
            Text("%,d원".format(cash), color = TextPrimary)
        }
        Row(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(TrackBg)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(cashRatio).background(Color(0xFF67C587)))
        }
    }
}

// ------------------------------
// Preview
// ------------------------------
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewBudgetScreen() {
    val vm = remember { TripsViewModel() }
    MaterialTheme {
        TripBudgetScreen(
            tripId = "preview-trip",
            tripTitle = "삿포로",
            onBack = {},
            vm = vm
        )
    }
}
