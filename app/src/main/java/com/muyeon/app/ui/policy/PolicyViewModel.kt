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
            when (val r = getPolicy(docKey, version)) {
                is ApiResult.Success -> _state.update { PolicyUiState(r.data, PolicyBlock.parse(r.data.body), isLoading = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, failed = true) }
            }
        }
    }
}
