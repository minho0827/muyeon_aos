package com.muyeon.app.ui.castingmaterial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.castingmaterial.CastingMaterialBatch
import com.muyeon.domain.model.castingmaterial.CastingMaterialKind
import com.muyeon.domain.usecase.castingmaterial.CastingMaterialUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CastingMaterialsUiState(
    /** null = 확인 중, false = 운영 계정 아님(웹 EmptyView). */
    val allowed: Boolean? = null,
    val images: List<String> = emptyList(),
    val instagram: String = "",
    val link: String = "",
    val uploading: Boolean = false,
    val saving: Boolean = false,
    /** 빨간 안내(웹 msg) — 업로드·등록 실패. */
    val message: String? = null,
    val items: List<CastingMaterialBatch> = emptyList(),
    /** 지우는 중인 묶음 id. */
    val deleting: Long? = null,
)

sealed interface CastingMaterialsEvent {
    data object Submitted : CastingMaterialsEvent
    /** ★ 11-10 지웠다 — [kept] 개의 공고·공연은 남아 있다. */
    data class Deleted(val kept: Int) : CastingMaterialsEvent
    data class DeleteFailed(val message: String) : CastingMaterialsEvent
}

/**
 * ★ 2026-11-08 묶음 2 — 캐스팅·공연 자료 업로드(웹 pages/muyeon/my/CastingMaterials.js, kind=CASTING|PERFORMANCE).
 *   권한 확인(GET /casting-materials/access) → 목록 · 캡처 사진(최대 20)·인스타·그 밖의 링크 → [AI로 … 등록하기](POST) → 토스트 + 비우기 + 목록.
 *   처리 중인 묶음이 있으면 10초마다 목록을 다시 본다(웹 setInterval 10000).
 */
class CastingMaterialsViewModel(val kind: CastingMaterialKind, private val useCase: CastingMaterialUseCase) : ViewModel() {
    private val _state = MutableStateFlow(CastingMaterialsUiState())
    val state: StateFlow<CastingMaterialsUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<CastingMaterialsEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<CastingMaterialsEvent> = _events.asSharedFlow()
    private var poll: Job? = null

    init {
        viewModelScope.launch {
            val ok = (useCase.access() as? ApiResult.Success)?.data == true
            _state.update { it.copy(allowed = ok) }
            if (ok) load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            (useCase.list(kind) as? ApiResult.Success)?.data?.let { items -> _state.update { it.copy(items = items) } }
            schedulePoll()
        }
    }

    private fun schedulePoll() {
        val busy = _state.value.items.any { it.processing }
        if (!busy) { poll?.cancel(); poll = null; return }
        if (poll?.isActive == true) return
        poll = viewModelScope.launch {
            while (isActive) {
                delay(POLL_MS)
                (useCase.list(kind) as? ApiResult.Success)?.data?.let { items -> _state.update { it.copy(items = items) } }
                if (_state.value.items.none { it.processing }) break
            }
        }
    }

    fun onInstagram(v: String) = _state.update { it.copy(instagram = v.take(MAX_LINK)) }
    fun onLink(v: String) = _state.update { it.copy(link = v.take(MAX_LINK)) }
    fun onUploading(v: Boolean) = _state.update { it.copy(uploading = v) }
    fun addImages(urls: List<String>) = _state.update { it.copy(images = (it.images + urls).take(CastingMaterialUseCase.MAX_IMAGES)) }
    fun removeImage(i: Int) = _state.update { it.copy(images = it.images.filterIndexed { idx, _ -> idx != i }) }
    fun showMessage(m: String?) = _state.update { it.copy(message = m) }

    /** ★ 11-10 묶음 지우기 — 성공이면 목록에서 뺀다. 실패(처리 중 409 등)는 서버 문구 그대로. */
    fun delete(id: Long) {
        if (_state.value.deleting != null) return
        _state.update { it.copy(deleting = id) }
        viewModelScope.launch {
            when (val r = useCase.delete(id)) {
                is ApiResult.Success -> {
                    _state.update { s -> s.copy(deleting = null, items = s.items.filterNot { it.id == id }) }
                    _events.tryEmit(CastingMaterialsEvent.Deleted(r.data.kept.size))
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(deleting = null) }
                    _events.tryEmit(CastingMaterialsEvent.DeleteFailed(r.message))
                }
            }
        }
    }

    fun submit(failText: String) {
        val s = _state.value
        if (s.saving || s.uploading) return
        _state.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            when (val r = useCase.submit(kind, s.images, s.instagram, s.link)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false, images = emptyList(), instagram = "", link = "") }
                    _events.tryEmit(CastingMaterialsEvent.Submitted)
                    load()
                }
                // 웹: 서버 문구가 있으면 그대로, 없으면 '올리지 못했어요…'.
                is ApiResult.Error -> _state.update { it.copy(saving = false, message = r.message.takeIf { _ -> r.statusCode in 400..499 || s.images.isEmpty() } ?: failText) }
            }
        }
    }

    companion object {
        private const val POLL_MS = 10_000L
        private const val MAX_LINK = 500
    }
}
