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
 *   채용 탭(채용 2계층의 첫 칸): 구인 · 구직 / 공간 탭: 대관 · 양도양수(화면 안에서 나눈다 — 사용자 결정 2026-11-08).
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
        MainTopBar(title = title, chat = true, actions = actions)
        UnderlineTabs(
            options = sections.map { ChipOption(it.key, stringResource(it.labelRes)) },
            selected = selected.key,
            onSelect = { key -> MainTab.fromKey(key)?.let(onSelect) },
        )
    }
}

val RECRUIT_SECTIONS = listOf(MainTab.JOBS, MainTab.SEEK)
val SPACE_SECTIONS = listOf(MainTab.SPACES, MainTab.TRANSFERS)

@Composable
fun spaceTitle() = stringResource(R.string.tab_space)

@Composable
fun recruitTitle() = stringResource(R.string.tab_recruit)
