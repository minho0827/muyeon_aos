package com.muyeon.app.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.UnderlineTabs

/**
 * ★ 2026-11-08 탭 안 나눔 머리 — 위 바(탭 이름 + 오른쪽 버튼) + 밑줄 글자 탭.
 *   캐스팅 탭(제목 공연/캐스팅): 캐스팅 · 공연 / 공간 탭: 대관 · 양도양수(2계층 탭바 대신 화면 안에서 나눈다 — 사용자 결정).
 */
@Composable
fun TabSectionHeader(
    title: String,
    sections: List<MainTab>,
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    Column {
        MainTopBar(title = title, actions = actions)
        UnderlineTabs(
            options = sections.map { ChipOption(it.key, stringResource(it.labelRes)) },
            selected = selected.key,
            onSelect = { key -> MainTab.fromKey(key)?.let(onSelect) },
        )
    }
}

val STAGE_SECTIONS = listOf(MainTab.CASTING, MainTab.PERFORMANCE)

/**
 * 캐스팅 탭 머리 — 홈과 같은 흰 위 바, 가운데 [캐스팅 | 공연] 토글(사용자 요청 2026-11-08). 밑줄 탭은 없다.
 *   오른쪽 [actions] — 캐스팅이면 글쓰기·전체 보기.
 */
@Composable
fun StageTopBar(selected: MainTab, onSelect: (MainTab) -> Unit, actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}) {
    com.muyeon.app.ui.components.BrandTopBar(
        center = {
            com.muyeon.app.ui.components.SegmentToggle(
                STAGE_SECTIONS.map { ChipOption(it.key, stringResource(it.labelRes)) }, selected.key,
                { key -> MainTab.fromKey(key)?.let(onSelect) },
            )
        },
        actions = actions,
    )
}
val SPACE_SECTIONS = listOf(MainTab.SPACES, MainTab.TRANSFERS)

@Composable
fun stageTitle() = stringResource(R.string.tab_stage_title)

@Composable
fun spaceTitle() = stringResource(R.string.tab_space)
