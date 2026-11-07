package com.muyeon.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.collectAppResults
import com.muyeon.app.result.ResultKeys
import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.model.home.AcademyHomeProfile
import com.muyeon.domain.model.home.HomeLesson
import com.muyeon.domain.model.home.HomeLessonPreview
import com.muyeon.domain.model.home.HomeMyJob
import com.muyeon.domain.model.home.HomeNotice
import com.muyeon.domain.model.home.HomeQuote
import com.muyeon.domain.model.home.HomeRecruit
import com.muyeon.domain.model.home.HomeSpace
import com.muyeon.domain.model.home.HomeTransfer
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.model.teacher.TeacherCard
import com.muyeon.domain.usecase.home.HomeSwipeUseCase
import com.muyeon.domain.usecase.my.GetBadgeCountsUseCase
import com.muyeon.domain.usecase.region.ObserveActiveRegionUseCase
import com.muyeon.domain.usecase.scrap.GetLikedIdsUseCase
import com.muyeon.domain.usecase.scrap.ObserveScrapChangesUseCase
import com.muyeon.domain.usecase.scrap.ToggleScrapResult
import com.muyeon.domain.usecase.scrap.ToggleScrapUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 학원 홈 맞춤 묶음(웹 HomeSwipe isAcademy). */
data class AcademyHomeState(
    val profile: AcademyHomeProfile? = null,
    val recTeachers: List<TeacherCard> = emptyList(),
    val recLoaded: Boolean = false,
    val myLessons: List<HomeLesson> = emptyList(),
    val myLessonsLoaded: Boolean = false,
    val myJobs: List<HomeMyJob> = emptyList(),
    val myJobsLoaded: Boolean = false,
)

data class HomeUiState(
    val activeType: String = ActiveType.GENERAL,
    val isLoggedIn: Boolean = false,
    val region: ActiveRegion? = null,
    val unread: Int = 0,
    val isRefreshing: Boolean = false,
    // 일반·비회원
    val teachers: List<TeacherCard> = emptyList(),
    val preview: HomeLessonPreview = HomeLessonPreview(emptyList(), emptyList(), emptyList()),
    val recruits: List<HomeRecruit> = emptyList(),
    val jobNotice: List<HomeRecruit> = emptyList(),
    // 학원
    val academy: AcademyHomeState = AcademyHomeState(),
    // 같이 쓰는 것
    val spaces: List<HomeSpace> = emptyList(),
    val transfers: List<HomeTransfer> = emptyList(),
    val quotes: List<HomeQuote> = emptyList(),
    val subNotice: List<HomeRecruit> = emptyList(),
    val notices: List<HomeNotice> = emptyList(),
    val likedLessons: Set<Long> = emptySet(),
    val likedQuotes: Set<Long> = emptySet(),
    /** 추천 안 받기 시트 대상. */
    val blockTarget: TeacherCard? = null,
    val blockBusy: Boolean = false,
) {
    val isAcademy: Boolean get() = activeType == ActiveType.ACADEMY
}

sealed interface HomeEvent {
    data class ScrapToast(val added: Boolean) : HomeEvent
    data class Message(val text: String) : HomeEvent
    data class MessageRes(val res: Int) : HomeEvent
    data object LoginRequired : HomeEvent
}

/**
 * 홈(일반·비회원·학원 스와이프 홈) + 위 바(대표 지역·안 읽은 알림) — 웹 pages/muyeon/Home.js HomeSwipe · Header(isMain).
 *   강사 홈·클래식 홈은 각자 ViewModel([TeacherHomeViewModel] · [HomeClassicViewModel]), 위 바 값은 여기서 같이 쓴다.
 */
