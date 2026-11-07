package com.muyeon.app.ui.home

import androidx.annotation.StringRes
import com.muyeon.app.R

/** 클래식 홈 영역·세부 이름(웹 homeDomains · domainLabelMap · feedTagLabel). 한글 값(장르 등)은 그대로. */
object HomeDomainLabels {
    @StringRes
    fun chip(code: String): Int? = when (code) {
        "ALL" -> R.string.common_all
        "JOB" -> R.string.home_domain_job
        "SUB" -> R.string.home_domain_sub
        "CASTING" -> R.string.home_domain_casting
        "SPACE" -> R.string.home_domain_space
        "CLASS" -> R.string.home_domain_class
        else -> null
    }

    /** 카드 칩(웹 domainLabelMap — 캐스팅은 '캐스팅'). */
    @StringRes
    fun cardChip(domain: String): Int? = when (domain) {
        "JOB" -> R.string.home_domain_job
        "SUB" -> R.string.home_domain_sub
        "CASTING" -> R.string.home_kind_casting
        "SPACE" -> R.string.home_domain_space
        "CLASS" -> R.string.home_domain_class
        else -> null
    }

    /** 세부·태그 코드(커뮤니티 카테고리·수업 대상) 이름. 모르면 null(값 그대로 쓴다). */
    @StringRes
    fun tag(value: String): Int? = when (value) {
        "QUESTION" -> R.string.community_cat_question
        "CHAT" -> R.string.community_cat_chat
        "ACADEMY_REVIEW" -> R.string.community_cat_academy_review
        "SHOW_REVIEW" -> R.string.community_cat_show_review
        "MEETING" -> R.string.community_cat_meeting
        "MARKET" -> R.string.community_cat_market
        "KIDS" -> R.string.target_kids
        "ELEM" -> R.string.target_elem
        "TEEN" -> R.string.target_teen
        "ADULT" -> R.string.target_adult
        "EXAM" -> R.string.target_exam
        "MAJOR" -> R.string.target_major
        else -> null
    }
}
