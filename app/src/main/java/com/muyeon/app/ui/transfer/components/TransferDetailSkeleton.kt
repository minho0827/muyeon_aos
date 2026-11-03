package com.muyeon.app.ui.transfer.components

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
import kotlinx.coroutines.launch

/** 상세 자리(불러오는 중) — 갤러리(280) · 칩 · 제목 2줄 · 요약 줄 · 기본정보 표 자리. 실제 화면과 같은 여백. */
@Composable
fun TransferDetailSkeleton() {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(280.dp).background(SkeletonColor))
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(3) { Box(Modifier.width(52.dp).height(22.dp).skeleton()) } }
                SkeletonLine(22, 0.85f, lines = 2, lineHeight = 30)
                SkeletonLine(13, 0.5f)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonLine(18, 0.3f)
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, MuyeonList.ink200, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    repeat(6) { Box(Modifier.padding(vertical = 10.dp)) { SkeletonLine(14, 0.7f) } }
                }
            }
        }
    }
}

