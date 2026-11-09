package com.muyeon.app.ui.castingstage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.castingstage.data.CastingStageApi
import com.muyeon.app.ui.castingstage.model.StageCategory
import com.muyeon.app.ui.castingstage.model.StageNewsItem
import com.muyeon.app.ui.components.BrandSearchBar
import com.muyeon.app.ui.main.tabBarBottomPadding
import com.muyeon.app.ui.performance.components.PerfCastingHeader
import com.muyeon.app.ui.performance.components.PerfCastingItem
import com.muyeon.app.ui.performance.components.PerfCastingMode

/**
 * ★ 2026-11-09 공연·캐스팅 통합 탭 — 캐스팅 칸.
 *   검색창 위 바 · [공연][캐스팅] | 캐스팅 공고·국내소식… 줄(섹션 이동) · 캐스팅·무대 본문([CastingStageBody]).
 *   공연 공고 등록 ＋ 는 하단 [+ 글쓰기]가, 공고 전체 보기는 섹션 [더 보기]가 맡는다.
 */
@Composable
fun CastingHomeScreen(
    api: CastingStageApi,
    onSearch: () -> Unit,
    onMode: (PerfCastingMode) -> Unit,
    onOpenCasting: (Int) -> Unit,
    onOpenNews: (StageNewsItem, StageCategory) -> Unit,
    onCastingList: () -> Unit,
    onNewsList: (Int, String) -> Unit,
    onIssues: () -> Unit,
    reloadKey: Int = 0,
) {
    val body = rememberCastingStageBodyState()
    Column(Modifier.fillMaxSize().background(Color.White)) {
        BrandSearchBar(onClick = onSearch)
        PerfCastingHeader(
            mode = PerfCastingMode.CASTING,
            onMode = onMode,
            // ★ 11-09 이름·순서·보임은 서버 분류 목록(GET /stage-news/categories) 그대로.
            items = body.cats.map { PerfCastingItem(it.code, it.label) },
            selected = body.current,
            onSelect = body::jump,
        )
        CastingStageBody(
            api = api,
            state = body,
            sectionNav = false,
            bottomPadding = tabBarBottomPadding(),
            onOpenCasting = onOpenCasting,
            onOpenNews = onOpenNews,
            onCastingList = onCastingList,
            onNewsList = onNewsList,
            onIssues = onIssues,
            reloadKey = reloadKey,
        )
    }
}
