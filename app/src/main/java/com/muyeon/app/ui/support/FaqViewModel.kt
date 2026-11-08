package com.muyeon.app.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.model.support.Faq
import com.muyeon.domain.usecase.support.GetFaqsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FaqUiState(
    val categories: List<String> = emptyList(),
    /** [FaqViewModel.ALL] 또는 분류. */
    val tab: String = FaqViewModel.ALL,
    val items: List<Faq> = emptyList(),
    /** 펼친 질문(웹 details name= — 한 번에 하나). */
    val openId: Long? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val shown: List<Faq> get() = if (tab == FaqViewModel.ALL) items else items.filter { it.category == tab }
}

/** FAQ — 웹 pages/faq/faq.js. 분류 탭(전체 + 서버 분류) · 질문 누르면 답 펼침. 고른 탭은 기억한다(웹 usePersistedState faq.tab). */
class FaqViewModel(private val getFaqs: GetFaqsUseCase, private val prefs: KeyValueStore) : ViewModel() {
    private val _state = MutableStateFlow(FaqUiState(tab = prefs.getString(K_TAB) ?: ALL))
    val state: StateFlow<FaqUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = getFaqs()) {
                is ApiResult.Success -> _state.update { s ->
                    val (cats, faqs) = r.data
                    s.copy(categories = cats, items = faqs, isLoading = false, tab = s.tab.takeIf { it == ALL || it in cats } ?: ALL)
                }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, errorMessage = r.message) }
            }
        }
    }

    fun selectTab(tab: String) {
        prefs.putString(K_TAB, tab)
        _state.update { it.copy(tab = tab, openId = null) }
    }

    fun toggle(id: Long) = _state.update { it.copy(openId = if (it.openId == id) null else id) }

    companion object {
        const val ALL = "ALL"
        private const val K_TAB = "faq.tab"
    }
}
