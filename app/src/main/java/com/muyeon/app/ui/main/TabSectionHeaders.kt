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
val SPACE_SECTIONS = listOf(MainTab.SPACES, MainTab.TRANSFERS)

@Composable
fun stageTitle() = stringResource(R.string.tab_stage_title)

@Composable
fun spaceTitle() = stringResource(R.string.tab_space)
