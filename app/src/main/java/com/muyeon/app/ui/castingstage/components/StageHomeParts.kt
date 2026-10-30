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

/** 첫 화면 부품 — 섹션 틀·아이콘 칸·붙는 탭·전체 보기 버튼(iOS StageSectionHeader·StageIconGrid·StageChipTabBar·StageMoreButton). */

@Composable
internal fun StageSectionBox(title: String, content: @Composable () -> Unit) {
    // 위 여백 56 = 붙는 탭(46dp)에 제목이 가리지 않게 한 몫(iOS 와 같다).
    Column(Modifier.fillMaxWidth().padding(top = SECTION_TOP_DP.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, style = Bold, fontSize = 22.sp, color = MuyeonColors.textHead, modifier = Modifier.padding(horizontal = 16.dp))
        content()
    }
}

@Composable
internal fun StageIconGrid(cats: List<StageCategory>, onJump: (String) -> Unit) {
    // ★ 2026-10-31 두 줄 격자 → 한 줄(칸이 5개로 줄어 한 줄에 들어간다, 넘치면 옆으로 밀어 본다). iOS 와 같다.
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        cats.forEach { StageIconButton(it, onJump) }
    }
}

@Composable
internal fun StageIconButton(c: StageCategory, onJump: (String) -> Unit) {
    Column(
        Modifier.width(76.dp).clickable { onJump(c.code) }.semantics { contentDescription = "stage.icon.${c.code}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(c.bg), Alignment.Center) {
            Icon(c.icon, null, tint = c.fg, modifier = Modifier.size(28.dp))
        }
        Text(c.label, style = Medium, fontSize = 13.sp, color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
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
