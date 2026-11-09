package com.muyeon.app.ui.performance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.theme.AppColors

/** 공연·캐스팅 탭 안 두 칸 — 같은 하단 [공연] 칸을 쓴다(MainTab.PERFORMANCE · MainTab.CASTING). */
enum class PerfCastingMode { PERFORMANCE, CASTING }

/** 위 줄 오른쪽 분류 하나 — [key] 는 공연이면 장르 값, 캐스팅이면 StageCategory 코드. */
data class PerfCastingItem(val key: String, val label: String)

/**
 * ★ 2026-11-09 공연·캐스팅 통합 탭 머리(NOL 홈·티켓 줄, 사용자 시안 승인) — 위 바(탭 이름 + 채팅) 아래
 *   `[공연] [캐스팅] | 분류…` **한 줄 전체가 같이 가로로 밀린다**(사용자 결정). 고른 칸은 검은 알약, 고른 분류는 주황 굵게.
 *   공연 → 장르(전체·발레·…) · 캐스팅 → 캐스팅 공고·국내소식·공연·축제·콩쿠르·대회·무용입시·학교·교육과정(소식이 있는 섹션만).
 */
@Composable
fun PerfCastingHeader(
    mode: PerfCastingMode,
    onMode: (PerfCastingMode) -> Unit,
    items: List<PerfCastingItem>,
    selected: String?,
    onSelect: (String) -> Unit,
    /** 위 바 오른쪽 버튼(캐스팅: 공고 전체 보기). */
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().background(AppColors.Surface)) {
        MainTopBar(title = stringResource(R.string.tab_performance), chat = true, actions = actions)
        val listState = rememberLazyListState()
        // 칸을 바꾸면 줄 맨 앞(알약)으로 — 분류 목록이 통째로 바뀐다.
        LaunchedEffect(mode) { listState.scrollToItem(0) }
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item(key = "mode.performance") { ModePill(stringResource(R.string.tab_performance), mode == PerfCastingMode.PERFORMANCE, "perfCasting.mode.performance") { onMode(PerfCastingMode.PERFORMANCE) } }
            item(key = "mode.casting") { ModePill(stringResource(R.string.tab_casting), mode == PerfCastingMode.CASTING, "perfCasting.mode.casting") { onMode(PerfCastingMode.CASTING) } }
            item(key = "divider") { Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(18.dp).background(DividerColor)) }
            items(items, key = { "item.${it.key}" }) { it ->
                val on = it.key == selected
                Text(
                    it.label,
                    style = TextStyle(fontSize = 15.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                    color = if (on) AppColors.Primary else AppColors.TextSub,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onSelect(it.key) }
                        .padding(horizontal = 9.dp, vertical = 8.dp).testTag("perfCasting.item.${it.key}"),
                )
            }
        }
    }
}

private val DividerColor = Color(0xFFDCDCE0)

@Composable
private fun ModePill(text: String, on: Boolean, tag: String, onClick: () -> Unit) {
    Text(
        text,
        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
        color = if (on) Color.White else AppColors.TextHead,
        modifier = Modifier.padding(end = 4.dp).clip(RoundedCornerShape(18.dp))
            .background(if (on) AppColors.TextHead else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = if (on) 16.dp else 12.dp, vertical = 8.dp)
            .testTag(tag),
    )
}
