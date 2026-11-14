package com.muyeon.app.ui.recruit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.ChipRow
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.TopBarIconButton
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.lessons.LessonLabels
import com.muyeon.app.ui.main.MainTab
import com.muyeon.app.ui.main.TabReselectEffect
import com.muyeon.app.ui.main.tabBarBottomPadding
import com.muyeon.app.ui.recruit.components.FilterGroup
import com.muyeon.app.ui.recruit.components.RecruitFilterSheet
import com.muyeon.app.ui.recruit.components.SubRowView
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.domain.model.recruit.SubFilter
import com.muyeon.domain.model.teacher.LessonGenres
import com.muyeon.app.ui.theme.Dimensions
import org.koin.androidx.compose.koinViewModel

/** 대타공고 목록 — 웹 SubList. 장르 칩 + 필터 시트. 줄을 누르면 상세(지금은 웹 /subs/:id). ＋ 는 등록(설문식 등록이 생기면 앱 화면). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubsScreen(actions: RecruitActions, vm: SubsViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) { vm.onResume(); onPauseOrDispose { } }
    val list = rememberLazyListState()
    TabReselectEffect(MainTab.SUBS) { list.animateScrollToItem(0) }
    var sheet by remember { mutableStateOf(false) }
    val all = stringResource(R.string.common_all)

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        MainTopBar(stringResource(R.string.tab_subs)) {
            TopBarIconButton(R.drawable.ic_web_filter, stringResource(R.string.recruit_filter), onClick = { sheet = true })
        }
        PullToRefreshBox(s.isRefreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), list, contentPadding = PaddingValues(bottom = tabBarBottomPadding())) {
                item(key = "chips") {
                    ChipRow(listOf(ChipOption(ALL, all)) + LessonGenres.ALL.map { ChipOption(it, it) }, s.filter.genre ?: ALL,
                        { vm.selectGenre(it.takeIf { g -> g != ALL }) }, Modifier.fillMaxWidth().padding(vertical = 16.dp))
                }
                when {
                    s.items.isEmpty() && s.loading -> Unit
                    s.items.isEmpty() -> item(key = "empty") {
                        EmptyView(s.errorMessage ?: stringResource(R.string.recruit_sub_empty), Modifier.padding(top = Dimensions.emptyNoTabsOffset),
                            buttonText = s.errorMessage?.let { stringResource(R.string.common_retry) }, onButton = { vm.refresh() })
                    }
                    else -> items(s.items, key = { it.id }) { row -> SubRowView(row) { actions.openPath("/subs/${row.id}") } }
                }
            }
        }
    }
    if (sheet) {
        val f = s.filter
        val groups = listOf(
            FilterGroup("schedule", stringResource(R.string.recruit_f_schedule), RecruitLabels.SCHEDULES.map { ChipOption(it, RecruitLabels.schedule(it)?.let { r -> stringResource(r) } ?: it) }, false),
            FilterGroup("count", stringResource(R.string.recruit_f_count), RecruitLabels.CLASS_COUNTS.map { ChipOption(it.toString(), stringResource(R.string.recruit_class_count_plus, it)) }, false),
            FilterGroup("field", stringResource(R.string.recruit_f_field), RecruitLabels.FIELDS.map { ChipOption(it, LessonLabels.field(it)?.let { r -> stringResource(r) } ?: it) }, false),
            FilterGroup("target", stringResource(R.string.recruit_f_target), RecruitLabels.TARGETS.map { ChipOption(it, RecruitLabels.target(it)?.let { r -> stringResource(r) } ?: it) }, false),
            FilterGroup("salary", stringResource(R.string.recruit_f_salary), RecruitLabels.SALARIES.map { ChipOption(it, HomeLabels.salary(it)?.let { r -> stringResource(r) } ?: it) }, false),
        )
        RecruitFilterSheet(
            groups,
            mapOf("schedule" to setOfNotNull(f.scheduleType), "count" to setOfNotNull(f.minClassCount?.toString()), "field" to setOfNotNull(f.field),
                "target" to setOfNotNull(f.target), "salary" to setOfNotNull(f.salary)),
            onApply = { m ->
                sheet = false
                vm.applyFilter(f.copy(scheduleType = m["schedule"]?.firstOrNull(), minClassCount = m["count"]?.firstOrNull()?.toIntOrNull(),
                    field = m["field"]?.firstOrNull(), target = m["target"]?.firstOrNull(), salary = m["salary"]?.firstOrNull()))
            },
            onDismiss = { sheet = false },
        )
    }
}

private const val ALL = "ALL"
