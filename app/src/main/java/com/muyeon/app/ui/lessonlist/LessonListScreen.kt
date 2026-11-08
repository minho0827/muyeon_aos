package com.muyeon.app.ui.lessonlist

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.ChipRow
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.ViewModeToggle
import com.muyeon.app.ui.home.components.LessonProductCard
import com.muyeon.app.ui.lessonlist.components.LessonListRow
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions

/**
 * 홈 '전체보기' 레슨 목록 — 웹 /lessonList(주황 위 바 · 제목은 ?title=).
 *   장르 칩 한 줄 · 오른쪽 그리드/목록 전환 · 2열 상품 카드(홈 LessonProductCard 그대로) 또는 한 줄 목록.
 *
 * @param onOpenLesson 레슨 상세(앱 화면)
 * @param onLogin 비회원 찜 — 로그인
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListScreen(
    title: String,
    viewModel: LessonListViewModel,
    onBack: () -> Unit,
    onOpenLesson: (Long) -> Unit,
    onLogin: () -> Unit,
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val grid = rememberLazyGridState()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is LessonListEvent.ScrapToast -> Toast.makeText(context, if (e.added) R.string.common_scrap_added else R.string.common_scrap_removed, Toast.LENGTH_SHORT).show()
                is LessonListEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
                LessonListEvent.LoginRequired -> onLogin()
            }
        }
    }
    CursorLoadMoreEffect(grid, canLoad = s.cursor.canAutoLoad && !s.isLoadingMore, onLoadMore = viewModel::loadMore)
    val all = stringResource(R.string.common_all)
    val chips = viewModel.genreOptions.map { ChipOption(it, if (it == LessonListViewModel.ALL) all else it) }

    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = title.ifBlank { stringResource(R.string.lesson_list_title) }, onBack = onBack)
        PullToRefreshBox(s.isRefreshing, onRefresh = { viewModel.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                GridCells.Fixed(if (s.grid) 2 else 1), Modifier.fillMaxSize(), grid,
                contentPadding = PaddingValues(bottom = Dimensions.spacing3XL),
            ) {
                item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
                    ChipRow(chips, s.genre, viewModel::selectGenre, Modifier.padding(top = Dimensions.spacingM))
                }
                item(key = "toggle", span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingM), horizontalArrangement = Arrangement.End) {
                        ViewModeToggle(s.grid, viewModel::setGrid)
                    }
                }
                when {
                    s.items.isEmpty() && s.isInitialLoading -> Unit
                    s.items.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        EmptyView(
                            when {
                                s.errorMessage != null -> stringResource(R.string.common_list_failed)
                                s.genre == LessonListViewModel.ALL -> stringResource(R.string.lesson_list_empty)
                                else -> stringResource(R.string.lesson_list_genre_empty, s.genre)
                            },
                            buttonText = s.errorMessage?.let { stringResource(R.string.common_retry) },
                            onButton = { viewModel.refresh() },
                        )
                    }
                    s.grid -> itemsIndexed(s.items, key = { _, l -> l.id }) { i, l ->
                        // 2열 — 바깥 20 · 가운데 10(홈 장르 목록과 같은 간격).
                        val left = i % 2 == 0
                        LessonProductCard(
                            l, null, l.id in s.likedIds, onClick = { onOpenLesson(l.id) }, onToggleLike = { viewModel.toggleLike(l.id) },
                            modifier = Modifier.padding(start = if (left) 20.dp else 5.dp, end = if (left) 5.dp else 20.dp, bottom = 22.dp),
                        )
                    }
                    else -> itemsIndexed(s.items, key = { _, l -> l.id }) { i, l ->
                        LessonListRow(l, l.id in s.likedIds, onClick = { onOpenLesson(l.id) }, onToggleLike = { viewModel.toggleLike(l.id) })
                    }
                }
                item(key = "more", span = { GridItemSpan(maxLineSpan) }) {
                    CursorListFooter(s.isLoadingMore, s.moreError, s.cursor.needsTap || s.moreError != null, viewModel::loadMore)
                }
                item(key = "end", span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.padding(bottom = Dimensions.spacingL)) }
            }
        }
    }
}
