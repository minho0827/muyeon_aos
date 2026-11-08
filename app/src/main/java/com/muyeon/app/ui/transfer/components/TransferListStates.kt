package com.muyeon.app.ui.transfer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
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
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.EmptyView

/** 첫 로딩 — 실제 카드와 같은 높이의 카드 자리 N장(작은 스피너가 커지며 바뀌지 않게). */
@Composable
fun TransferListLoading(count: Int = 6) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) { TransferCardSkeleton() }
    }
}

/** 첫 쪽을 못 읽음 — 서버 문구 + [다시 시도]. */
@Composable
fun TransferListError(message: String, onRetry: () -> Unit) {
    EmptyView(message, buttonText = stringResource(R.string.common_retry), onButton = onRetry)
}

/** 빈 목록. */
@Composable
fun TransferListEmpty() {
    EmptyView(stringResource(R.string.transfer_list_empty))
}

/** 카드 목록 + 다음 쪽이 있으면 아래 고정 높이 자리(읽는 중일 때만 스피너). card 는 카드 한 장을 그리는 쪽이 고른다. */
@Composable
fun <T> TransferListItems(
    listState: LazyListState,
    items: List<T>,
    key: (T) -> Any,
    hasMore: Boolean,
    loadingMore: Boolean,
    /** 맨 아래 여백 — 탭(떠 있는 탭바 아래)에서 열면 더 크게. */
    bottomPadding: androidx.compose.ui.unit.Dp = 16.dp,
    card: @Composable (T) -> Unit,
) {
    LazyColumn(
        state = listState, modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomPadding), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = key) { card(it) }
        if (hasMore) item(key = "more") { ListMoreFooter(loadingMore) }
    }
}
