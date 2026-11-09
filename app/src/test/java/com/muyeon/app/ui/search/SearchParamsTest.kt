package com.muyeon.app.ui.search

import com.muyeon.domain.model.search.SearchFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 검색 필터 → 서버 쿼리 · 정렬 키 · 칩 점 상태. */
class SearchParamsTest {

    private val P = PerformanceSearchDomain
    private val C = CastingSearchDomain

    // ── 공연

    @Test fun `공연 - 빈 조건은 sort 만 보낸다`() {
        assertEquals(mapOf("sort" to "RECOMMENDED"), P.params("", SearchFilter.Empty, "RECOMMENDED").toQuery())
    }

    @Test fun `공연 - 여러 값은 칩 순서대로 콤마`() {
        val f = SearchFilter.Empty
            .toggle(FilterKeys.STATUS, "ENDED").toggle(FilterKeys.STATUS, "UPCOMING")
            .toggle(FilterKeys.GENRE, "현대무용").toggle(FilterKeys.GENRE, "발레")
            .toggle(FilterKeys.REGION, "경기").toggle(FilterKeys.REGION, "서울")
        val q = P.params("  발레   갈라 ", f, "ENDING").toQuery()
        assertEquals("발레 갈라", q["q"])
        assertEquals("UPCOMING,ENDED", q["statuses"])
        assertEquals("발레,현대무용", q["genres"])
        assertEquals("서울,경기", q["regions"])
        assertEquals("ENDING", q["sort"])
    }

    @Test fun `공연 - 2자 미만 검색어는 보내지 않는다`() {
        assertNull(P.params(" 발 ", SearchFilter.Empty, "START").toQuery()["q"])
    }

    @Test fun `공연 - 정렬 키와 모르는 정렬은 기본값`() {
        assertEquals(listOf("RECOMMENDED", "START", "ENDING", "RECENT"), P.SORTS.map { it.key })
        assertEquals("RECOMMENDED", P.params("", SearchFilter.Empty, "DEADLINE").sort)
    }

    @Test fun `공연 - 섹션은 상태 장르 지역, 지역 17개`() {
        assertEquals(listOf(FilterKeys.STATUS, FilterKeys.GENRE, FilterKeys.REGION), P.SECTIONS.map { it.key })
        assertEquals(listOf("UPCOMING", "ONGOING", "ENDED"), P.STATUS.order)
        assertEquals(17, P.REGION.options.size)
    }

    // ── 캐스팅

    @Test fun `캐스팅 - 쿼리 키 이름(genres sidos feeRange)`() {
        val f = SearchFilter.Empty
            .toggle(FilterKeys.GENRE, "행사").toggle(FilterKeys.GENRE, "발레")
            .toggle(FilterKeys.REGION, "부산")
            .toggle(FilterKeys.FEE, "F10_30", single = true)
        assertEquals(
            mapOf("q" to "콩쿠르", "genres" to "발레,행사", "sidos" to "부산", "feeRange" to "F10_30", "sort" to "DEADLINE"),
            C.params("콩쿠르", f, "DEADLINE"),
        )
    }

    @Test fun `캐스팅 - 출연료는 하나만, 다시 누르면 해제`() {
        var f = SearchFilter.Empty.toggle(FilterKeys.FEE, "F0_10", single = true)
        f = f.toggle(FilterKeys.FEE, "F100", single = true)
        assertEquals(setOf("F100"), f.of(FilterKeys.FEE))
        f = f.toggle(FilterKeys.FEE, "F100", single = true)
        assertTrue(f.isEmpty)
        assertTrue(C.FEE.single)
    }

    @Test fun `캐스팅 - 정렬 키, 기본은 최근 등록`() {
        assertEquals(listOf("RECENT", "DEADLINE"), C.SORTS.map { it.key })
        assertEquals("RECENT", C.params("", SearchFilter.Empty, "RECOMMENDED")["sort"])
    }

    // ── 칩 줄

    @Test fun `필터 칩 점 - 하나라도 고르면 켜지고 초기화하면 꺼진다`() {
        assertFalse(FilterChipState.dot(SearchFilter.Empty))
        val f = SearchFilter.Empty.toggle(FilterKeys.REGION, "제주")
        assertTrue(FilterChipState.dot(f))
        assertFalse(FilterChipState.dot(f.cleared(listOf(FilterKeys.REGION))))
        // 값을 다시 눌러 끄면 섹션 자체가 빠진다.
        assertFalse(FilterChipState.dot(f.toggle(FilterKeys.REGION, "제주")))
    }

    @Test fun `빠른 칩 이름 - 없으면 null, 여럿이면 첫 값과 나머지 수`() {
        assertNull(FilterChipState.picked(SearchFilter.Empty, P.GENRE))
        val f = SearchFilter.Empty.toggle(FilterKeys.GENRE, "뮤지컬").toggle(FilterKeys.GENRE, "발레")
        val picked = FilterChipState.picked(f, P.GENRE)!!
        assertEquals("발레", picked.first.value)
        assertEquals(1, picked.second)
        assertTrue(FilterChipState.active(f, P.GENRE))
        assertFalse(FilterChipState.active(f, P.REGION))
    }

    @Test fun `초기화는 시트 섹션만 비운다`() {
        val f = SearchFilter.Empty.toggle(FilterKeys.GENRE, "발레").toggle(FilterKeys.REGION, "서울")
        val r = f.cleared(listOf(FilterKeys.GENRE))
        assertTrue(r.of(FilterKeys.GENRE).isEmpty())
        assertEquals(setOf("서울"), r.of(FilterKeys.REGION))
    }
}
