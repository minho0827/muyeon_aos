package com.muyeon.app.ui.reservation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.common.ListFilterButton
import com.muyeon.app.ui.common.ListFilterEmpty
import com.muyeon.app.ui.common.ListFilterSheet
import com.muyeon.app.ui.common.MuyeonPillTab
import com.muyeon.app.ui.common.MuyeonPillTabs
import com.muyeon.app.ui.common.appliedPills
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.reservation.components.CompletedLessonRow
import com.muyeon.app.ui.reservation.components.ReservationRow
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.domain.model.reservation.CompletedLesson
import com.muyeon.domain.model.reservation.ReservationCard
import com.muyeon.domain.model.reservation.ReservationCategory
import org.koin.androidx.compose.koinViewModel

/**
 * 내가 예약한 내역 — 웹 /myReservations. 예약을 누르면 예약 상세(앱 화면 [onOpenReservation]),
 *   레슨 완료는 후기 쓰기 또는 내 후기([onReview]). 예약이 없으면 빈 화면에 [레슨 둘러보기].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReservationsScreen(
    onBack: () -> Unit,
    onOpenReservation: (Long) -> Unit,
    onReview: (CompletedLesson) -> Unit,
    onBrowseLessons: () -> Unit,
    viewModel: MyReservationsViewModel = koinViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val list = rememberLazyListState()
    var sheet by remember { mutableStateOf(false) }
    CursorLoadMoreEffect(list, canLoad = s.cursor.canAutoLoad && !s.isLoadingMore, onLoadMore = viewModel::loadMore)
    val tabs = listOf(
        MuyeonPillTab(ReservationCategory.PERSONAL, stringResource(R.string.reservation_tab_personal)),
        MuyeonPillTab(ReservationCategory.ACADEMY, stringResource(R.string.reservation_tab_academy)),
        MuyeonPillTab(ReservationCategory.DONE, stringResource(R.string.reservation_tab_done)),
        MuyeonPillTab(ReservationCategory.CANCELED, stringResource(R.string.reservation_tab_canceled)),
    )

    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.reservation_title), primary = false, onBack = onBack)
        MuyeonPillTabs(
            tabs, s.category, viewModel::selectCategory, tagPrefix = "reservation.tab", fixedWeight = true,
            leadingPills = s.filter.appliedPills(emptyList(), viewModel::setFilter),
            trailing = { ListFilterButton(s.filter.isActive, onClick = { sheet = true }) },
        )
        PullToRefreshBox(s.isRefreshing, onRefresh = { viewModel.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), state = list) {
                when {
                    s.items.isEmpty() && s.isInitialLoading -> Unit
                    s.items.isEmpty() && s.errorMessage != null -> item {
                        EmptyView(stringResource(R.string.common_list_failed), content = s.errorMessage,
                            buttonText = stringResource(R.string.common_retry), onButton = { viewModel.refresh() })
                    }
                    s.items.isEmpty() && s.filter.isActive -> item { ListFilterEmpty(onReset = { viewModel.setFilter(com.muyeon.app.ui.common.ListFilter()) }) }
                    s.items.isEmpty() -> item {
                        EmptyView(
                            stringResource(
                                when (s.category) {
                                    ReservationCategory.PERSONAL -> R.string.reservation_empty_personal
                                    ReservationCategory.ACADEMY -> R.string.reservation_empty_academy
                                    ReservationCategory.DONE -> R.string.reservation_empty_done
                                    ReservationCategory.CANCELED -> R.string.reservation_empty_canceled
                                },
                            ),
                            buttonText = stringResource(R.string.reservation_browse).takeIf { s.category != ReservationCategory.CANCELED },
                            onButton = onBrowseLessons,
                        )
                    }
                    else -> itemsIndexed(s.items, key = { _, r -> r.key }) { i, r ->
                        val last = i == s.items.lastIndex
                        when (r) {
                            is ReservationCard -> ReservationRow(r, onClick = if (r.canceled) null else ({ onOpenReservation(r.id) }), showDivider = !last)
                            is CompletedLesson -> CompletedLessonRow(r, onClick = { onReview(r) }, showDivider = !last)
                        }
                    }
                }
                item { CursorListFooter(s.isLoadingMore, s.moreError, s.cursor.needsTap || s.moreError != null, viewModel::loadMore) }
            }
        }
    }
    if (sheet) ListFilterSheet(s.filter, onApply = { viewModel.setFilter(it); sheet = false }, onDismiss = { sheet = false })
}
