package com.muyeon.app.ui.home

import androidx.annotation.StringRes
import com.muyeon.app.R
import com.muyeon.domain.model.home.HomeQuote

/**
 * 레슨 문의(견적 요청) 문구 — 웹 constants/quoteCategories.js(quoteCategoryLabel · describeAnswers OPTION_LABELS)와 같은 이름.
 */
object QuoteLabels {
    @StringRes
    fun category(id: String?): Int = when (id) {
        "ballet" -> R.string.quote_cat_ballet
        "korean" -> R.string.quote_cat_korean
        "modern" -> R.string.quote_cat_modern
        "practical" -> R.string.quote_cat_practical
        "musical" -> R.string.quote_cat_musical
        "barre" -> R.string.quote_cat_barre
        "balletfit" -> R.string.quote_cat_balletfit
        "hobby" -> R.string.quote_cat_hobby
        else -> R.string.quote_cat_lesson
    }

    fun isGeneric(id: String?) = category(id) == R.string.quote_cat_lesson

    private val DAY_ORDER = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")

    @StringRes
    private fun option(questionId: String, id: String): Int? = when (questionId) {
        "days" -> when (id) { "mon" -> R.string.day_mon; "tue" -> R.string.day_tue; "wed" -> R.string.day_wed; "thu" -> R.string.day_thu
            "fri" -> R.string.day_fri; "sat" -> R.string.day_sat; "sun" -> R.string.day_sun; else -> null }
        "time" -> when (id) { "dawn" -> R.string.time_dawn; "morning" -> R.string.time_morning; "noon" -> R.string.time_noon
            "afternoon" -> R.string.time_afternoon; "evening" -> R.string.time_evening; "night" -> R.string.time_night; else -> null }
        "format" -> when (id) { "private" -> R.string.format_private; "group" -> R.string.format_group; "academy" -> R.string.format_academy
            "any" -> R.string.format_any; "etc" -> R.string.format_etc; else -> null }
        else -> null
    }

    /** 답 하나를 "월, 수" 꼴로(요일은 월→일 순). 없으면 null. */
    fun answer(q: HomeQuote, questionId: String, str: (Int) -> String): String? {
        val a = q.answers.firstOrNull { it.questionId == questionId } ?: return null
        val ids = if (questionId == "days") a.optionIds.sortedBy { DAY_ORDER.indexOf(it) } else a.optionIds
        val text = ids.joinToString(", ") { id -> option(questionId, id)?.let(str) ?: id }
        return text.ifBlank { a.text?.takeIf { it.isNotBlank() } }
    }

    /** 을/를 — 받침 있으면 '을'. */
    fun objectParticle(word: String): String {
        val c = word.lastOrNull()?.code?.minus(0xAC00) ?: return "를"
        return if (c in 0..11171 && c % 28 != 0) "을" else "를"
    }
}
