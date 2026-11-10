package com.muyeon.app.ui.quote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * 견적 문진 상태 — iOS `QuoteWizardViewModel.swift` 1:1 이식.
 *  progress/canProceed/answeredQuestions 계산식과 toggle 규칙(single=치환, multi=토글)을 동일하게 유지.
 *
 *  ★ 2026-11-10 질문은 서버 설문(QuestionnaireRepository)에서 받는다.
 *   - 0단계(신청자 유형)는 첫 질문으로 그리고, 답은 answers 가 아니라 맨 위 requesterType 으로 보낸다.
 *   - 0단계 답을 바꾸면 그 유형의 세트를 다시 받는다 — 같은 id 의 답은 남기고 나머지는 버린다.
 *   - visibleWhen(앞 답에 따라 묻는 질문 — whenDate·inquiryText)은 [questions] 계산에서 걸러진다.
 *   - 둘 다 못 받으면 하드코딩 설문(QuoteQuestions.forCategory)으로 돌고 0단계를 건너뛴다.
 */
class QuoteWizardViewModel(
    val category: QuoteCategory,
    private val seedAnswers: Map<String, QuoteAnswer> = emptyMap(),
    // ★ 2026-11-07 지정(1:1) 요청 — 질문 목록에서 teacherGender 를 뺀다(진행률·단계도 이 목록 기준이라 같이 맞는다).
    private val isDirect: Boolean = false,
    /** 서버 설문. null 이면 처음부터 하드코딩 설문(미리보기·테스트용). */
    private val repo: QuestionnaireRepository? = null,
) : ViewModel() {

    val answers = mutableStateMapOf<String, QuoteAnswer>()

    /** 0단계 질문(없으면 0단계 건너뜀). */
    var stepZero by mutableStateOf<QuoteQuestion?>(null)
        private set
    /** 지금 세트의 질문(visibleWhen 거르기 전). */
    private val setQuestions = mutableStateListOf<QuoteQuestion>()
    /** 받은 세트 — null 이면 하드코딩 폴백(requesterType·questionSetId 를 보내지 않는다). */
    var questionSet by mutableStateOf<QuestionnaireSet?>(null)
        private set
    /** 설문을 받는 중 — 화면은 '입력 중…' 말풍선을 띄우고 [다음]을 막는다. */
    var loading by mutableStateOf(repo != null)
        private set

    var currentIndex by mutableStateOf(0)
        private set
    /** '수정'으로 돌아갔을 때 원래 있던 질문 id — 질문 목록이 바뀌어도 id 로 찾아 돌아간다. */
    private var editReturnId: String? = null
    private var seedsApplied = false

    init {
        if (repo == null) applyFallback() else viewModelScope.launch { loadEntry() }
    }

    // MARK: - 설문 받기

    private suspend fun loadEntry() {
        loading = true
        val entry = repo?.entry()
        if (entry == null) { applyFallback(); loading = false; return }
        val q = entry.question
        if (entry.skip || q == null) {
            stepZero = null
            val set = repo?.set(REQUESTER_TYPE_GENERAL, category.id, isDirect)
            if (set == null) applyFallback() else applySet(set)
        } else {
            stepZero = q
            // 미리 '선택된 상태'로만 — 자동 진행 안 함.
            entry.preselect?.let { answers[REQUESTER_TYPE_QID] = QuoteAnswer(REQUESTER_TYPE_QID, selectedOptionIds = listOf(it)) }
            // 0단계에서 진행률 칸 수가 1/1 로 보이지 않게 — 고르기 전엔 하드코딩 목록을 자리만 채워 둔다(그려지지 않는다).
            setQuestions.clear(); setQuestions.addAll(QuoteQuestions.forCategory(category, isDirect))
        }
        currentIndex = 0
        loading = false
    }

    private fun applyFallback() {
        stepZero = null
        questionSet = null
        setQuestions.clear(); setQuestions.addAll(QuoteQuestions.forCategory(category, isDirect))
        applySeedsOnce()
    }

    /** 세트 적용 — 새 세트에 없는 답은 버리고, 비어 있는 질문은 서버 prefill 로 미리 채운다. */
    private fun applySet(set: QuestionnaireSet) {
        questionSet = set
        setQuestions.clear(); setQuestions.addAll(set.questions)
        val keep = set.questions.map { it.id }.toSet() + REQUESTER_TYPE_QID
        answers.keys.filter { it !in keep }.forEach { answers.remove(it) }
        set.prefill.forEach { (qid, a) ->
            if (qid in keep && answer(qid).isEmpty) answers[qid] = a
        }
        applySeedsOnce()
    }

    /** '같은 조건으로 견적 요청' 프리필 — 처음 한 번, 이 세트에 있는 질문에만(single 은 첫 값만). */
    private fun applySeedsOnce() {
        if (seedsApplied || seedAnswers.isEmpty()) { seedsApplied = true; return }
        seedsApplied = true
        for ((qid, a) in seedAnswers) {
            val q = setQuestions.firstOrNull { it.id == qid } ?: continue
            if (!answer(qid).isEmpty) continue
            answers[qid] = if (q.type == QuoteAnswerType.SINGLE) a.copy(selectedOptionIds = a.selectedOptionIds.take(1)) else a
        }
        if (stepZero == null) {
            // 프리필된 질문 뒤(첫 미답변)로 이동 — iOS convenience init 동일.
            while (currentIndex < questions.size - 1 && seedAnswers.containsKey(questions[currentIndex].id)) currentIndex += 1
        }
    }

    /** 0단계 답으로 세트를 받는다. 실패하면 하드코딩 설문(0단계 답은 보내지 않는다). */
    private fun loadSetFor(type: String) {
        val r = repo ?: return
        loading = true
        viewModelScope.launch {
            val set = r.set(type, category.id, isDirect)
            if (set == null) {
                questionSet = null
                setQuestions.clear(); setQuestions.addAll(QuoteQuestions.forCategory(category, isDirect))
                val keep = setQuestions.map { it.id }.toSet() + REQUESTER_TYPE_QID
                answers.keys.filter { it !in keep }.forEach { answers.remove(it) }
                applySeedsOnce()
            } else applySet(set)
            // 현재 위치는 0단계 다음.
            editReturnId = null
            currentIndex = minOf(1, questions.lastIndex)
            loading = false
        }
    }

    // MARK: - 질문 목록

    /** 지금 묻는 질문들 — 0단계 + 세트(visibleWhen 통과한 것만). */
    val questions: List<QuoteQuestion>
        get() {
            val visible = setQuestions.filter { it.isVisible(answers) }
            return stepZero?.let { listOf(it) + visible } ?: visible
        }

    val title: String get() = category.title
    val currentQuestionOrNull: QuoteQuestion? get() = questions.getOrNull(currentIndex)
    val currentQuestion: QuoteQuestion get() = questions[currentIndex]
    val isOnStepZero: Boolean get() = stepZero != null && currentIndex == 0
    /** 고른 신청자 유형과 받은 세트가 같은지 — 다르면 [다음]이 세트를 다시 받는다. */
    private val stepZeroDirty: Boolean
        get() {
            val picked = answer(REQUESTER_TYPE_QID).selectedOptionIds.firstOrNull() ?: return false
            return questionSet?.requesterType != picked
        }
    val isLastQuestion: Boolean get() = !isOnStepZero && currentIndex >= questions.size - 1

    /** 0.0~1.0 — iOS: currentIndex / max(count-1, 1) */
    val progress: Float
        get() = if (questions.isEmpty()) 0f
        else currentIndex.toFloat() / maxOf(questions.size - 1, 1).toFloat()

    /** 현재 질문 이전(이미 답변한) 질문들 — 채팅 로그용. */
    val answeredQuestions: List<QuoteQuestion> get() = questions.take(currentIndex)

    fun answer(q: QuoteQuestion): QuoteAnswer = answer(q.id)
    private fun answer(id: String): QuoteAnswer = answers[id] ?: QuoteAnswer(id)

    val canProceed: Boolean
        get() {
            if (loading) return false
            val q = currentQuestionOrNull ?: return false
            if (!q.required) return true
            return !answer(q).isEmpty
        }

    fun isSelected(option: QuoteOption): Boolean =
        answer(currentQuestion).selectedOptionIds.contains(option.id)

    fun toggleOption(option: QuoteOption) {
        val q = currentQuestion
        val a = answer(q)
        val next = when (q.type) {
            QuoteAnswerType.SINGLE -> a.copy(selectedOptionIds = listOf(option.id))
            QuoteAnswerType.MULTI -> when {
                a.selectedOptionIds.contains(option.id) -> a.copy(selectedOptionIds = a.selectedOptionIds - option.id)
                // ★ 2026-11-10 '무관' 같은 단독 선택지 — 고르면 다른 선택을 푼다 / 다른 걸 고르면 단독 선택지를 푼다.
                option.exclusive -> a.copy(selectedOptionIds = listOf(option.id))
                else -> {
                    val exclusive = q.options.filter { it.exclusive }.map { it.id }.toSet()
                    a.copy(selectedOptionIds = a.selectedOptionIds.filter { it !in exclusive } + option.id)
                }
            }
            else -> a
        }
        answers[q.id] = next
    }

    fun setRegion(region: String, code: String?) {
        answers[currentQuestion.id] = answer(currentQuestion).copy(region = region, regionCode = code)
    }

    fun setText(text: String) {
        answers[currentQuestion.id] = answer(currentQuestion).copy(text = text)
    }

    fun setDateText(dateText: String, dateValue: String? = null) {
        answers[currentQuestion.id] = answer(currentQuestion).copy(dateText = dateText, dateValue = dateValue)
    }

    /** 다음으로. 마지막이면 onComplete — iOS next(onComplete:) 동일. */
    fun goNextOrComplete(onComplete: () -> Unit) {
        if (loading) return
        // ★ 2026-11-10 0단계 — 고른 유형의 세트를 (다시) 받는다. 받은 뒤 0단계 다음으로.
        if (isOnStepZero && (stepZeroDirty || questionSet == null) && repo != null &&
            answer(REQUESTER_TYPE_QID).selectedOptionIds.isNotEmpty()
        ) {
            loadSetFor(answer(REQUESTER_TYPE_QID).selectedOptionIds.first())
            return
        }
        val returnId = editReturnId
        if (returnId != null) {
            val list = questions
            val returnIdx = list.indexOfFirst { it.id == returnId }.takeIf { it >= 0 } ?: list.lastIndex
            // 수정으로 새로 보이게 된 질문(예: when=date → whenDate)이 비어 있으면 그것부터 묻는다.
            val pending = (currentIndex + 1 until returnIdx).firstOrNull { i -> list[i].required && answer(list[i]).isEmpty }
            if (pending != null) {
                currentIndex = pending
            } else {
                editReturnId = null
                currentIndex = maxOf(0, minOf(returnIdx, list.lastIndex))
            }
        } else if (isLastQuestion) {
            onComplete()
        } else {
            currentIndex += 1
        }
    }

    /** 이전 답변 '수정' — 해당 질문으로 이동. */
    fun editStep(questionId: String) {
        val list = questions
        val idx = list.indexOfFirst { it.id == questionId }
        if (idx >= 0) {
            if (editReturnId == null) editReturnId = list.getOrNull(currentIndex)?.id
            currentIndex = idx
        }
    }

    fun back() {
        if (currentIndex > 0) currentIndex -= 1
    }

    // MARK: - 보내기

    /** 고른 신청자 유형 — 세트를 받았을 때만(폴백이면 null → 보내지 않는다). 서버 응답의 유형을 따른다. */
    val requesterType: String? get() = questionSet?.requesterType
    val questionSetId: Int? get() = questionSet?.setId
    /** 같이 올리기 스펙 — 세트에 있을 때만. */
    val crossPost: List<CrossPostSpec> get() = questionSet?.crossPost.orEmpty()

    /** ★ 2026-11-10 마지막 단계 '☑ 채용 공고에도 올립니다' — 종류별 체크(없으면 기본 체크). */
    private val crossPostOff = mutableStateListOf<String>()
    fun isCrossPostOn(kind: String): Boolean = kind !in crossPostOff
    fun setCrossPost(kind: String, on: Boolean) { if (on) crossPostOff.remove(kind) else if (kind !in crossPostOff) crossPostOff.add(kind) }
    /** 보낼 종류 — 체크된 것만. */
    val checkedCrossPostKinds: List<String> get() = crossPost.map { it.kind }.filter { isCrossPostOn(it) }

    /** 보낼 답 — 지금 보이는 질문의 답만(0단계·숨은 질문 답은 뺀다). */
    fun submitAnswers(): Map<String, QuoteAnswer> {
        val ids = questions.filter { it.id != REQUESTER_TYPE_QID }.map { it.id }.toSet()
        return answers.filterKeys { it in ids }
    }
}
