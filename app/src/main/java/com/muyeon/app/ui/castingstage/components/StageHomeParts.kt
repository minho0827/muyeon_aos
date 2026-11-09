package com.muyeon.app.ui.castingstage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.screens.*

/** 첫 화면 부품 — 섹션 틀·바로가기 칩·붙는 탭·전체 보기 버튼(iOS StageSectionHeader·StageIconGrid·StageChipTabBar·StageMoreButton). */

@Composable
internal fun StageSectionBox(title: String, top: Int = SECTION_TOP_DP, content: @Composable () -> Unit) {
    // 위 여백 56 = 붙는 탭(46dp)에 제목이 가리지 않게 한 몫(iOS 와 같다). 붙는 탭이 없으면(통합 탭) 좁게 — [top].
    Column(Modifier.fillMaxWidth().padding(top = top.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, style = Bold, fontSize = 22.sp, color = MuyeonColors.textHead, modifier = Modifier.padding(horizontal = 16.dp))
        content()
    }
}

/**
 * 섹션 바로가기 — ★ 2026-11-09 아이콘 칸 → 글자 칩(사용자 결정: 아이콘 없이 글자만). 공용 [ChipRow] 그대로.
 *   고른 칸 표시는 바로 아래 붙는 탭이 맡아서 여기는 고른 칩 없이 둔다.
 */
@Composable
internal fun StageChipRow(cats: List<StageCategory>, onJump: (String) -> Unit) {
    com.muyeon.app.ui.components.ChipRow(
        options = cats.map { com.muyeon.app.ui.components.ChipOption(it.code, it.label) },
        selected = "",
        onSelect = onJump,
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
    )
}

@Composable
internal fun StageTabBar(cats: List<StageCategory>, current: String, onJump: (String) -> Unit) {
    val rowState = rememberLazyListState()
    LaunchedEffect(current) {
        val i = cats.indexOfFirst { it.code == current }
        if (i >= 0) rowState.animateScrollToItem(maxOf(0, i - 1))
    }
    Column(Modifier.fillMaxWidth().height(TAB_HEIGHT_DP.dp).background(Color.White)) {
        LazyRow(state = rowState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            items(cats, key = { it.code }) { c ->
                val on = c.code == current
                Column(Modifier.fillMaxHeight().clickable { onJump(c.code) }.semantics { contentDescription = "stage.tab.${c.code}${if (on) ".selected" else ""}" }) {
                    Box(Modifier.weight(1f), Alignment.Center) {
                        Text(c.label, style = if (on) Bold else Regular, fontSize = 16.sp,
                            color = if (on) MuyeonColors.textHead else MuyeonColors.textSub)
                    }
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if (on) MuyeonColors.textHead else Color.Transparent))
                }
            }
        }
        HorizontalDivider(color = MuyeonColors.border)
    }
}

@Composable
internal fun StageMoreButton(bold: String, onClick: () -> Unit) {
    Box(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp).fillMaxWidth().heightIn(min = 50.dp)
            .clip(RoundedCornerShape(14.dp)).border(1.dp, Ink300, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        Alignment.Center,
    ) {
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Bold)) { append(bold) }
            withStyle(SpanStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Normal)) { append(" 전체보기") }
        }, fontSize = 15.sp, color = MuyeonColors.textHead)
    }
}
