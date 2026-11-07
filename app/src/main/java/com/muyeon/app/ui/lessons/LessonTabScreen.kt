package com.muyeon.app.ui.lessons

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
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
import com.muyeon.app.ui.components.GuideBanner
import com.muyeon.app.ui.components.HelpGuideDialog
import com.muyeon.app.ui.components.HelpItem
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.TopBarIconButton
import com.muyeon.app.ui.components.TutorialAnchors
import com.muyeon.app.ui.components.TutorialOverlay
import com.muyeon.app.ui.components.TutorialStep
import com.muyeon.app.ui.components.tutorialAnchor
import com.muyeon.app.ui.lessons.components.LessonBoardHead
import com.muyeon.app.ui.lessons.components.LessonRegionFilterSheet
import com.muyeon.app.ui.lessons.components.LessonTeacherCard
import com.muyeon.app.ui.main.MainTab
import com.muyeon.app.ui.main.TabReselectEffect
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions
import org.koin.androidx.compose.koinViewModel

/**
 * 강사 탭 — 주황 위 바 "강사" + 오른쪽 이용 안내(?) · 안내 배너 · 장르 칩 · 보기 전환/필터 · 강사 카드(그리드/목록).
 *
 * @param onOpenProfile 강사 프로필(기존 앱 화면)
 * @param onRequestLesson 레슨 견적요청(기존 앱 견적 위저드) — (카테고리, 강사 id)
 * @param onLogin 비회원 찜 — 웹 로그인 창
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonTabScreen(
    onOpenProfile: (Long) -> Unit,
    onRequestLesson: (String, Long) -> Unit,
    onLogin: () -> Unit,
    viewModel: LessonTabViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val anchors = remember { TutorialAnchors() }

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    TabReselectEffect(MainTab.LESSONS) { viewModel.onTabReselect() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is LessonTabEvent.OpenProfile -> onOpenProfile(e.userId)
                is LessonTabEvent.RequestLesson -> onRequestLesson(e.category, e.teacherId)
                is LessonTabEvent.ScrapToast -> Toast.makeText(context, if (e.added) R.string.common_scrap_added else R.string.common_scrap_removed, Toast.LENGTH_SHORT).show()
                is LessonTabEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
                LessonTabEvent.LoginRequired -> onLogin()
                LessonTabEvent.ScrollToTop -> gridState.animateScrollToItem(0)
            }
        }
    }
    CursorLoadMoreEffect(gridState, canLoad = state.cursor.canAutoLoad && !state.isLoadingMore, onLoadMore = viewModel::loadMore)

    val all = stringResource(R.string.common_all)
    val shown = state.shown
    val full: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan) = { GridItemSpan(maxLineSpan) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
            MainTopBar(title = stringResource(R.string.tab_lessons)) {
                TopBarIconButton(R.drawable.ic_web_faq, stringResource(R.string.lesson_help_title), onClick = viewModel::openHelp)
            }
            PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = { viewModel.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (state.grid) 2 else 1),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Dimensions.spacingXXL),
                    horizontalArrangement = Arrangement.spacedBy(CARD_GAP),
                    verticalArrangement = Arrangement.spacedBy(CARD_GAP),
                ) {
                    if (state.showGuide) item(key = "guide", span = full) {
                        GuideBanner(
                            text = stringResource(R.string.lesson_guide),
                            onClose = viewModel::closeGuide,
                            modifier = Modifier.padding(start = Dimensions.gutter, end = Dimensions.gutter, top = Dimensions.spacingM),
                        )
                    }
                    item(key = "chips", span = full) {
                        ChipRow(
                            options = viewModel.genres.map { ChipOption(it, if (it == LessonTabViewModel.ALL) all else it) },
                            selected = state.genre,
                            onSelect = viewModel::selectGenre,
                            modifier = Modifier.fillMaxWidth().padding(top = Dimensions.spacingL).tutorialAnchor(anchors, "chips"),
                        )
                    }
                    item(key = "head", span = full) {
                        LessonBoardHead(grid = state.grid, filterOn = state.sido != null, onGrid = viewModel::setGrid, onFilter = { viewModel.openFilter(true) })
                    }
                    when {
                        shown.isEmpty() && state.isInitialLoading -> Unit
                        shown.isEmpty() -> item(key = "empty", span = full) {
                            EmptyView(
                                title = when {
                                    state.errorMessage != null -> stringResource(R.string.common_list_failed)
                                    state.sido != null -> stringResource(R.string.lesson_empty_region, state.sido!!)
                                    state.genre == LessonTabViewModel.ALL -> stringResource(R.string.lesson_empty_all)
                                    else -> stringResource(R.string.lesson_empty_genre, state.genre)
                                },
                                buttonText = state.errorMessage?.let { stringResource(R.string.common_retry) },
                                onButton = { viewModel.refresh() },
                            )
                        }
                        else -> itemsIndexed(shown, key = { _, t -> t.id }) { i, t ->
                            val first = i == 0
                            // 바깥 여백 20 — 그리드면 왼쪽 칸은 왼쪽만, 오른쪽 칸은 오른쪽만(칸 사이는 12).
                            val left = !state.grid || i % 2 == 0
                            val right = !state.grid || i % 2 == 1
                            LessonTeacherCard(
                                teacher = t,
                                grid = state.grid,
                                liked = t.id in state.likedIds,
                                showDm = state.showDm,
                                onClick = { viewModel.openProfile(t) },
                                onToggleLike = { viewModel.toggleLike(t.id) },
                                onRequest = { viewModel.requestLesson(t) },
                                // 그리드 바깥 여백(왼쪽 칸은 왼쪽만, 오른쪽 칸은 오른쪽만 20).
                                modifier = Modifier.padding(start = if (left) Dimensions.gutter else 0.dp, end = if (right) Dimensions.gutter else 0.dp),
                                anchors = if (first) anchors else null,
                            )
                        }
                    }
                    item(key = "footer", span = full) {
                        CursorListFooter(
                            loading = state.isLoadingMore,
                            error = state.moreError,
                            hasMore = state.cursor.needsTap || state.moreError != null,
                            onLoadMore = viewModel::loadMore,
                        )
                    }
                }
            }
        }
        if (state.showTutorial) {
            TutorialOverlay(
                steps = buildList {
                    add(TutorialStep("chips", stringResource(R.string.lesson_tut_chips)))
                    add(TutorialStep("heart", stringResource(R.string.lesson_tut_heart)))
                    add(TutorialStep("card", stringResource(R.string.lesson_tut_card)))
                    if (state.showDm) add(TutorialStep("quote", stringResource(R.string.lesson_tut_quote)))
                },
                anchors = anchors,
                onClose = viewModel::closeTutorial,
            )
        }
    }
    if (state.showFilter) LessonRegionFilterSheet(viewModel.sidoOptions, state.sido, viewModel::setSido) { viewModel.openFilter(false) }
    if (state.showHelp) HelpGuideDialog(
        title = stringResource(R.string.lesson_help_title),
        items = listOf(
            HelpItem("👤", stringResource(R.string.lesson_help_profile_t), stringResource(R.string.lesson_help_profile_d)),
            HelpItem("❤️", stringResource(R.string.lesson_help_like_t), stringResource(R.string.lesson_help_like_d)),
            HelpItem("🔍", stringResource(R.string.lesson_help_filter_t), stringResource(R.string.lesson_help_filter_d)),
            HelpItem("💬", stringResource(R.string.lesson_help_quote_t), stringResource(R.string.lesson_help_quote_d)),
        ),
        onClose = viewModel::closeHelpAndReplay,
    )
}

private val CARD_GAP = 12.dp
