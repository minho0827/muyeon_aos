package com.muyeon.network.dto

import com.muyeon.network.dto.quotefeed.QuoteFeedAvailableDto
import com.muyeon.network.dto.quotefeed.QuoteFeedItemDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-08 '레슨을 찾는 회원들' — quotes home · available 응답 → 앱 모델. */
class QuoteFeedDtoTest {
    private val moshi = Moshi.Builder().build()

    @Test fun `홈 — 요일 순서 맞춤 · id 없는 칸 버림 · isMine·canRespond 그대로`() {
        val type = Types.newParameterizedType(List::class.java, QuoteFeedItemDto::class.java)
        val json = """[
            {"id":7,"categoryId":"ballet","region":"서울 강남구","answers":[{"questionId":"days","optionIds":["sat","mon"]},{"questionId":"time","optionIds":["evening"]},{"questionId":"format","optionIds":["private"]}],
             "createdAt":"2026-11-08T01:00:00.000Z","isDemo":false,"isMine":true,"canRespond":false},
            {"categoryId":"korean"}]"""
        val items = moshi.adapter<List<QuoteFeedItemDto>>(type).fromJson(json)!!.mapNotNull { it.toDomain() }
        assertEquals(1, items.size)
        assertEquals(listOf("mon", "sat"), items[0].days)
        assertEquals(listOf("evening"), items[0].times)
        assertTrue(items[0].isMine)
        assertFalse(items[0].canRespond)
    }

    @Test fun `강사 목록 — 보낼 수 있는 요청만 오므로 canRespond, 비회원 요청 표시`() {
        val json = """{"needsGenre":false,"items":[{"id":3,"categoryId":"modern","region":"","answers":[{"questionId":"detail","text":"비공개"}],"guestRequest":true}],"total":21,"page":0,"hasMore":true}"""
        val p = moshi.adapter(QuoteFeedAvailableDto::class.java).fromJson(json)!!.toDomain(0)
        assertTrue(p.hasMore)
        assertTrue(p.items[0].canRespond)
        assertTrue(p.items[0].guestRequest)
        assertEquals(null, p.items[0].region)
    }
}
