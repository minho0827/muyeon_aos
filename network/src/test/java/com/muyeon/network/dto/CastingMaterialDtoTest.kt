package com.muyeon.network.dto

import com.muyeon.network.dto.castingmaterial.CastingMaterialListDto
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-08 묶음 2 — casting-materials list() 응답 → 앱 모델(등록 글 주소·건너뜀·처리 중). */
class CastingMaterialDtoTest {
    private val adapter = Moshi.Builder().build().adapter(CastingMaterialListDto::class.java)

    @Test fun `목록 — 등록 글 주소 · 건너뜀 · 처리 중 · id 없는 칸 버림`() {
        val json = """{"items":[
            {"id":3,"status":"DONE","imageCount":2,"instagramUrl":"https://www.instagram.com/p/x","linkUrl":null,"createdAt":"2026-11-08T01:02:00.000Z",
             "result":{"created":[{"title":"지젤 캐스팅","castingId":11},{"title":"공연","performanceId":5}],"skipped":[{"image":1,"reason":"포스터 아님"}]},"errorMessage":null},
            {"id":4,"status":"PROCESSING","imageCount":1,"result":null},
            {"status":"DONE"}]}"""
        val items = adapter.fromJson(json)!!.items!!.mapNotNull { it.toDomain() }
        assertEquals(2, items.size)
        assertEquals(listOf("/casting/11", "/performance/5"), items[0].created.map { it.path })
        assertEquals(1, items[0].skipped[0].image)
        assertNull(items[0].linkUrl)
        assertTrue(items[1].processing)
    }
}
