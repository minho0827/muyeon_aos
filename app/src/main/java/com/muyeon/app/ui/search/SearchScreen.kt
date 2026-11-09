package com.muyeon.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.muyeon.app.ui.castingstage.rememberStageApi
import com.muyeon.app.ui.theme.AppColors
import org.koin.compose.koinInject

/**
 * ★ 2026-11-09 공연·캐스팅 검색(NOL 식) — 위 바(← 입력 ⓧ 홈) · 밑줄 탭 · 고른 도메인의 결과 칸.
 *   검색어는 두 탭이 같이 쓰고, 칩·결과는 도메인마다 따로 지킨다.
 *
 * @param initialKey 처음 고를 탭([SearchDomainKeys])
 * @param onOpen 결과 누름 — 바깥 NavHost 경로
 */
@Composable
fun SearchScreen(
    domains: List<SearchDomain<*>>,
    initialKey: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(domains.firstOrNull { it.key == initialKey }?.key ?: domains.first().key) }

    // 흰 바 — 상태바 아이콘 검정.
    val view = LocalView.current
    if (!view.isInEditMode) DisposableEffect(Unit) {
        (view.context as? android.app.Activity)?.window?.let { w ->
            androidx.core.view.WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = true
        }
        onDispose { }
    }

    Column(Modifier.fillMaxSize().background(AppColors.Surface).statusBarsPadding()) {
        SearchTopBar(query = query, onQuery = { query = it }, onBack = onBack, onHome = onHome)
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        DomainTabs(domains, selected, onSelect = { selected = it; focusManager.clearFocus() })  // 탭 바꾸기 = 결과 보기
        val domain = domains.first { it.key == selected }
        key(domain.key) { SearchResultPane(domain, query, onOpen) }
    }
}

/** 검색 도메인 두 개 — 공연(Koin) · 캐스팅(캐스팅·무대 API). 순서 = 탭 순서. */
@Composable
fun rememberSearchDomains(): List<SearchDomain<*>> {
    val performance = koinInject<PerformanceSearchDomain>()
    val stageApi = rememberStageApi()
    return remember(performance, stageApi) { listOf(performance, CastingSearchDomain(stageApi)) }
}
