package com.muyeon.app.ui.quote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * 견적 문진 상태 — iOS `QuoteWizardViewModel.swift` 1:1 이식.
 *  progress/canProceed/answeredQuestions 계산식과 toggle 규칙(single=치환, multi=토글)을 동일하게 유지.
 */
class QuoteWizardViewModel(
    val category: QuoteCategory,
    seedAnswers: Map<String, QuoteAnswer> = emptyMap(),
) : ViewModel() {

    val questions: List<QuoteQuestion> = QuoteQuestions.forCategory(category)
    val answers = mutableStateMapOf<String, QuoteAnswer>()

    var currentIndex by mutableStateOf(0)
        private set

    init {
        if (seedAnswers.isNotEmpty()) {
            answers.putAll(seedAnswers)
            // 프리필된 질문 뒤(첫 미답변)로 이동 — iOS convenience init 동일.
            while (currentIndex < questions.size - 1 && seedAnswers.containsKey(questions[currentIndex].id)) {
                currentIndex += 1
            }
        }
    }

    val title: String get() = category.title
    val currentQuestion: QuoteQuestion get() = questions[currentIndex]
    val isLastQuestion: Boolean get() = currentIndex >= questions.size - 1

    /** 0.0~1.0 — iOS: currentIndex / max(count-1, 1) */
    val progress: Float
        get() = if (questions.isEmpty()) 0f
        else currentIndex.toFloat() / maxOf(questions.size - 1, 1).toFloat()

    /** 현재 질문 이전(이미 답변한) 질문들 — 채팅 로그용. */
    val answeredQuestions: List<QuoteQuestion> get() = questions.take(currentIndex)

    fun answer(q: QuoteQuestion): QuoteAnswer = answers[q.id] ?: QuoteAnswer(q.id)

    val canProceed: Boolean
        get() {
            val q = currentQuestion
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
            QuoteAnswerType.MULTI ->
                if (a.selectedOptionIds.contains(option.id))
                    a.copy(selectedOptionIds = a.selectedOptionIds - option.id)
                else a.copy(selectedOptionIds = a.selectedOptionIds + option.id)
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

    fun setDateText(dateText: String) {
        answers[currentQuestion.id] = answer(currentQuestion).copy(dateText = dateText)
    }

    /** 다음으로. 마지막이면 onComplete — iOS next(onComplete:) 동일. */
    fun goNextOrComplete(onComplete: () -> Unit) {
        if (isLastQuestion) onComplete() else currentIndex += 1
    }

    /** 이전 답변 '수정' — 해당 질문으로 이동. */
    fun editStep(questionId: String) {
        val idx = questions.indexOfFirst { it.id == questionId }
        if (idx >= 0) currentIndex = idx
    }

    fun back() {
        if (currentIndex > 0) currentIndex -= 1
    }
}
