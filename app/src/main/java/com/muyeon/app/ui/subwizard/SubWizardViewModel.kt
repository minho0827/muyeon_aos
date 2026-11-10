package com.muyeon.app.ui.subwizard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.derivedStateOf
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
import com.muyeon.domain.model.write.SubEditForm
import com.muyeon.domain.model.write.SubUpdate
import com.muyeon.domain.usecase.write.EditSubUseCase
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
    /** ★ 2026-11-08 묶음 3 — 수정 저장됨. */
    data class Updated(val id: Long) : SubWizardEvent
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
    /** 레슨 요청 [네, 강사 구해요]에서 넘어온 미리 채움 번호(?prefill=). */
    private val prefillId: String?,
    /** ★ 2026-11-08 묶음 3 — 수정 모드(웹 /subs/:id/edit): 기존 공고를 채우고 PATCH 로 저장. 등록이면 null. */
    private val editSub: EditSubUseCase? = null,
    private val editId: Long? = null,
) : ViewModel() {
    val isEdit = editId != null
    /** 칸 목록 — 수정이면 기간·타임 수는 선택(웹: 옛 공고의 급한 수정이 막히지 않게), 목록 밖 옛 값은 선택지에 얹는다. */
    var fields by mutableStateOf(SubWizardQuestions.fields)
        private set
    /** 수정: 기존 공고를 다 읽었다(등록은 처음부터 true). */
    var loaded by mutableStateOf(editId == null)
        private set
    var loadFailed by mutableStateOf(false)
        private set
    /** 수정: 이미 긴급 발송 중인 공고 — 급여 칸을 숨긴다(웹 dispatch.enabled). */
    var dispatchLocked by mutableStateOf(false)
        private set
    /** 수정: 확정된 공고 — 급여를 바꿀 수 없다(웹 payLocked). */
    var payLocked by mutableStateOf(false)
        private set
    val answers = mutableStateMapOf<String, SubAnswer>()
    var submitting by mutableStateOf(false)
        private set
    var uploading by mutableStateOf(false)
    var academy by mutableStateOf("")
    /** 제목 — 고치기 전까지는 고른 값으로 저절로 채운다. */
    var title by mutableStateOf("")
        private set
    private var titleEdited = false
    private val _events = MutableSharedFlow<SubWizardEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<SubWizardEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch { runCatching { info(ActiveType.visible(activeType.current())).name }.getOrNull()?.let { if (academy.isBlank()) academy = it } }
        prefillId?.takeIf { it.isNotBlank() }?.let { id -> viewModelScope.launch { (createSub.prefill(id) as? ApiResult.Success)?.data?.let(::applyPrefill) } }
        if (editId != null && editSub != null) viewModelScope.launch {
            when (val r = editSub.load(editId)) {
                is ApiResult.Success -> applyEdit(r.data)
                is ApiResult.Error -> loadFailed = true
            }
        }
    }

    /** ★ 2026-11-08 묶음 3 — 기존 공고로 칸을 채운다(웹 SubCreate 수정 모드 callGetSub). 제목은 그대로(자동 제목 안 함). */
    private fun applyEdit(d: SubEditForm) {
        answers.clear()
        fun put(id: String, v: String?) { if (!v.isNullOrBlank()) answers[id] = SubAnswer(listOf(v)) }
        put(SubWizardQuestions.GENRE, d.genre); put(SubWizardQuestions.TARGET, d.target)
        if (d.fields.isNotEmpty()) answers[SubWizardQuestions.FIELDS] = SubAnswer(d.fields)
        put(SubWizardQuestions.SCHEDULE, d.scheduleType); put(SubWizardQuestions.DATE, d.classDate)
        d.classTime?.split("~")?.map { it.trim() }?.takeIf { it.size == 2 && it.all { t -> runCatching { LocalTime.parse(t) }.isSuccess } }?.let { (a, b) ->
            put(SubWizardQuestions.START, a); put(SubWizardQuestions.END, b)
        }
        put(SubWizardQuestions.COUNT, d.classCount?.toString())
        d.region?.let { answers[SubWizardQuestions.REGION] = SubAnswer(listOf(it), regionCode = d.regionCode) }
        put(SubWizardQuestions.SALARY, d.salary); put(SubWizardQuestions.CAREER, d.career)
        d.description?.let { answers[SubWizardQuestions.DESC] = SubAnswer(text = it) }
        d.image?.let { answers[SubWizardQuestions.IMAGE] = SubAnswer(text = it) }
        title = d.title; titleEdited = true
        if (academy.isBlank()) d.academy?.let { academy = it }
        dispatchLocked = d.dispatchEnabled
        payLocked = d.payLocked
        fields = SubWizardQuestions.fields.map { f ->
            val cur = answers[f.id]?.values?.firstOrNull()
            val opts = if (f.type == SubFieldType.CHIPS && cur != null && cur !in f.options) f.options + cur else f.options
            when (f.id) {
                SubWizardQuestions.SCHEDULE, SubWizardQuestions.COUNT -> f.copy(options = opts, required = false)
                else -> f.copy(options = opts)
            }
        }.filterNot { dispatchLocked && it.id == SubWizardQuestions.SALARY }
        loaded = true
    }

    /** 레슨 요청 답으로 비어 있는 칸만 채운다(웹 useQuotePrefill). 수업 시간 'HH:MM ~ HH:MM' 은 시작·끝 그대로. */
    private fun applyPrefill(p: com.muyeon.domain.model.recruit.SubPrefill) {
        fun put(id: String, v: String?) { if (v != null && answers[id] == null) answers[id] = SubAnswer(listOf(v)) }
        put(SubWizardQuestions.GENRE, p.genre?.takeIf { it in com.muyeon.domain.model.teacher.LessonGenres.ALL })
        put(SubWizardQuestions.TARGET, p.target)
        if (p.fields.isNotEmpty() && answers[SubWizardQuestions.FIELDS] == null) answers[SubWizardQuestions.FIELDS] = SubAnswer(p.fields)
        put(SubWizardQuestions.DATE, p.classDate?.takeIf { runCatching { LocalDate.parse(it) }.isSuccess })
        p.classTime?.split("~")?.map { it.trim() }?.takeIf { it.size == 2 && it.all { t -> runCatching { LocalTime.parse(t) }.isSuccess } }?.let { (a, b) ->
            put(SubWizardQuestions.START, a); put(SubWizardQuestions.END, b)
        }
        p.region?.let { if (answers[SubWizardQuestions.REGION] == null) answers[SubWizardQuestions.REGION] = SubAnswer(listOf(it), regionCode = p.regionCode) }
        p.content?.let { if (answers[SubWizardQuestions.DESC] == null) answers[SubWizardQuestions.DESC] = SubAnswer(text = it) }
        p.title?.let { if (title.isBlank()) { title = it; titleEdited = true } }
        refreshTitle()
    }

    fun answer(id: String) = answers[id] ?: SubAnswer()
    private fun one(id: String) = answer(id).values.firstOrNull()

    /** 칩 고르기 — MULTI 는 켜고 끄기, 그 밖은 하나만(다시 누르면 비움 — 선택 항목만). */
    fun toggle(field: SubField, value: String) {
        if (payLocked && field.id == SubWizardQuestions.SALARY) return
        val a = answer(field.id)
        answers[field.id] = when {
            field.type == SubFieldType.MULTI -> SubAnswer(if (value in a.values) a.values - value else a.values + value)
            value in a.values && !field.required -> SubAnswer()
            else -> SubAnswer(listOf(value))
        }
        refreshTitle()
    }
    fun setDate(iso: String) { answers[SubWizardQuestions.DATE] = SubAnswer(listOf(iso)); refreshTitle() }
    fun setStart(hhmm: String) { answers[SubWizardQuestions.START] = SubAnswer(listOf(hhmm)); refreshTitle() }
    fun setEnd(hhmm: String) { answers[SubWizardQuestions.END] = SubAnswer(listOf(hhmm)) }
    fun setRegion(name: String, code: String?) { answers[SubWizardQuestions.REGION] = SubAnswer(listOf(name), regionCode = code) }
    fun setText(t: String) { answers[SubWizardQuestions.DESC] = SubAnswer(text = t) }
    fun setImage(url: String?) { answers[SubWizardQuestions.IMAGE] = SubAnswer(text = url) }
    fun editTitle(t: String) { title = t; titleEdited = true }

    val start: String? get() = one(SubWizardQuestions.START)
    val end: String? get() = one(SubWizardQuestions.END)

    /** "HH:mm~HH:mm" — 끝이 시작보다 늦을 때만. */
    fun classTime(): String? {
        val s = start ?: return null; val e = end ?: return null
        return if (LocalTime.parse(e).isAfter(LocalTime.parse(s))) "$s~$e" else null
    }
    /** 끝이 시작보다 늦지 않다. */
    fun timeOrderError(): Boolean = start != null && end != null && classTime() == null

    /** 시작이 지금보다 이전(KST)인가. 날짜·시작이 없으면 null. */
    fun timeError(): Boolean? {
        val date = one(SubWizardQuestions.DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val s = start?.let { LocalTime.parse(it) } ?: return null
        return LocalDateTime.of(date, s).isBefore(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
    }

    /** 비어 있는 필수 칸(첫 번째) — 없으면 null. */
    fun firstMissing(): SubField? = fields.firstOrNull { f ->
        f.required && when (f.type) {
            SubFieldType.TIME -> classTime() == null
            SubFieldType.TEXT, SubFieldType.IMAGE -> false
            else -> answer(f.id).isEmpty
        }
    }

    val canSubmit: Boolean get() = loaded && !submitting && !uploading && firstMissing() == null && timeError() != true && title.isNotBlank()

    var targetLabel: (String) -> String = { it }
    var titleTemplate: String = "%1\$s%2\$s"
    private fun refreshTitle() { if (!titleEdited) title = autoTitle() }
    private fun autoTitle(): String {
        val head = listOfNotNull(one(SubWizardQuestions.GENRE), one(SubWizardQuestions.TARGET)?.let(targetLabel)).joinToString(" ")
        if (head.isEmpty()) return ""
        val date = one(SubWizardQuestions.DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val whenText = listOfNotNull(date?.let { "${it.monthValue}/${it.dayOfMonth}" }, start).joinToString(" ")
        return String.format(titleTemplate, head, if (whenText.isNotEmpty()) " · $whenText" else "")
    }

    fun submit() {
        if (!canSubmit) return
        submitting = true
        if (editId != null && editSub != null) { submitEdit(editId, editSub); return }
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
            career = one(SubWizardQuestions.CAREER),
            image = answer(SubWizardQuestions.IMAGE).text?.ifBlank { null },
        )
        viewModelScope.launch {
            when (val r = createSub(draft)) {
                is ApiResult.Success -> _events.tryEmit(SubWizardEvent.Created(r.data))
                is ApiResult.Error -> { submitting = false; _events.tryEmit(SubWizardEvent.Failed(r.message)) }
            }
        }
    }

    /** ★ 2026-11-08 묶음 3 — 수정 저장(웹 buildPayload isEdit — 빈 칸은 보내지 않고, 급여만 비우면 null 로 지운다. 긴급 공고는 급여를 보내지 않는다). */
    private fun submitEdit(id: Long, edit: EditSubUseCase) {
        val u = SubUpdate(
            title = title.trim(),
            academy = academy.trim().ifBlank { null },
            genre = one(SubWizardQuestions.GENRE).orEmpty(),
            target = one(SubWizardQuestions.TARGET).orEmpty(),
            fields = answer(SubWizardQuestions.FIELDS).values,
            scheduleType = one(SubWizardQuestions.SCHEDULE),
            // 날짜 없는 글은 classDate 를 빼고 보낸다(빈 글자 "" 를 보내지 않는다).
            classDate = one(SubWizardQuestions.DATE)?.ifBlank { null },
            classTime = classTime(),
            classCount = one(SubWizardQuestions.COUNT)?.toIntOrNull()?.takeIf { it > 0 },
            region = one(SubWizardQuestions.REGION),
            regionCode = answer(SubWizardQuestions.REGION).regionCode,
            salary = one(SubWizardQuestions.SALARY),
            sendSalary = !dispatchLocked,
            career = one(SubWizardQuestions.CAREER),
            description = answer(SubWizardQuestions.DESC).text?.ifBlank { null },
            image = answer(SubWizardQuestions.IMAGE).text?.ifBlank { null },
        )
        viewModelScope.launch {
            when (val r = edit.save(id, u)) {
                is ApiResult.Success -> _events.tryEmit(SubWizardEvent.Updated(id))
                is ApiResult.Error -> { submitting = false; _events.tryEmit(SubWizardEvent.Failed(r.message)) }
            }
        }
    }

    companion object {
        /** Koin 이름 — 수정 모드 ViewModel(AppModule). */
        const val EDIT = "subEdit"
    }
}
