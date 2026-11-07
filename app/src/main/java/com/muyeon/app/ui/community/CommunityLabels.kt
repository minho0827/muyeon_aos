package com.muyeon.app.ui.community

import androidx.annotation.StringRes
import com.muyeon.app.R

/** 커뮤니티 카테고리 코드 → 이름. 모르는 코드는 '기타'(웹 categoryLabel — 코드가 그대로 보이지 않게). */
object CommunityLabels {
    @StringRes
    fun category(code: String?): Int = when (code) {
        "QUESTION" -> R.string.community_cat_question
        "CHAT" -> R.string.community_cat_chat
        "ACADEMY_REVIEW" -> R.string.community_cat_academy_review
        "SHOW_REVIEW" -> R.string.community_cat_show_review
        "MEETING" -> R.string.community_cat_meeting
        "MARKET" -> R.string.community_cat_market
        else -> R.string.community_cat_etc
    }
}
