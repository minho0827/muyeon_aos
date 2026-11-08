package com.muyeon.app.ui.likes

import android.content.Context
import com.muyeon.app.R
import com.muyeon.app.ui.community.CommunityLabels
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.resume.ResumeOptions
import com.muyeon.domain.model.scrap.LikedItem

/** 찜 카드 표시값 — 웹 Likes cardOf(종류마다 귀퉁이 한 줄이 다르다). */
object LikesLabels {
    fun corner(item: LikedItem, ctx: Context): String = when (item.targetType) {
        "TEACHER", "DANCER" -> (item.fields.map(ResumeOptions::fieldLabel).ifEmpty { item.genres }.take(2) +
            listOfNotNull(item.career?.let(ResumeOptions::careerLabel)?.takeIf { it.isNotBlank() })).joinToString(" · ")
        "SPACE" -> item.pricePerHour?.let { ctx.getString(R.string.likes_price_hour, HomeLabels.won(it)) } ?: item.region.orEmpty()
        "LESSON" -> listOfNotNull(item.genre, item.price?.let { ctx.getString(R.string.home_price_won, HomeLabels.won(it)) }).joinToString(" · ")
        "PERFORMANCE" -> listOfNotNull(item.genre, item.venue).joinToString(" · ")
        "POST" -> item.category?.let { ctx.getString(CommunityLabels.category(it)) }.orEmpty()
        else -> listOfNotNull(item.genre, item.salary?.let(ResumeOptions::salaryLabel)?.takeIf { it.isNotBlank() }).joinToString(" · ")
    }

    /** 종류 칩 이름. */
    fun tab(type: String): Int = when (type) {
        "LESSON" -> R.string.likes_tab_lesson
        "TEACHER" -> R.string.likes_tab_teacher
        "PERFORMANCE" -> R.string.likes_tab_performance
        "POST" -> R.string.likes_tab_post
        else -> R.string.common_all
    }

    /** 누르면 갈 주소(웹 PATH_BY_TYPE) — 앱 화면이 있으면 NativeRoutes 가 앱으로 연다. */
    fun path(item: LikedItem): String? = when (item.targetType) {
        "LESSON" -> "/lessons/${item.targetId}"
        "TEACHER" -> "/teachers/${item.targetId}"
        "PERFORMANCE" -> "/performance/${item.targetId}"
        "SPACE" -> "/spaces/${item.targetId}"
        "JOB" -> "/jobs/${item.targetId}"
        "SUB" -> "/subs/${item.targetId}"
        "DANCER" -> "/dancers/${item.targetId}"
        "CASTING" -> "/casting/${item.targetId}"
        "POST" -> "/community/post/${item.targetId}"
        else -> null
    }
}
