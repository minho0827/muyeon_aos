package com.muyeon.app.ui.academyedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.AppResults
import com.muyeon.app.result.ResultKeys
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.academy.AcademyLinkedLesson
import com.muyeon.domain.model.academy.AcademyProfileForm
import com.muyeon.domain.usecase.academy.AcademyProfileUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AcademyEditUiState(
    val loaded: Boolean = false,
    val form: AcademyProfileForm = AcademyProfileForm(),
    /** 키워드 입력 글(쉼표로 구분) — 저장 때 최대 8개로 자른다. */
    val keywordText: String = "",
    /** 대표원장 — /auth/me 의 name(본인인증 실명, 읽기 전용). */
    val ownerName: String = "",
    /** 미리보기 '수업 안내' — 내 학원 공개 프로필의 수업. */
    val lessons: List<AcademyLinkedLesson> = emptyList(),
    val uploading: Boolean = false,
    val saving: Boolean = false,
    val previewOpen: Boolean = false,
    /** 결과 팝업(웹 CommonModal) — [saved] 면 확인 뒤 MY 로. */
    val resultMessage: String? = null,
    val saved: Boolean = false,
)

sealed interface AcademyEditEvent {
    /** 저장 성공 팝업을 닫았다 — 웹처럼 /mypage 로. */
    data object GoMy : AcademyEditEvent
    /** 저장 성공 — 화면이 AI 초안 채택을 기록한다(웹 markAiSaved). */
    data class Saved(val intro: String) : AcademyEditEvent
}

/**
 * ★ 2026-11-08 묶음 2 — 학원 정보 관리(웹 pages/muyeon/my/AcademyProfile.js).
 *   불러오기(GET /auth/me/academy · 실패해도 빈 양식) · 대표원장(GET /auth/me) · 미리보기 수업(GET /academies/:내 번호) ·
 *   저장(PATCH /auth/me/academy) → 결과 팝업 → 성공이면 MY. 필수 칸 확인은 화면의 RequiredForm.
 */
class AcademyEditViewModel(private val useCase: AcademyProfileUseCase) : ViewModel() {
    private val _state = MutableStateFlow(AcademyEditUiState())
    val state: StateFlow<AcademyEditUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<AcademyEditEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<AcademyEditEvent> = _events.asSharedFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            // 웹: 불러오기 실패해도 빈 양식으로 연다(catch → setLoaded(true)).
            val f = (useCase.load() as? ApiResult.Success)?.data ?: AcademyProfileForm()
            _state.update { it.copy(loaded = true, form = f, keywordText = f.academyKeywords.joinToString(", ")) }
        }
        viewModelScope.launch {
            val owner = (useCase.owner() as? ApiResult.Success)?.data ?: return@launch
            _state.update { it.copy(ownerName = owner.name) }
            val id = owner.id ?: return@launch
            (useCase.lessons(id) as? ApiResult.Success)?.data?.let { l -> _state.update { it.copy(lessons = l) } }
        }
    }

    fun edit(block: AcademyProfileForm.() -> AcademyProfileForm) = _state.update { it.copy(form = it.form.block()) }
    fun onKeywords(v: String) = _state.update { it.copy(keywordText = v) }

    /** 체크 묶음 하나 켜고 끄기(웹 toggle). */
    fun toggle(list: List<String>, value: String) = if (value in list) list - value else list + value

    fun onUploading(v: Boolean) = _state.update { it.copy(uploading = v) }
    fun addImages(urls: List<String>) = edit { copy(academyImages = (academyImages + urls).take(AcademyProfileUseCase.MAX_IMAGES)) }
    fun removeImage(i: Int) = edit { copy(academyImages = academyImages.filterIndexed { idx, _ -> idx != i }) }
    fun showMessage(m: String) = _state.update { it.copy(resultMessage = m, saved = false) }

    /** AI 도우미 초안(?aiDraft=) — 학원 소개만 바꾼다(편의시설 문장은 소개 끝에, 1500자). 웹 useAiDraft 적용부 그대로. */
    fun applyAiDraft(intro: String?, facilitiesText: String?) {
        val text = listOfNotNull(intro, facilitiesText).filter { it.isNotEmpty() }.joinToString("\n\n").take(AcademyProfileUseCase.MAX_INTRO)
        if (text.isNotEmpty()) edit { copy(academyIntro = text) }
    }

    fun setPreview(open: Boolean) = _state.update { it.copy(previewOpen = open) }

    fun save(savedText: String, failText: String) {
        val s = _state.value
        if (s.saving || s.uploading) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val r = useCase.save(s.form, s.keywordText)) {
                is ApiResult.Success -> {
                    AppResults.publish(setOf(ResultKeys.ACADEMY))
                    _events.tryEmit(AcademyEditEvent.Saved(s.form.academyIntro))
                    _state.update { it.copy(saving = false, resultMessage = savedText, saved = true) }
                }
                // 웹: 서버 문구가 있으면 그대로, 없으면 '저장에 실패했어요…'.
                is ApiResult.Error -> _state.update {
                    it.copy(saving = false, saved = false, resultMessage = r.message.takeIf { _ -> r.statusCode in 400..499 } ?: failText)
                }
            }
        }
    }

    fun dismissResult() {
        val saved = _state.value.saved
        _state.update { it.copy(resultMessage = null, saved = false) }
        if (saved) _events.tryEmit(AcademyEditEvent.GoMy)
    }
}
