package com.muyeon.app.ui.space

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.result.OnChildResult
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.FormChipWrap
import com.muyeon.app.ui.common.market.MarketDates
import com.muyeon.app.ui.common.market.MarketFilterPill
import com.muyeon.app.ui.common.market.MarketHeart
import com.muyeon.app.ui.common.market.MarketThumb
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonEmptyState
import com.muyeon.app.ui.common.MuyeonHeaderIcon
import com.muyeon.app.ui.common.MuyeonList
import com.muyeon.app.ui.common.MuyeonListHeader
import com.muyeon.app.ui.common.MuyeonPillTab
import com.muyeon.app.ui.common.MuyeonPillTabs
import com.muyeon.app.ui.common.market.SheetActions
import com.muyeon.app.ui.transfer.TransferLabels
import kotlinx.coroutines.launch

private val SORTS = listOf("recent" to "추천순", "priceAsc" to "가격 낮은순", "priceDesc" to "가격 높은순")

/**
 * 공간 대관 목록 — 카테고리 칩(GET /spaces/categories, 첫 칩 "전체") + [지역][날짜][인원][정렬] 시트.
 *  카드: 사진 · 이름 · "서울 동작구 · 최대 15명" · "시간당 7,000원~" · 하트.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceListScreen(
    api: SpaceApi,
    canCreate: Boolean,
    onBack: () -> Unit,
    onOpen: (Int, String?) -> Unit,
    onCreate: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<SpaceCategory>>(emptyList()) }
    var categoryId by rememberSaveable { mutableIntStateOf(0) }
    var sido by rememberSaveable { mutableStateOf<String?>(null) }
    var date by rememberSaveable { mutableStateOf<String?>(null) }
    var headcount by rememberSaveable { mutableStateOf<Int?>(null) }
    var sort by rememberSaveable { mutableStateOf("recent") }
    var items by remember { mutableStateOf<List<SpaceListItem>>(emptyList()) }
    var scrapped by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { api.categories().onSuccess { categories = it } }
    LaunchedEffect(categoryId, sido, date, headcount, sort) {
        loading = true
        api.list(sido, categoryId.takeIf { it > 0 }, date, headcount, sort)
            .onSuccess { items = it; error = null }.onFailure { error = it.message }
        scrapped = api.scrappedSpaceIds()
        loading = false
    }
    // 상세에서 찜을 바꾸고 돌아오면 하트를 다시 읽는다.
    OnChildResult { keys -> if ("SPACE" in keys) scope.launch { scrapped = api.scrappedSpaceIds() } }

    Column(Modifier.fillMaxSize().background(MuyeonList.pageBg)) {
        MuyeonListHeader("공간 대관", onBack = onBack) {
            if (canCreate) MuyeonHeaderIcon(Icons.Filled.Add, "공간 등록", onClick = onCreate)
        }
        MuyeonPillTabs(
            listOf(MuyeonPillTab(0, "전체")) + categories.map { MuyeonPillTab(it.id, it.name) },
            categoryId, { categoryId = it }, tagPrefix = "space.category", compact = true, showDivider = false,
        )
        Row(
            Modifier.fillMaxWidth().background(Color.White).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MarketFilterPill(TransferLabels.regions.firstOrNull { it.second == sido }?.first?.takeIf { sido != null } ?: "지역", sido != null) { sheet = "region" }
            MarketFilterPill(date?.substring(5)?.replace('-', '/') ?: "날짜", date != null) { sheet = "date" }
            MarketFilterPill(headcount?.let { "${it}명" } ?: "인원", headcount != null) { sheet = "headcount" }
            MarketFilterPill(SORTS.first { it.first == sort }.second, sort != "recent") { sheet = "sort" }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MuyeonList.ink200))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading && items.isEmpty() -> CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.align(Alignment.Center))
                items.isEmpty() -> MuyeonEmptyState(
                    error ?: "조건에 맞는 공간이 없어요.", icon = Icons.Outlined.MeetingRoom, modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items, key = { it.id }) { s ->
                        SpaceListCard(s, s.id in scrapped, onClick = { onOpen(s.id, date) }) {
                            if (!api.isLoggedIn) { onMessage("로그인 후 이용할 수 있어요."); return@SpaceListCard }
                            val next = s.id !in scrapped
                            scrapped = if (next) scrapped + s.id else scrapped - s.id
                            scope.launch {
                                api.setScrap(s.id, next).onFailure { e ->
                                    scrapped = if (next) scrapped - s.id else scrapped + s.id
                                    onMessage(e.message ?: "찜 처리에 실패했어요.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    when (sheet) {
        "region" -> FilterSheet("지역", onDismiss = { sheet = null }) { close ->
            var pick by remember { mutableStateOf(sido) }
            Column(Modifier.padding(horizontal = 20.dp)) {
                FormChipWrap(TransferLabels.regions.map { (it.second ?: "") to it.first }, { (pick ?: "") == it }) { pick = it.ifEmpty { null } }
            }
            SheetActions(onReset = { sido = null; close() }, onApply = { sido = pick; close() })
        }
        "date" -> FilterSheet("날짜", onDismiss = { sheet = null }) { close ->
            val today = remember { MarketDates.todayKst() }
            val state = rememberDatePickerState(
                initialSelectedDateMillis = MarketDates.toMillis(date),
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = MarketDates.ymd(utcTimeMillis) >= today
                },
            )
            DatePicker(state, title = null, headline = null, showModeToggle = false)
            SheetActions(onReset = { date = null; close() }, onApply = { date = state.selectedDateMillis?.let { MarketDates.ymd(it) }; close() })
        }
        "headcount" -> FilterSheet("인원", onDismiss = { sheet = null }) { close ->
            var n by remember { mutableIntStateOf(headcount ?: 1) }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleStep(Icons.Filled.Remove, n > 1) { n -= 1 }
                Text("${n}명", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MuyeonColors.textHead)
                CircleStep(Icons.Filled.Add, n < 300) { n += 1 }
            }
            SheetActions(onReset = { headcount = null; close() }, onApply = { headcount = n; close() })
        }
        "sort" -> FilterSheet("정렬", onDismiss = { sheet = null }) { close ->
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SORTS.forEach { (k, label) -> RadioRow(label, sort == k) { sort = k; close() } }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(
                title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            content(onDismiss)
        }
    }
}

@Composable
private fun CircleStep(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).alpha(if (enabled) 1f else 0.35f).clip(CircleShape).border(1.dp, MuyeonList.ink300, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        Alignment.Center,
    ) { Icon(icon, null, tint = MuyeonColors.textHead) }
}

@Composable
internal fun SpaceListCard(s: SpaceListItem, liked: Boolean?, onClick: () -> Unit, onHeart: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(Color.White).clickable(onClick = onClick)) {
        Box {
            MarketThumb(s.image, Modifier.fillMaxWidth().height(180.dp))
            if (liked != null) {
                Box(Modifier.align(Alignment.TopEnd).padding(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f))) {
                    MarketHeart(liked, 20.dp, onHeart)
                }
            }
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(s.name, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (s.metaLine.isNotEmpty()) Text(s.metaLine, fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub, maxLines = 1)
            s.priceLine?.let { Text(it, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MuyeonColors.textHead) }
        }
    }
}
