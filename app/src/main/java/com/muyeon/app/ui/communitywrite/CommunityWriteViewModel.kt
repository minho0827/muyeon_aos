package com.muyeon.app.ui.communitywrite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.R
import com.muyeon.app.navigation.AppResults
import com.muyeon.app.ui.community.CommunityListViewModel
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.ErrorKind
import com.muyeon.domain.model.write.CommunityDraft
import com.muyeon.domain.usecase.community.GetCommunityPostUseCase
import com.muyeon.domain.usecase.write.WriteCommunityPostUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-08 묶음 3 — 커뮤니티 글쓰기/수정 상태(웹 CommunityWrite). [editId] 가 있으면 수정(기존 글을 채운다).
 *   빠진 칸 검사는 화면(RequiredForm)이 먼저 한다 — 여기서는 저장만.
 */
data class CommunityWriteUiState(
    val category: String = "",
    val title: String = "",
    val body: String = "",
    val images: List<String> = emptyList(),
    /** 수정 모드에서 기존 글을 읽는 중. */
    val loading: Boolean = false,
    val uploading: Boolean = false,
    val submitting: Boolean = false,
    val done: Boolean = false,
    /** 화면 아래 빨간 안내(서버 문구 그대로) — 없으면 [errorRes]. */
    val error: String? = null,
    val errorRes: Int? = null,
)

class CommunityWriteViewModel(
    private val editId: Long?,
    private val getPost: GetCommunityPostUseCase,
    private val write: WriteCommunityPostUseCase,
) : ViewModel() {
    val isEdit = editId != null
    private val _state = MutableStateFlow(CommunityWriteUiState(loading = editId != null))
    val state: StateFlow<CommunityWriteUiState> = _state.asStateFlow()

    init {
        editId?.let { id ->
            viewModelScope.launch {
                when (val r = getPost(id)) {
                    is ApiResult.Success -> _state.update {
                        it.copy(category = r.data.summary.category.orEmpty(), title = r.data.summary.title, body = r.data.body,
                            images = r.data.images, loading = false)
                    }
                    is ApiResult.Error -> _state.update { it.copy(loading = false, errorRes = R.string.b3_cwrite_load_failed) }
                }
            }
        }
    }

    fun onCategory(v: String) = _state.update { it.copy(category = v) }
    fun onTitle(v: String) = _state.update { it.copy(title = v.take(WriteCommunityPostUseCase.TITLE_MAX)) }
    fun onBody(v: String) = _state.update { it.copy(body = v.take(WriteCommunityPostUseCase.BODY_MAX)) }
    fun onUploading(v: Boolean) = _state.update { it.copy(uploading = v) }
    fun addImage(url: String) = _state.update {
        if (it.images.size >= WriteCommunityPostUseCase.IMAGES_MAX) it else it.copy(images = it.images + url)
    }
    fun removeImage(index: Int) = _state.update { it.copy(images = it.images.filterIndexed { i, _ -> i != index }) }
    fun showUploadError(text: String?) = _state.update { it.copy(error = text, errorRes = if (text == null) R.string.b3_cwrite_upload_failed else null) }

    /** 화면이 빠진 칸이 없음을 확인한 뒤 부른다. */
    fun submit() {
        val s = _state.value
        if (s.uploading || s.submitting || s.loading) return
        _state.update { it.copy(submitting = true, error = null, errorRes = null) }
        viewModelScope.launch {
            when (val r = write.save(editId, CommunityDraft(s.category, s.title, s.body, s.images))) {
                is ApiResult.Success -> {
                    AppResults.publish(setOf(CommunityListViewModel.RESULT_COMMUNITY))
                    _state.update { it.copy(submitting = false, done = true) }
                }
                is ApiResult.Error -> _state.update {
                    it.copy(submitting = false,
                        error = r.message.takeIf { r.kind == ErrorKind.REJECTED },
                        errorRes = if (r.kind == ErrorKind.REJECTED) null else if (isEdit) R.string.b3_cwrite_failed_edit else R.string.b3_cwrite_failed_new)
                }
            }
        }
    }
}
