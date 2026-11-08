package com.muyeon.app.ui.likes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.model.scrap.LikedItem
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.usecase.scrap.GetLikedItemsUseCase
import com.muyeon.domain.usecase.scrap.ToggleScrapResult
import com.muyeon.domain.usecase.scrap.ToggleScrapUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LikesUiState(
    /** [LikesViewModel.ALL] 또는 ScrapTarget.serverType. */
    val tab: String = LikesViewModel.ALL,
    val grid: Boolean = true,
    val items: List<LikedItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface LikesEvent {
    data class Message(val text: String) : LikesEvent
    data object Removed : LikesEvent
}

/**
 * 찜/관심 — 웹 pages/muyeon/my/Likes.js. 종류 칩(전체·레슨·강사·공연·커뮤니티) · 그리드/목록 · 하트 누르면 해제(목록에서 빠짐).
 *   칩·보기 방식은 기억한다(웹 likes.tab · likes.view). 상세에서 찜을 바꾸고 돌아오면 [reload].
 */
class LikesViewModel(
    private val getItems: GetLikedItemsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    private val prefs: KeyValueStore,
) : ViewModel() {
    val tabs: List<String> = listOf(ALL, ScrapTarget.LESSON.serverType, ScrapTarget.TEACHER.serverType, ScrapTarget.PERFORMANCE.serverType, ScrapTarget.POST.serverType)

    private val _state = MutableStateFlow(
        LikesUiState(tab = prefs.getString(K_TAB)?.takeIf { it in tabs } ?: ALL, grid = prefs.getString(K_VIEW) != "basic"),
    )
    val state: StateFlow<LikesUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<LikesEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<LikesEvent> = _events.asSharedFlow()
    private var job: Job? = null

    init { reload() }

    fun selectTab(tab: String) {
        if (tab == _state.value.tab) return
        prefs.putString(K_TAB, tab)
        _state.update { it.copy(tab = tab, items = emptyList(), isLoading = true) }
        reload()
    }

    fun setGrid(grid: Boolean) {
        prefs.putString(K_VIEW, if (grid) "half" else "basic")
        _state.update { it.copy(grid = grid) }
    }

    /** 탭 전환·복귀 — 복귀 때는 목록을 비우지 않고 응답으로 바꾼다(웹 silentRef). */
    fun reload(pull: Boolean = false) {
        job?.cancel()
        _state.update { it.copy(isRefreshing = pull, errorMessage = null) }
        val tab = _state.value.tab
        job = viewModelScope.launch {
            when (val r = getItems(ScrapTarget.entries.firstOrNull { it.serverType == tab })) {
                is ApiResult.Success -> _state.update { it.copy(items = r.data, isLoading = false, isRefreshing = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = r.message) }
            }
        }
    }

    /** 하트 — 찜 해제. 먼저 목록에서 빼고(웹과 같다), 실패하면 되돌린다. */
    fun unlike(item: LikedItem) {
        val target = item.target ?: return
        val before = _state.value.items
        _state.update { s -> s.copy(items = s.items.filterNot { it.targetType == item.targetType && it.targetId == item.targetId }) }
        viewModelScope.launch {
            when (val r = toggleScrap(target, item.targetId, currentlyLiked = true)) {
                is ToggleScrapResult.Done -> _events.tryEmit(LikesEvent.Removed)
                is ToggleScrapResult.Failed -> { _state.update { it.copy(items = before) }; _events.tryEmit(LikesEvent.Message(r.message)) }
                ToggleScrapResult.LoginRequired -> _state.update { it.copy(items = before) }
            }
        }
    }

    companion object {
        const val ALL = "ALL"
        private const val K_TAB = "likes.tab"
        private const val K_VIEW = "likes.view"
    }
}
