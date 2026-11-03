package com.muyeon.app.ui.space.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.space.*
import kotlinx.coroutines.launch
import org.json.JSONArray

/** 공간 상세 섹션 공용 틀 — 흰 섹션·읽기 칸·번호 목록·└ 상세행·섹션 제목. */

// MARK: 공용 조각

@Composable
internal fun WhiteSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = SpaceDesign.gutter, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = {
            Text(
                title,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                lineHeight = 22.sp, color = SpaceDesign.ink900,
            )
            content()
        },
    )
}

/** 시안 입력 필드(라벨 + 테두리 박스)를 읽기 전용으로 사용. */
@Composable
internal fun ReadField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = SpaceDesign.ink900,
        )
        Text(
            value.ifEmpty { "-" },
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
            lineHeight = 18.sp, color = SpaceDesign.ink700,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, SpaceDesign.ink200, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

@Composable
internal fun NumberedBlock(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = SpaceDesign.ink900,
        )
        Text(
            items.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n"),
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
            lineHeight = 24.sp, color = SpaceDesign.ink700,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(SpaceDesign.ink100).padding(16.dp),
        )
    }
}

/** ic_x12_level(└) + 라벨(+배지) ↔ 값 행. */
@Composable
internal fun DetailRow(row: SpaceFeeRow, color: Color = SpaceDesign.ink500) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        SpaceLevelLead()
        Row(
            Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            row.badge?.takeIf { it.isNotEmpty() }?.let { SpaceMiniLabel(it) }
            Text(
                row.title,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 16.sp, color = color,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            row.value,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = color, textAlign = TextAlign.End,
        )
    }
}

internal fun firstNonEmpty(vararg values: String?): String? =
    values.filterNotNull().firstOrNull { it.trim().isNotEmpty() }

/** 섹션 제목 ("…님을 위한 혜택" 자리). */
@Composable
internal fun SpaceSectionHeadline(text: String) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
        lineHeight = 22.sp, color = SpaceDesign.ink900,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 21.dp)
            .padding(top = 32.dp, bottom = 15.dp),
    )
}
