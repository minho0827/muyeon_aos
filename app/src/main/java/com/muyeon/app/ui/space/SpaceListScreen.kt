package com.muyeon.app.ui.space

import com.muyeon.app.ui.common.market.loginRequired
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.result.OnChildResult
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.space.components.*
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.EmptyView

/**
 * 공간 대관 목록 — [필터(이용유형·가격·편의시설)][지역][날짜][인원][정렬] 시트.
 *  카드: 사진 · 이름 · "서울 동작구 · 최대 15명" · "시간당 7,000원~" · 하트. 화면은 상태 연결과 조립만 한다.
 */
@Composable
fun SpaceListScreen(
    api: SpaceApi,
    canCreate: Boolean,
    /** null = 탭(뒤로 없음). */
    onBack: (() -> Unit)?,
    onOpen: (Int, String?) -> Unit,
    onCreate: () -> Unit,
    onMessage: (String) -> Unit,
    /** 공간 탭 안 머리(위 바 + 대관·양도양수 탭). 주면 이 화면 위 바 대신 그린다 — 등록(＋)은 머리가 맡는다. */
    header: (@Composable () -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<SpaceCategory>>(emptyList()) }
    var categoryId by rememberSaveable { mutableIntStateOf(0) }
    var sido by rememberSaveable { mutableStateOf<String?>(null) }
    var date by rememberSaveable { mutableStateOf<String?>(null) }
    var headcount by rememberSaveable { mutableStateOf<Int?>(null) }
    var sort by rememberSaveable { mutableStateOf("recent") }
    var filter by remember { mutableStateOf(SpaceFilter()) }
    var items by remember { mutableStateOf<List<SpaceListItem>>(emptyList()) }
    var scrapped by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { api.categories().onSuccess { categories = it } }
    LaunchedEffect(categoryId, sido, date, headcount, sort, filter) {
        loading = true
        api.list(sido, categoryId.takeIf { it > 0 }, date, headcount, sort, filter)
            .onSuccess { items = it.filterNot { s -> com.muyeon.domain.usecase.home.DemoContent.isDemoTitle(s.name) }; error = null } // 예시 숨김(사용자 결정 2026-11-08).onFailure { error = it.message }
        scrapped = api.scrappedSpaceIds()
        loading = false
    }
    // 상세에서 찜을 바꾸고 돌아오면 하트를 다시 읽는다.
    OnChildResult { keys -> if ("SPACE" in keys) scope.launch { scrapped = api.scrappedSpaceIds() } }

    fun toggleScrap(s: SpaceListItem) {
        if (!api.isLoggedIn) { onMessage(loginRequired("openSpaceDetail", "spaceId" to s.id, "then" to "like")); return }
        val next = s.id !in scrapped
        scrapped = if (next) scrapped + s.id else scrapped - s.id
        scope.launch {
            api.setScrap(s.id, next).onFailure { e ->
                scrapped = if (next) scrapped - s.id else scrapped + s.id
                onMessage(e.message ?: "찜 처리에 실패했어요.")
            }
        }
    }

    Column(Modifier.fillMaxSize().background(MuyeonList.pageBg)) {
        if (header != null) header() else MuyeonListHeader("공간 대관", onBack = onBack) {
            if (canCreate) MuyeonHeaderIcon(Icons.Filled.Add, "공간 등록", onClick = onCreate)
        }
        // ★ 2026-11-08 위 카테고리 칩(서버 공간 테마 — 발레·한국무용·매매 …)은 대관에 맞지 않아 뺐다(사용자 요청). 시설 필터로 거른다.
        SpaceFilterBar(sido, date, headcount, sort, filter.count) { sheet = it }

        val phase = when {
            loading && items.isEmpty() -> ListPhase.Loading
            items.isEmpty() -> ListPhase.Empty
            else -> ListPhase.Items
        }
        StableSwap(phase, Modifier.weight(1f).fillMaxWidth()) { p ->
            Box(Modifier.fillMaxSize()) {
                when (p) {
                    ListPhase.Loading -> Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(3) { SpaceCardSkeleton() }
                    }
                    ListPhase.Empty, ListPhase.Error -> EmptyView(error ?: stringResource(R.string.space_list_empty))
                    ListPhase.Items -> LazyColumn(Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (onBack == null) com.muyeon.app.ui.main.tabBarBottomPadding() else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(items, key = { it.id }) { s ->
                            SpaceCard(s, onClick = { onOpen(s.id, date) }) { SpaceCardHeart(s.id in scrapped) { toggleScrap(s) } }
                        }
                    }
                }
            }
        }
    }

    val close = { sheet = null }
    when (sheet) {
        "region" -> SpaceRegionSheet(sido, onApply = { sido = it }, onDismiss = close)
        "date" -> SpaceDateSheet(date, onApply = { date = it }, onDismiss = close)
        "headcount" -> SpaceHeadcountSheet(headcount, onApply = { headcount = it }, onDismiss = close)
        "sort" -> SpaceSortSheet(sort, onApply = { sort = it }, onDismiss = close)
        "filter" -> SpaceFilterSheet(filter, onApply = { filter = it; close() }, onDismiss = close)
    }
}
