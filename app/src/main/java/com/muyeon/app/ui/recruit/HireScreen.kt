package com.muyeon.app.ui.recruit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.TopBarIconButton
import com.muyeon.app.ui.components.ViewModeToggle
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.lessons.LessonLabels
import com.muyeon.app.ui.main.MainTab
import com.muyeon.app.ui.main.TabReselectEffect
import com.muyeon.app.ui.main.tabBarBottomPadding
import com.muyeon.app.ui.recruit.components.FilterGroup
import com.muyeon.app.ui.recruit.components.JobCardView
import com.muyeon.app.ui.recruit.components.RecruitFilterSheet
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.domain.model.recruit.JobFilter
import org.koin.androidx.compose.koinViewModel

/** 구인(채용공고) 목록 — 웹 JobBoard tab=hire. 카드를 누르면 공고 상세(지금은 웹 /jobs/:id). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HireScreen(actions: RecruitActions, vm: HireViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    RecruitEventsEffect(vm.events, actions)
    LifecycleResumeEffect(Unit) { vm.onResume(); onPauseOrDispose { } }
    val grid = rememberLazyGridState()
    TabReselectEffect(MainTab.JOBS) { grid.animateScrollToItem(0) }
    CursorLoadMoreEffect(grid, canLoad = s.list.cursor.canAutoLoad && !s.list.isLoadingMore, onLoadMore = vm::loadMore)
    var sheet by remember { mutableStateOf(false) }
    val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    val applied = stringResource(R.string.home_job_applied)
    val viewed = stringResource(R.string.home_job_viewed)
    val open: (Long) -> Unit = { id -> actions.openPath("/jobs/$id") }

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        // 채용 — 위 바 + 구인·구직 밑줄 탭(사용자 결정 2026-11-08).
        com.muyeon.app.ui.main.TabSectionHeader(com.muyeon.app.ui.main.recruitTitle(), com.muyeon.app.ui.main.RECRUIT_SECTIONS, MainTab.JOBS, actions.selectTab) {
            TopBarIconButton(R.drawable.ic_web_filter, stringResource(R.string.recruit_filter), onClick = { sheet = true })
        }
        PullToRefreshBox(s.isRefreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                GridCells.Fixed(if (s.grid) 2 else 1), Modifier.fillMaxSize(), grid,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = tabBarBottomPadding()),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (s.forMe.isNotEmpty()) item(key = "forMe", span = full) {
                    Column(Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.recruit_for_me), style = AppTypography.Section16, color = AppColors.TextHead)
                        LazyRow(contentPadding = PaddingValues(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(s.forMe, key = { "m${it.id}" }) { j ->
                                JobCardView(j, true, null, j.id in s.liked, { open(j.id) }, { vm.toggleLike(j.id) }, Modifier.width(200.dp))
                            }
                        }
                    }
                }
                item(key = "bar", span = full) {
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        ViewModeToggle(s.grid, vm::setGrid)
                        if (s.filter.isActive) Text(stringResource(R.string.recruit_filter_on), style = AppTypography.Caption12, color = AppColors.Primary,
                            modifier = Modifier.padding(start = 12.dp))
                    }
                }
                val l = s.list
                when {
                    l.items.isEmpty() && l.isInitialLoading -> Unit
                    l.items.isEmpty() -> item(key = "empty", span = full) {
                        EmptyView(l.errorMessage ?: stringResource(R.string.recruit_hire_empty),
                            buttonText = l.errorMessage?.let { stringResource(R.string.common_retry) }, onButton = { vm.refresh() })
                    }
                    else -> itemsIndexed(l.items, key = { _, it -> it.id }) { _, j ->
                        val status = when (j.id) { in s.applied -> applied; in s.viewed -> viewed; else -> null }
                        JobCardView(j, s.grid, status, j.id in s.liked, { open(j.id) }, { vm.toggleLike(j.id) })
                    }
                }
                item(key = "more", span = full) { CursorListFooter(l.isLoadingMore, l.moreError, l.cursor.needsTap || l.moreError != null, vm::loadMore) }
            }
        }
    }
    if (sheet) {
        val careers = RecruitLabels.CAREERS.map { ChipOption(it, LessonLabels.career(it)?.let { r -> stringResource(r) } ?: it) }
        val salaries = RecruitLabels.SALARIES.map { ChipOption(it, HomeLabels.salary(it)?.let { r -> stringResource(r) } ?: it) }
        RecruitFilterSheet(
            listOf(FilterGroup("career", stringResource(R.string.recruit_f_career), careers, true), FilterGroup("salary", stringResource(R.string.recruit_f_salary), salaries, true)),
            mapOf("career" to s.filter.careers, "salary" to s.filter.salaries),
            onApply = { m -> sheet = false; vm.applyFilter(JobFilter(m["career"].orEmpty(), m["salary"].orEmpty())) },
            onDismiss = { sheet = false },
        )
    }
}
