package com.muyeon.app.ui.dancer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.common.MuyeonPillTab
import com.muyeon.app.ui.common.MuyeonPillTabs
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.ChipRow
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.dancer.components.DancerRow
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.teacher.LessonGenres
import org.koin.androidx.compose.koinViewModel

/** 무용수 프로필 열람 — 웹 /dancers(전공 칩 · 성별/영상/수상 칩 · 목록 · 찜). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DancerListScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, onLogin: () -> Unit, viewModel: DancerListViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val list = rememberLazyListState()
    CursorLoadMoreEffect(list, canLoad = s.cursor.canAutoLoad && !s.isLoadingMore, onLoadMore = viewModel::loadMore)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is DancerEvent.OpenDetail -> onOpen(e.id)
                DancerEvent.LoginRequired -> onLogin()
                is DancerEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
                else -> Unit
            }
        }
    }
    val all = stringResource(R.string.common_all)
    val majors = listOf(ChipOption(DancerListViewModel.ALL, all)) + LessonGenres.ALL.map { ChipOption(it, it) }
    val f = s.filter
    // 둘째 줄: 성별(전체·여성·남성 하나) + 영상 보유·수상 경력(켜고 끄기).
    val female = stringResource(R.string.dancer_female)
    val male = stringResource(R.string.dancer_male)
    val second = listOf(
        MuyeonPillTab("G:ALL", all), MuyeonPillTab("G:$female", female), MuyeonPillTab("G:$male", male),
        MuyeonPillTab("V", stringResource(R.string.dancer_has_video)), MuyeonPillTab("A", stringResource(R.string.dancer_has_award)),
    )
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.dancer_list_title), primary = false, onBack = onBack)
        ChipRow(majors, f.major ?: DancerListViewModel.ALL, { viewModel.setFilter(f.copy(major = it.takeIf { v -> v != DancerListViewModel.ALL })) },
            Modifier.padding(top = Dimensions.spacingS))
        MuyeonPillTabs(
            second, "", onSelect = { k ->
                when {
                    k.startsWith("G:") -> viewModel.setFilter(f.copy(gender = k.removePrefix("G:").takeIf { it != "ALL" }))
                    k == "V" -> viewModel.setFilter(f.copy(hasVideo = !f.hasVideo))
                    k == "A" -> viewModel.setFilter(f.copy(hasAward = !f.hasAward))
                }
            },
            compact = true, fixedWeight = true,
            isSelected = { k ->
                when {
                    k == "G:ALL" -> f.gender == null
                    k.startsWith("G:") -> f.gender == k.removePrefix("G:")
                    k == "V" -> f.hasVideo
                    else -> f.hasAward
                }
            },
        )
        PullToRefreshBox(s.isRefreshing, onRefresh = { viewModel.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), state = list) {
                when {
                    s.items.isEmpty() && s.isInitialLoading -> Unit
                    s.items.isEmpty() && s.errorMessage != null -> item {
                        EmptyView(stringResource(R.string.common_list_failed), content = s.errorMessage,
                            buttonText = stringResource(R.string.common_retry), onButton = { viewModel.refresh() })
                    }
                    s.items.isEmpty() -> item { EmptyView(stringResource(R.string.dancer_empty)) }
                    else -> itemsIndexed(s.items, key = { _, d -> d.id }) { i, d ->
                        DancerRow(d, d.id in s.likedIds, onClick = { viewModel.open(d.id) }, onLike = { viewModel.toggleLike(d.id) }, showDivider = i < s.items.lastIndex)
                    }
                }
                item { CursorListFooter(s.isLoadingMore, s.moreError, s.cursor.needsTap || s.moreError != null, viewModel::loadMore) }
            }
        }
    }
    CustomDialog(
        title = stringResource(R.string.dancer_read_notice), content = null, leftButtonText = null,
        rightButtonText = stringResource(R.string.common_confirm), buttonCount = 1, alignment = ContentAlignment.Middle,
        onDismiss = viewModel::dismissNotice, onRightButtonClick = viewModel::dismissNotice, showPopup = s.readNotice,
    )
}
