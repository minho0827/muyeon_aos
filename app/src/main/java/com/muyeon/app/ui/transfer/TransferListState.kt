package com.muyeon.app.ui.transfer

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

/**
 * 목록 상태 — 액티비티에 붙들어 상세에 다녀와도 스크롤·목록이 남는다(돌아오면 조용히 1쪽부터 다시 읽는다).
 */
class TransferListState {
    var region by mutableStateOf<String?>(null)
    var filter by mutableStateOf(TransferFilter())
    var items by mutableStateOf<List<TransferCard>>(emptyList())
    var hasMore by mutableStateOf(true)
    var loading by mutableStateOf(false)
    var loadedOnce by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    val listState = LazyListState()
    private var cursor: String? = null
    private var generation = 0

    suspend fun load(api: TransferApi, reset: Boolean) {
        if (!reset && (loading || !hasMore)) return
        val gen = if (reset) ++generation else generation
        if (reset) cursor = null
        loading = true; error = null
        api.page(region, filter, cursor)
            .onSuccess { p ->
                if (gen != generation) return@onSuccess
                // 예시 글은 화면에서만 숨긴다(사용자 결정 2026-11-08).
                val fresh = p.items.filterNot { com.muyeon.domain.usecase.home.DemoContent.isDemoTitle(it.title) }
                items = if (reset) fresh else (items + fresh).distinctBy { it.id }
                hasMore = p.hasMore && p.nextCursor != null
                cursor = p.nextCursor
            }
            .onFailure { if (gen == generation) error = it.message }
        if (gen == generation) { loading = false; loadedOnce = true }
    }

    fun patch(id: Int, block: (TransferCard) -> TransferCard) {
        items = items.map { if (it.id == id) block(it) else it }
    }
}
