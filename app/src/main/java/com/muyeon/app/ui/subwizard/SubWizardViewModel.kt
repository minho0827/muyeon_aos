package com.muyeon.app.ui.subwizard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.recruit.SubDraft
import com.muyeon.domain.usecase.my.GetMyPageInfoUseCase
import com.muyeon.domain.usecase.recruit.CreateSubUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** 설문 답 — 고른 값들 · 글자 · 지역(이름·코드). */
data class SubAnswer(val values: List<String> = emptyList(), val text: String? = null, val regionCode: String? = null) {
    val isEmpty get() = values.isEmpty() && text.isNullOrBlank()
}

sealed interface SubWizardEvent {
    data class Created(val id: Long) : SubWizardEvent
    data class Failed(val message: String) : SubWizardEvent
}

/**
 * 대타공고 설문 상태 — 레슨 견적 위저드(QuoteWizardViewModel)와 같은 규칙: 한 질문씩 · 답한 질문은 위에 말풍선 · '수정'으로 돌아갔다가
 *   다음을 누르면 원래 자리로. 마지막 확인 단계에서 제목·학원명을 고치고 등록.
 */
class SubWizardViewModel(
    private val createSub: CreateSubUseCase,
    private val info: GetMyPageInfoUseCase,
    private val activeType: ActiveTypeStore,
) : ViewModel() {
    val steps = SubWizardQuestions.steps
    val answers = mutableStateMapOf<String, SubAnswer>()
    var currentIndex by mutableStateOf(0)
        private set
    var submitting by mutableStateOf(false)
        private set
    /** 확인 단계 — 제목(자동, 고칠 수 있음) · 학원/단체 이름(계정 이름으로 채움). */
    var title by mutableStateOf("")
    var academy by mutableStateOf("")
    private var editReturn: Int? = null
    private val _events = MutableSharedFlow<SubWizardEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<SubWizardEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch { runCatching { info(ActiveType.visible(activeType.current())).name }.getOrNull()?.let { if (academy.isBlank()) academy = it } }
    }

    val current get() = steps[currentIndex]
    val answered get() = steps.take(currentIndex)
    val isLast get() = currentIndex == steps.lastIndex
    val progress get() = currentIndex.toFloat() / (steps.size - 1)
    fun answer(id: String) = answers[id] ?: SubAnswer()

    val canProceed: Boolean get() = when (current.type) {
        SubStepType.CONFIRM -> title.isNotBlank() && !submitting && timeError() == null
        else -> !current.required || !answer(current.id).isEmpty
    }

    fun toggle(value: String) {
        val s = current; val a = answer(s.id)
        answers[s.id] = when (s.type) {
            SubStepType.MULTI -> a.copy(values = if (value in a.values) a.values - value else a.values + value)
            else -> a.copy(values = listOf(value))
        }
    }

    fun setDate(iso: String) { answers[SubWizardQuestions.DATE] = SubAnswer(listOf(iso)) }
    fun setRegion(name: String, code: String?) { answers[SubWizardQuestions.REGION] = SubAnswer(listOf(name), regionCode = code) }
    fun setText(t: String) { answers[current.id] = answer(current.id).copy(text = t) }

    fun next() {
        val back = editReturn
        when {
            back != null -> { editReturn = null; currentIndex = back; if (isLast && title.isBlank()) title = autoTitle() }
            isLast -> submit()
            else -> { currentIndex++; if (isLast && title.isBlank()) title = autoTitle() }
        }
    }

    fun edit(id: String) {
        val i = steps.indexOfFirst { it.id == id }
        if (i < 0) return
        if (editReturn == null) editReturn = currentIndex
        currentIndex = i
    }

    fun back(): Boolean { if (currentIndex == 0) return false; currentIndex--; return true }

    private fun one(id: String) = answer(id).values.firstOrNull()

    /** 수업 시간 'HH:mm~HH:mm'. */
    fun classTime(): String? {
        val start = one(SubWizardQuestions.START) ?: return null
        val len = one(SubWizardQuestions.LENGTH)?.toLongOrNull() ?: return null
        val t = LocalTime.parse(start)
        return "$start~${t.plusMinutes(len)}"
    }

    /** 오늘 지난 시각이면 문구 키(서버 assertFutureClassSchedule 와 같은 판단 — 미리 알려 준다). */
    fun timeError(): Boolean? {
        val date = one(SubWizardQuestions.DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val start = one(SubWizardQuestions.START)?.let { LocalTime.parse(it) } ?: return null
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        return if (LocalDateTime.of(date, start).isBefore(now)) true else null
    }

    /** 자동 제목 — "{장르} {대상} 대타 구해요 · 11/20 19:00"(사용자가 고칠 수 있다). 대상 이름·문구 틀은 화면이 리소스로 넘긴다. */
    var targetLabel: (String) -> String = { it }
    var titleTemplate: String = "%1\$s%2\$s"
    private fun autoTitle(): String {
        val date = one(SubWizardQuestions.DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val whenText = listOfNotNull(date?.let { "${it.monthValue}/${it.dayOfMonth}" }, one(SubWizardQuestions.START)).joinToString(" ")
        val head = listOfNotNull(one(SubWizardQuestions.GENRE), one(SubWizardQuestions.TARGET)?.let(targetLabel)).joinToString(" ")
        return String.format(titleTemplate, head, if (whenText.isNotEmpty()) " · $whenText" else "")
    }

    private fun submit() {
        if (submitting) return
        submitting = true
        val draft = SubDraft(
            title = title.trim(),
            academy = academy.trim().ifBlank { null },
            genre = one(SubWizardQuestions.GENRE).orEmpty(),
            target = one(SubWizardQuestions.TARGET).orEmpty(),
            fields = answer(SubWizardQuestions.FIELDS).values,
            scheduleType = one(SubWizardQuestions.SCHEDULE).orEmpty(),
            classDate = one(SubWizardQuestions.DATE).orEmpty(),
            classTime = classTime().orEmpty(),
            classCount = one(SubWizardQuestions.COUNT)?.toIntOrNull() ?: 1,
            region = one(SubWizardQuestions.REGION),
            regionCode = answer(SubWizardQuestions.REGION).regionCode,
            salary = one(SubWizardQuestions.SALARY),
            pay = null,
            description = answer(SubWizardQuestions.DESC).text?.trim()?.ifBlank { null },
        )
        viewModelScope.launch {
            when (val r = createSub(draft)) {
                is ApiResult.Success -> _events.tryEmit(SubWizardEvent.Created(r.data))
                is ApiResult.Error -> { submitting = false; _events.tryEmit(SubWizardEvent.Failed(r.message)) }
            }
        }
    }
}
