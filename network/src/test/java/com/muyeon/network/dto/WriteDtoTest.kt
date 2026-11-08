package com.muyeon.network.dto

import com.muyeon.domain.model.write.CommunityDraft
import com.muyeon.domain.model.write.SubUpdate
import com.muyeon.network.dto.write.CommunityPostBody
import com.muyeon.network.dto.write.SubEditDto
import com.muyeon.network.dto.write.SubUpdateBody
import com.squareup.moshi.Moshi
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 묶음 3 — 대타 수정 본문(빈 칸 빼기 · 급여만 null) · 커뮤니티 글 본문(새 글 사진 없으면 images 빼기) · 수정 화면 채우기. */
class WriteDtoTest {
    private val moshi = Moshi.Builder().build()
    private fun update(salary: String?, sendSalary: Boolean) = SubUpdate(
        title = "t", academy = null, genre = "발레", target = "KIDS", fields = emptyList(), scheduleType = null,
        classDate = "2026-12-01", classTime = "19:00~20:00", classCount = null, region = null, regionCode = null,
        salary = salary, sendSalary = sendSalary, career = null, description = null, image = null,
    )
    private fun json(u: SubUpdate) = Buffer().also { SubUpdateBody.of(moshi, u).writeTo(it) }.readUtf8()

    @Test fun `대타 수정 — 비운 급여는 null, 빈 칸은 뺀다`() {
        val j = json(update(null, sendSalary = true))
        assertTrue(j.contains("\"salary\":null"))
        assertFalse(j.contains("academy"))
        assertFalse(j.contains("fields"))
        assertFalse(j.contains("classCount"))
        assertTrue(j.contains("\"classTime\":\"19:00~20:00\""))
    }

    @Test fun `대타 수정 — 긴급 공고는 급여를 안 보낸다`() {
        assertFalse(json(update("W3_4", sendSalary = false)).contains("salary"))
        assertTrue(json(update("W3_4", sendSalary = true)).contains("\"salary\":\"W3_4\""))
    }

    @Test fun `커뮤니티 — 새 글 사진 없으면 images 없음, 수정은 빈 배열`() {
        val d = CommunityDraft("CHAT", "t", "b", emptyList())
        assertNull(CommunityPostBody.of(d, edit = false).images)
        assertEquals(emptyList<String>(), CommunityPostBody.of(d, edit = true).images)
        assertEquals(listOf("/u/1.jpg"), CommunityPostBody.of(d.copy(images = listOf("/u/1.jpg")), edit = false).images)
    }

    @Test fun `대타 수정 화면 채우기`() {
        val dto = moshi.adapter(SubEditDto::class.java).fromJson(
            """{"title":"x","genre":"발레","classCount":0,"imageUrl":"/u/a.jpg","dispatchEnabled":true,"confirmedApplicationId":3,"fields":["BASIC",""]}""")!!
        val f = dto.toDomain()
        assertNull(f.classCount)
        assertEquals("/u/a.jpg", f.image)
        assertTrue(f.dispatchEnabled)
        assertTrue(f.payLocked)
        assertEquals(listOf("BASIC"), f.fields)
    }
}
