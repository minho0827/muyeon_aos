package com.muyeon.app.ui.policy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.policy.PolicyBlock
import com.muyeon.domain.model.policy.PolicyView
import com.muyeon.domain.usecase.policy.GetPolicyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class PolicyUiState(
    val view: PolicyView? = null,
    val blocks: List<PolicyBlock> = emptyList(),
    val isLoading: Boolean = true,
    val failed: Boolean = false,
)

/** 약관 문서 — 웹 pages/setting/Policy.js(?doc= · ?version=). 인자: docKey, version(없으면 현재). */
class PolicyViewModel(private val docKey: String, private val version: String?, private val getPolicy: GetPolicyUseCase) : ViewModel() {
    private val _state = MutableStateFlow(PolicyUiState())
    val state: StateFlow<PolicyUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, failed = false) }
        viewModelScope.launch {
            // ★ 2026-11-18 실패·빈 본문·응답 지연(15초)은 모두 '불러오지 못했어요 + 다시 시도' — 빈 화면으로 두지 않는다.
            val r = withTimeoutOrNull(15_000) { getPolicy(docKey, version) }
            val blocks = (r as? ApiResult.Success)?.data?.body?.let(PolicyBlock::parse).orEmpty()
            if (r is ApiResult.Success && blocks.isNotEmpty()) _state.update { PolicyUiState(r.data, blocks, isLoading = false) }
            else _state.update { it.copy(isLoading = false, failed = true) }
        }
    }
}
