package com.muyeon.app.ui.space.booking

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
import com.muyeon.app.ui.space.SpaceListItem
import com.muyeon.app.ui.space.components.SpaceCard
import kotlinx.coroutines.launch

/** 내 공간 한 줄 — 카드 + [수정][삭제]. */
@Composable
fun MySpaceRow(s: SpaceListItem, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(Color.White)) {
        SpaceCard(s, onClick = onOpen)
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MarketRowButton("수정", Modifier.weight(1f), onClick = onEdit)
            MarketRowButton("삭제", Modifier.weight(1f), danger = true, onClick = onDelete)
        }
    }
}

/** 내 공간 목록 자리 — 카드 자리 + 버튼 줄(40), MySpaceRow 와 같은 높이. */
@Composable
fun MySpaceListSkeleton(count: Int = 3) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(Color.White)) {
                com.muyeon.app.ui.space.components.SpaceCardSkeleton()
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) { Box(Modifier.weight(1f).height(40.dp).skeleton(10)) }
                }
            }
        }
    }
}
