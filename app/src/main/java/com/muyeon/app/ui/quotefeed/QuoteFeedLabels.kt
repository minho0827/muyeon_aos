package com.muyeon.app.ui.quotefeed

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.format.timeAgo
import com.muyeon.domain.model.quotefeed.QuoteFeedItem

/**
 * '레슨을 찾는 회원들' 카드·줄 문구 — 웹 HomeQuoteRequests.js(seekingLine · 희망 요일/시간 · 레슨 형태 · "N시간 전 등록")와 같다.
 *   선택지 id → 짧은 이름은 웹 constants/quoteCategories OPTION_LABELS 그대로(strings_quotefeed.xml).
 */
object QuoteFeedLabels {
    /** 장르 그림 — 레슨 요청 장르 고르기(QuoteCategoryScreen)와 홈 장르 아이콘 줄과 같은 그림. 없으면 null(취미무용 등). */
    @DrawableRes
    fun genreImage(categoryId: String?): Int? = when (categoryId) {
        "ballet" -> R.drawable.img_genre_ballet
        "korean" -> R.drawable.img_genre_korean
        "modern" -> R.drawable.img_genre_modern
        "practical" -> R.drawable.img_genre_practical
        "musical" -> R.drawable.img_genre_musical
        "barre" -> R.drawable.img_genre_barre
        "balletfit" -> R.drawable.img_genre_balletfit
        else -> null
    }

    @Composable
    fun genre(categoryId: String?): String? = when (categoryId) {
        "ballet" -> R.string.quote_feed_cat_ballet
        "korean" -> R.string.quote_feed_cat_korean
        "modern" -> R.string.quote_feed_cat_modern
        "practical" -> R.string.quote_feed_cat_practical
        "musical" -> R.string.quote_feed_cat_musical
        "barre" -> R.string.quote_feed_cat_barre
        "balletfit" -> R.string.quote_feed_cat_balletfit
        "hobby" -> R.string.quote_feed_cat_hobby
        else -> null
    }?.let { stringResource(it) }

    /** "발레를 가르쳐주실 강사님을 찾고 있어요"(받침에 따라 을/를) — 장르를 모르면 "레슨을 해 주실 …". */
    @Composable
    fun seeking(categoryId: String?): String {
        val g = genre(categoryId) ?: return stringResource(R.string.quote_feed_seeking_any)
        val code = g.last().code - 0xAC00
        val batchim = code in 0..11171 && code % 28 != 0
        return stringResource(R.string.quote_feed_seeking, g, stringResource(if (batchim) R.string.quote_feed_particle_eul else R.string.quote_feed_particle_reul))
    }

    @Composable
    fun region(item: QuoteFeedItem): String = item.region ?: stringResource(R.string.quote_feed_region_any)

    /** "월·수 · 저녁" — 요일·시간대가 다 없으면 "일정 협의". */
    @Composable
    fun schedule(item: QuoteFeedItem): String {
        val days = item.days.map { day(it) }.joinToString("·")
        val times = item.times.map { time(it) }.joinToString("·")
        return listOf(days, times).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { stringResource(R.string.quote_feed_schedule_any) }
    }

    /** "개인 · 3시간 전 등록" */
    @Composable
    fun meta(item: QuoteFeedItem): String {
        val formats = item.formats.map { format(it) }.joinToString("·")
        val created = item.createdAt?.let { stringResource(R.string.quote_feed_created, timeAgo(it)) }.orEmpty()
        return listOf(formats, created).filter { it.isNotBlank() }.joinToString(" · ")
    }

    /**
     * 누르면 갈 곳 — 웹 HomeQuoteRequests path 와 같다. 내 요청이면 받은 견적, 아니면 견적 응답 화면(/availableQuotes/:id).
     *   [sent] 이미 제안을 보낸 요청은 ?sent=1(응답 화면 제목 힌트).
     */
    fun path(item: QuoteFeedItem, sent: Boolean): String =
        if (item.isMine) "/myQuotes/${item.id}" else "/availableQuotes/${item.id}${if (sent) "?sent=1" else ""}"

    @Composable
    private fun day(id: String): String = when (id) {
        "mon" -> R.string.quote_feed_day_mon
        "tue" -> R.string.quote_feed_day_tue
        "wed" -> R.string.quote_feed_day_wed
        "thu" -> R.string.quote_feed_day_thu
        "fri" -> R.string.quote_feed_day_fri
        "sat" -> R.string.quote_feed_day_sat
        "sun" -> R.string.quote_feed_day_sun
        else -> null
    }?.let { stringResource(it) } ?: id

    @Composable
    private fun time(id: String): String = when (id) {
        "dawn" -> R.string.quote_feed_time_dawn
        "morning" -> R.string.quote_feed_time_morning
        "noon" -> R.string.quote_feed_time_noon
        "afternoon" -> R.string.quote_feed_time_afternoon
        "evening" -> R.string.quote_feed_time_evening
        "night" -> R.string.quote_feed_time_night
        else -> null
    }?.let { stringResource(it) } ?: id

    @Composable
    private fun format(id: String): String = when (id) {
        "private" -> R.string.quote_feed_format_private
        "group" -> R.string.quote_feed_format_group
        "academy" -> R.string.quote_feed_format_academy
        "any" -> R.string.quote_feed_format_any
        "etc" -> R.string.quote_feed_format_etc
        else -> null
    }?.let { stringResource(it) } ?: id
}
