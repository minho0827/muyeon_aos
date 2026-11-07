package com.muyeon.network.dto

import com.muyeon.domain.model.performance.PerformanceStatus
import com.muyeon.network.dto.common.CursorPageDto
import com.muyeon.network.dto.performance.PerformanceDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PerformanceDtoTest {
    private val moshi = Moshi.Builder().build()
    private val pageAdapter = moshi.adapter<CursorPageDto<PerformanceDto>>(
        Types.newParameterizedType(CursorPageDto::class.java, PerformanceDto::class.java),
    )

    @Test fun `목록 — null 칸·imageUrl 대체·상태·커서`() {
        val json = """{"items":[{"id":7,"title":"지젤","genre":"발레","subGenre":null,"status":"ONGOING","posterUrl":null,
            "imageUrl":"/uploads/view?key=a_v2.jpg","company":"","venue":"예술의전당","startAt":"2026-11-06T15:00:00.000Z","endAt":null},
            {"title":"id 없음"}],"hasMore":true,"nextCursor":"abc"}"""
        val page = pageAdapter.fromJson(json)!!
        val items = page.items!!.mapNotNull { it.toSummary() }
        assertEquals(1, items.size)
        assertEquals(PerformanceStatus.ONGOING, items[0].status)
        assertEquals("/uploads/view?key=a_v2.jpg", items[0].posterUrl)
        assertNull(items[0].company)
        assertEquals("abc", page.next)
    }

    @Test fun `hasMore false 면 커서가 와도 끝`() {
        val page = pageAdapter.fromJson("""{"items":[],"hasMore":false,"nextCursor":"x"}""")!!
        assertNull(page.next)
    }

    @Test fun `상세 — 출처·그림 상세`() {
        val d = moshi.adapter(PerformanceDto::class.java).fromJson(
            """{"id":3,"title":"t","status":"WHAT","images":["a",""],"detailType":"IMAGE","detailImages":["x"],
               "details":{"source":"WEB","sourceName":"블로그","sourceUrl":"https://e.com"}}""",
        )!!.toDetail()!!
        assertNull(d.summary.status)
        assertEquals(listOf("a"), d.images)
        assertEquals("WEB", d.source)
        assertEquals("https://e.com", d.sourceUrl)
    }
}
