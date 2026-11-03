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

/** 첫 로딩. */
@Composable
fun BoxScope.TransferListLoading() {
    CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.align(Alignment.Center))
}

/** 첫 쪽을 못 읽음 — 서버 문구 + [다시 시도]. */
@Composable
fun BoxScope.TransferListError(message: String, onRetry: () -> Unit) {
    MuyeonEmptyState(message, actionTitle = "다시 시도", onAction = onRetry, modifier = Modifier.align(Alignment.Center))
}

/** 빈 목록. */
@Composable
fun BoxScope.TransferListEmpty() {
    MuyeonEmptyState("아직 올라온 양도 글이 없어요.", icon = Icons.Outlined.Storefront, modifier = Modifier.align(Alignment.Center))
}

/** 카드 목록 + (다음 쪽 읽는 중이면) 아래 스피너. card 는 카드 한 장을 그리는 쪽이 고른다. */
@Composable
fun <T> TransferListItems(
    listState: LazyListState,
    items: List<T>,
    key: (T) -> Any,
    loadingMore: Boolean,
    card: @Composable (T) -> Unit,
) {
    LazyColumn(
        state = listState, modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = key) { card(it) }
        if (loadingMore) item(key = "more") {
            Box(Modifier.fillMaxWidth().padding(12.dp), Alignment.Center) { CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.size(24.dp)) }
        }
    }
}