class HomeViewModel(
    private val home: HomeSwipeUseCase,
    observeRegion: ObserveActiveRegionUseCase,
    private val badges: GetBadgeCountsUseCase,
    private val getLikedIds: GetLikedIdsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    observeScrap: ObserveScrapChangesUseCase,
    private val tokens: TokenStore,
    private val activeTypeStore: ActiveTypeStore,
) : ViewModel() {
    private val regionFlow = observeRegion()
    private val _state = MutableStateFlow(HomeUiState(activeType = type(), isLoggedIn = tokens.isLoggedIn, region = regionFlow.value))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()
    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch { regionFlow.drop(1).collect { r -> _state.update { it.copy(region = r) }; load() } }
        viewModelScope.launch { observeScrap(ScrapTarget.LESSON).collect { c -> setLiked(lesson = true, c.id, c.liked) } }
        viewModelScope.launch { observeScrap(ScrapTarget.QUOTE).collect { c -> setLiked(lesson = false, c.id, c.liked) } }
        viewModelScope.collectAppResults(ResultKeys.NOTIFICATIONS) { refreshUnread() }
        viewModelScope.collectAppResults(ResultKeys.QUOTES, ResultKeys.LESSONS, ResultKeys.JOB_POSTINGS, ResultKeys.ACADEMY) { load(silent = true) }
    }

    private fun type() = ActiveType.visible(activeTypeStore.current())

    /** 화면 복귀 — 유형·로그인이 바뀌었으면 다시(웹은 user.id·activeType 이 바뀌면 다시 받는다). */
    fun onResume() {
        val t = type(); val li = tokens.isLoggedIn
        if (t != _state.value.activeType || li != _state.value.isLoggedIn) {
            _state.update { it.copy(activeType = t, isLoggedIn = li) }
            load()
        } else refreshUnread()
    }

    private fun refreshUnread() {
        if (!tokens.isLoggedIn) { _state.update { it.copy(unread = 0) }; return }
        viewModelScope.launch { _state.update { it.copy(unread = badges.noti()) } }
    }

    fun load(pull: Boolean = false, silent: Boolean = false) {
        loadJob?.cancel()
        if (pull) _state.update { it.copy(isRefreshing = true) }
        val s = _state.value
        loadJob = viewModelScope.launch {
            refreshUnread()
            val region = s.region
            val spaces = async { home.spaces() }
            val transfers = async { home.transfers() }
            val quotes = async { if (s.isAcademy) home.homeQuotes() else emptyList() }
            val subs = async { home.recruitNotice(HomeRecruit.Kind.SUB) }
            val notices = async { home.notices() }
            if (s.isAcademy) {
                // 학원 맞춤 — 조각마다 '불러왔음'을 따로(빈 상태 카드를 받은 뒤에만 보인다).
                launch { val p = home.academyProfile(); _state.update { it.copy(academy = it.academy.copy(profile = p)) } }
                launch { val r = home.recommendedTeachers(); _state.update { it.copy(academy = it.academy.copy(recTeachers = r, recLoaded = true)) } }
                launch { val l = home.myActiveLessons(); _state.update { it.copy(academy = it.academy.copy(myLessons = l, myLessonsLoaded = true)) } }
                launch { val j = home.myOpenJobs(); _state.update { it.copy(academy = it.academy.copy(myJobs = j, myJobsLoaded = true)) } }
            } else {
                val teachers = async { home.likedTeachers(region) }
                val preview = async { home.lessonPreview(region) }
                val recruits = async { home.recruits() }
                val jobNotice = async { if (!s.isLoggedIn) home.recruitNotice(HomeRecruit.Kind.JOB) else emptyList() }
                val p = (preview.await() as? ApiResult.Success)?.data
                _state.update {
                    it.copy(
                        teachers = teachers.await(),
                        preview = p?.let { d -> d.copy(popular = HomeSwipeUseCase.featuredFirst(d.popular), experience = d.experience.take(HomeSwipeUseCase.LESSON_LIMIT), oneday = d.oneday.take(HomeSwipeUseCase.LESSON_LIMIT)) } ?: it.preview,
                        recruits = recruits.await(),
                        jobNotice = jobNotice.await(),
                    )
                }
            }
            _state.update {
                it.copy(spaces = spaces.await(), transfers = transfers.await(), quotes = quotes.await(), subNotice = subs.await(), notices = notices.await(), isRefreshing = false)
            }
            loadLikes()
        }
    }

    private fun loadLikes() {
        viewModelScope.launch {
            val lessons = getLikedIds(ScrapTarget.LESSON)
            val quotes = if (_state.value.isAcademy) getLikedIds(ScrapTarget.QUOTE) else emptySet()
            _state.update { it.copy(likedLessons = lessons, likedQuotes = quotes) }
        }
    }

    private fun setLiked(lesson: Boolean, id: Long, liked: Boolean) = _state.update {
        if (lesson) it.copy(likedLessons = if (liked) it.likedLessons + id else it.likedLessons - id)
        else it.copy(likedQuotes = if (liked) it.likedQuotes + id else it.likedQuotes - id)
    }

    fun toggleLessonLike(id: Long) = toggle(ScrapTarget.LESSON, id, id in _state.value.likedLessons, lesson = true)
    fun toggleQuoteLike(id: Long) = toggle(ScrapTarget.QUOTE, id, id in _state.value.likedQuotes, lesson = false)

    private fun toggle(target: ScrapTarget, id: Long, was: Boolean, lesson: Boolean) {
        if (tokens.isLoggedIn) setLiked(lesson, id, !was)
        viewModelScope.launch {
            when (val r = toggleScrap(target, id, was)) {
                is ToggleScrapResult.Done -> _events.tryEmit(HomeEvent.ScrapToast(r.liked))
                is ToggleScrapResult.Failed -> { setLiked(lesson, id, was); _events.tryEmit(HomeEvent.Message(r.message)) }
                ToggleScrapResult.LoginRequired -> _events.tryEmit(HomeEvent.LoginRequired)
            }
        }
    }

    // 추천 안 받기
    fun openBlock(t: TeacherCard?) = _state.update { it.copy(blockTarget = t) }

    fun confirmBlock() {
        val t = _state.value.blockTarget ?: return
        _state.update { it.copy(blockBusy = true) }
        viewModelScope.launch {
            when (home.blockRecommendation(t.id)) {
                is ApiResult.Success -> {
                    _state.update { s ->
                        s.copy(blockTarget = null, blockBusy = false, teachers = s.teachers.filter { it.id != t.id },
                            academy = s.academy.copy(recTeachers = s.academy.recTeachers.filter { it.id != t.id }))
                    }
                    _events.tryEmit(HomeEvent.MessageRes(com.muyeon.app.R.string.home_rec_done))
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(blockTarget = null, blockBusy = false) }
                    _events.tryEmit(HomeEvent.MessageRes(com.muyeon.app.R.string.home_rec_failed))
                }
            }
        }
    }
}
