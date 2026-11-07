package com.muyeon.app.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.UnderlineTabs
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.home.components.ArtistCard
import com.muyeon.app.ui.home.components.HomeBanner
import com.muyeon.app.ui.home.components.HomeBannerPager
import com.muyeon.app.ui.home.components.ArtistsHeading
import com.muyeon.app.ui.home.components.HomeLegalFooter
import com.muyeon.app.ui.home.components.HomeTopBar
import com.muyeon.app.ui.home.components.LessonCategoryIcons
import com.muyeon.app.ui.home.components.LessonProductCard
import com.muyeon.app.ui.home.components.LessonSection
import com.muyeon.app.ui.main.MainTab
import com.muyeon.app.ui.main.TabReselectEffect
import com.muyeon.app.ui.main.tabBarBottomPadding
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.domain.model.home.HomeLesson
import com.muyeon.domain.model.teacher.LessonGenres
import com.muyeon.domain.usecase.home.HomeArtistsUseCase
import org.koin.androidx.compose.koinViewModel

/**
 * ★ 2026-11-08 홈 — 모든 회원·비회원 같은 화면(사용자 결정). 위 바 [舞踊緣][강사 | 레슨][🔔] + 밑줄 탭.
 *   강사: COREO 에이전시형 — 넓은 사진 띠 · ARTISTS · 2열 큰 세로 사진 그리드(끝없이 내려 받기).
 *   레슨: 뷰티컬리 — 추천 탭(배너 · 장르 아이콘 · 섹션) / 장르 탭(그 장르 레슨 2열 그리드).
 *   맨 아래 사업자 정보. 홈 칸 다시 누르기 = 강사 ↔ 레슨.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(isLoggedIn: Boolean, actions: HomeActions, viewModel: HomeViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(isLoggedIn) { viewModel.onSession(isLoggedIn) }
    LifecycleResumeEffect(Unit) { viewModel.onSession(isLoggedIn); onPauseOrDispose { } }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is HomeEvent.ScrapToast -> Toast.makeText(context, if (e.added) R.string.common_scrap_added else R.string.common_scrap_removed, Toast.LENGTH_SHORT).show()
                is HomeEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
                HomeEvent.LoginRequired -> actions.login()
            }
        }
    }

    val all = stringResource(R.string.common_all)
    val recommend = stringResource(R.string.home_lesson_recommend)
    val teacherTabs = listOf(ChipOption(HomeArtistsUseCase.ALL, all)) + LessonGenres.ALL.map { ChipOption(it, it) }
    val lessonTabs = listOf(ChipOption(HomeUiState.LESSON_RECOMMEND, recommend)) + LessonGenres.ALL.map { ChipOption(it, it) }
    val openLesson: (HomeLesson) -> Unit = { actions.openPath("/lessons/${it.id}") }
    val lessonList: (String, String) -> Unit = { title, type -> actions.openPath("/lessonList?title=${Uri.encode(title)}&type=$type") }

    // 강사·레슨 그리드를 따로 둔다 — 토글을 오가도 각자 스크롤 자리를 지킨다.
    val teacherGrid = rememberLazyGridState()
    val lessonGrid = rememberLazyGridState()
    val grid = if (s.mode == HomeMode.TEACHERS) teacherGrid else lessonGrid
    // 홈 칸을 한 번 더 누르면 강사 ↔ 레슨(컬리 홈 칸처럼 — 사용자 요청 2026-11-08).
    TabReselectEffect(MainTab.HOME) { viewModel.setMode(if (s.mode == HomeMode.TEACHERS) HomeMode.LESSONS else HomeMode.TEACHERS) }
    CursorLoadMoreEffect(teacherGrid, canLoad = s.mode == HomeMode.TEACHERS && s.artists.cursor.canAutoLoad && !s.artists.isLoadingMore, onLoadMore = viewModel::loadMoreArtists)
    CursorLoadMoreEffect(lessonGrid, canLoad = s.mode == HomeMode.LESSONS && s.lessonTab != HomeUiState.LESSON_RECOMMEND &&
        s.genreLessons.cursor.canAutoLoad && !s.genreLessons.isLoadingMore, onLoadMore = viewModel::loadMoreGenreLessons)

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        HomeTopBar(s.mode, viewModel::setMode, s.unread, onBell = { actions.native("openNotifications", emptyMap()) })
        if (s.mode == HomeMode.TEACHERS) UnderlineTabs(teacherTabs, s.teacherGenre, viewModel::selectTeacherGenre)
        else UnderlineTabs(lessonTabs, s.lessonTab, viewModel::selectLessonTab)

        PullToRefreshBox(s.isRefreshing, onRefresh = viewModel::pullRefresh, modifier = Modifier.fillMaxSize()) {
            if (s.mode == HomeMode.TEACHERS) {
                LazyVerticalGrid(
                    GridCells.Fixed(2), Modifier.fillMaxSize(), teacherGrid,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = tabBarBottomPadding()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val a = s.artists
                    if (s.teacherGenre == HomeArtistsUseCase.ALL) item(key = "hero", span = FULL) {
                        // 띠는 화면 끝까지(그리드 좌우 여백을 넘어선다).
                        // 맨 위 넘기는 배너 — 사진 있는 강사(부스트 먼저, 서버 순서) 최대 10명.
                        HomeBannerPager(
                            a.items.filter { it.image != null }.take(BANNER_COUNT).map { t ->
                                HomeBanner(t.id, t.image!!, t.name, t.genres.joinToString(" · ").ifBlank { null }) { openProfile(actions, t.id) }
                            },
                            onAll = null, modifier = Modifier.bleed(16.dp),
                        )
                    }
                    item(key = "heading", span = FULL) { ArtistsHeading(s.teacherGenre.takeIf { it != HomeArtistsUseCase.ALL }) }
                    when {
                        a.items.isEmpty() && a.isInitialLoading -> Unit
                        a.items.isEmpty() -> item(key = "empty", span = FULL) {
                            EmptyView(stringResource(if (a.errorMessage != null) R.string.home_artists_failed else R.string.home_artists_empty),
                                buttonText = a.errorMessage?.let { stringResource(R.string.common_retry) }, onButton = viewModel::pullRefresh)
                        }
                        else -> items(a.items, key = { it.id }) { t -> ArtistCard(t, onClick = { openProfile(actions, t.id) }) }
                    }
                    item(key = "more", span = FULL) { CursorListFooter(a.isLoadingMore, a.moreError, a.cursor.needsTap || a.moreError != null, viewModel::loadMoreArtists) }
                    if (!a.cursor.hasMore && a.items.isNotEmpty()) footer(s, actions)
                }
            } else {
                LazyVerticalGrid(
                    GridCells.Fixed(2), Modifier.fillMaxSize(), lessonGrid,
                    contentPadding = PaddingValues(bottom = tabBarBottomPadding()),
                ) {
                    if (s.lessonTab == HomeUiState.LESSON_RECOMMEND) {
                        val p = s.preview
                        item(key = "banner", span = FULL) {
                            HomeBannerPager(
                                p.popular.filter { it.image != null }.map { l -> HomeBanner(l.id, l.image!!, l.title, l.place) { openLesson(l) } },
                                onAll = { lessonList(recommendTitle(context), "popular") },
                            )
                        }
                        item(key = "icons", span = FULL) { LessonCategoryIcons(LessonGenres.ALL, viewModel::selectLessonTab) }
                        item(key = "popular", span = FULL) {
                            LessonSection(stringResource(R.string.home_sec_recommend), stringResource(R.string.home_sec_popular_desc), p.popular, null, s.likedLessons,
                                openLesson, viewModel::toggleLessonLike, onAll = { lessonList(recommendTitle(context), "popular") })
                        }
                        item(key = "experience", span = FULL) {
                            val t = stringResource(R.string.home_list_experience)
                            LessonSection(stringResource(R.string.home_sec_experience), stringResource(R.string.home_sec_experience_desc), p.experience,
                                stringResource(R.string.home_tag_experience), s.likedLessons, openLesson, viewModel::toggleLessonLike, onAll = { lessonList(t, "experience") })
                        }
                        item(key = "oneday", span = FULL) {
                            val t = stringResource(R.string.home_list_oneday)
                            LessonSection(stringResource(R.string.home_sec_oneday), stringResource(R.string.home_sec_oneday_desc), p.oneday,
                                stringResource(R.string.home_tag_oneday), s.likedLessons, openLesson, viewModel::toggleLessonLike, onAll = { lessonList(t, "oneday") })
                        }
                        item(key = "newest", span = FULL) {
                            LessonSection(stringResource(R.string.home_sec_newest), stringResource(R.string.home_sec_newest_desc), s.newest, null, s.likedLessons,
                                openLesson, viewModel::toggleLessonLike, onAll = null)
                        }
                        if (s.previewLoaded && p.popular.isEmpty() && p.experience.isEmpty() && p.oneday.isEmpty() && s.newest.isEmpty()) {
                            item(key = "empty", span = FULL) { EmptyView(stringResource(R.string.home_lessons_empty)) }
                        }
                        item(key = "gap", span = FULL) { Spacer(Modifier.height(24.dp)) }
                        footer(s, actions)
                    } else {
                        val g = s.genreLessons
                        item(key = "top", span = FULL) { Spacer(Modifier.height(16.dp)) }
                        when {
                            g.items.isEmpty() && g.isInitialLoading -> Unit
                            g.items.isEmpty() -> item(key = "empty", span = FULL) {
                                EmptyView(stringResource(if (g.errorMessage != null) R.string.home_lessons_failed else R.string.home_lessons_genre_empty),
                                    buttonText = g.errorMessage?.let { stringResource(R.string.common_retry) }, onButton = viewModel::pullRefresh)
                            }
                            else -> itemsIndexed(g.items, key = { _, it -> it.id }) { i, l ->
                                // 2열 — 바깥 20 · 가운데 10(왼쪽 칸은 오른쪽 5, 오른쪽 칸은 왼쪽 5).
                                val left = i % 2 == 0
                                LessonProductCard(l, null, l.id in s.likedLessons, onClick = { openLesson(l) }, onToggleLike = { viewModel.toggleLessonLike(l.id) },
                                    modifier = Modifier.padding(start = if (left) 20.dp else 5.dp, end = if (left) 5.dp else 20.dp, bottom = 22.dp))
                            }
                        }
                        item(key = "more", span = FULL) { CursorListFooter(g.isLoadingMore, g.moreError, g.cursor.needsTap || g.moreError != null, viewModel::loadMoreGenreLessons) }
                    }
                }
            }
        }
    }
}

private const val BANNER_COUNT = 10

private val FULL: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

/** 그리드 좌우 여백을 넘어 화면 끝까지 그린다(COREO 첫 사진 띠). */
private fun Modifier.bleed(side: androidx.compose.ui.unit.Dp) = layout { m, c ->
    val extra = (side * 2).roundToPx()
    val p = m.measure(c.copy(minWidth = c.minWidth + extra, maxWidth = c.maxWidth + extra))
    layout(c.maxWidth, p.height) { p.place(-extra / 2, 0) }
}

private fun openProfile(actions: HomeActions, id: Long) = actions.native("openPublicProfile", mapOf("userId" to id.toString()))

private fun recommendTitle(context: android.content.Context) = context.getString(R.string.home_list_popular)

/** 맨 아래 — 사업자 정보(법적 고지, 두 모드 공통). 공지는 홈에서 뺐다(사용자 결정 2026-11-08 — 나중에 MY). */
private fun LazyGridScope.footer(s: HomeUiState, actions: HomeActions) {
    item(key = "legal", span = FULL) { Column(Modifier.padding(top = 32.dp)) { HomeLegalFooter(actions.openPath, actions.dial, actions.mail) } }
}
