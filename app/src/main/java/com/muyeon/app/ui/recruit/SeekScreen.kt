package com.muyeon.app.ui.recruit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.TopBarIconButton
import com.muyeon.app.ui.lessons.LessonLabels
import com.muyeon.app.ui.main.MainTab
import com.muyeon.app.ui.main.TabReselectEffect
import com.muyeon.app.ui.main.tabBarBottomPadding
import com.muyeon.app.ui.recruit.components.FilterGroup
import com.muyeon.app.ui.recruit.components.RecruitFilterSheet
import com.muyeon.app.ui.recruit.components.SeekerCardView
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.recruit.SeekFilter
import com.muyeon.domain.model.teacher.LessonGenres
import org.koin.androidx.compose.koinViewModel

/** 구직(이력서) 목록 — 웹 JobBoard tab=seek. 강사 카드는 앱 공개 프로필(구인구직 문맥), 무용수는 웹 /dancers/:id. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekScreen(actions: RecruitActions, vm: SeekViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    RecruitEventsEffect(vm.events, actions)
    val grid = rememberLazyGridState()
    TabReselectEffect(MainTab.SEEK) { grid.animateScrollToItem(0) }
    CursorLoadMoreEffect(grid, canLoad = s.list.cursor.canAutoLoad && !s.list.isLoadingMore, onLoadMore = vm::loadMore)
    var sheet by remember { mutableStateOf(false) }
    val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        // 채용 — 위 바 + 구인·구직 밑줄 탭(사용자 결정 2026-11-08).
        com.muyeon.app.ui.main.TabSectionHeader(com.muyeon.app.ui.main.recruitTitle(), com.muyeon.app.ui.main.RECRUIT_SECTIONS, MainTab.SEEK, actions.selectTab) {
            TopBarIconButton(R.drawable.ic_web_filter, stringResource(R.string.recruit_filter), onClick = { sheet = true })
            TopBarIconButton(R.drawable.ic_web_edit, stringResource(R.string.recruit_create_resume), onClick = { actions.openPath("/profileManage?seek=1&from=jobs") })
        }
        PullToRefreshBox(s.isRefreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                GridCells.Fixed(2), Modifier.fillMaxSize(), grid,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = tabBarBottomPadding()),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "top", span = full) { Spacer(Modifier.height(4.dp)) }
                val l = s.list
                when {
                    l.items.isEmpty() && l.isInitialLoading -> Unit
                    l.items.isEmpty() -> item(key = "empty", span = full) {
                        EmptyView(l.errorMessage ?: stringResource(R.string.recruit_seek_empty),
                            buttonText = l.errorMessage?.let { stringResource(R.string.common_retry) }, onButton = { vm.refresh() })
                    }
                    else -> items(l.items, key = { it.role + it.id }) { c ->
                        val dancer = c.role == ActiveType.DANCER
                        SeekerCardView(
                            c, c.id in (if (dancer) s.likedDancers else s.likedTeachers),
                            onClick = {
                                if (dancer) actions.openPath("/dancers/${c.id}")
                                else actions.native("openPublicProfile", mapOf("userId" to c.id.toString(), "src" to "jobs"))
                            },
                            onLike = { vm.toggleLike(c) },
                        )
                    }
                }
                item(key = "more", span = full) { CursorListFooter(l.isLoadingMore, l.moreError, l.cursor.needsTap || l.moreError != null, vm::loadMore) }
            }
        }
    }
    if (sheet) {
        val genres = LessonGenres.ALL.map { ChipOption(it, it) }
        val groups = if (s.teacherView) listOf(
            FilterGroup("types", stringResource(R.string.recruit_f_type), RecruitLabels.SEEK_TYPES.map { ChipOption(it, stringResource(RecruitLabels.seekType(it))) }, true),
            FilterGroup("fields", stringResource(R.string.recruit_f_genre), genres, true),
        ) else listOf(
            FilterGroup("fields", stringResource(R.string.recruit_f_genre), genres, true),
            FilterGroup("targets", stringResource(R.string.recruit_f_target), RecruitLabels.TARGETS.map { ChipOption(it, RecruitLabels.target(it)?.let { r -> stringResource(r) } ?: it) }, true),
            FilterGroup("careers", stringResource(R.string.recruit_f_career), RecruitLabels.CAREERS.map { ChipOption(it, LessonLabels.career(it)?.let { r -> stringResource(r) } ?: it) }, true),
        )
        RecruitFilterSheet(
            groups, mapOf("types" to s.filter.types, "fields" to s.filter.fields, "targets" to s.filter.targets, "careers" to s.filter.careers),
            onApply = { m -> sheet = false; vm.applyFilter(SeekFilter(m["types"].orEmpty(), m["fields"].orEmpty(), m["targets"].orEmpty(), m["careers"].orEmpty())) },
            onDismiss = { sheet = false },
        )
    }
}
