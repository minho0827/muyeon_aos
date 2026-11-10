package com.muyeon.app.ui.castingstage.create

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** 공고 수정 — GET 원본 → 폼 채우기, 바뀐 칸만 PATCH(서버 update 규칙). */
class CastingEditRulesTest {
    private val raw = JSONObject(
        """{"id":7,"title":"정기공연","genre":"발레","role":"군무, 솔리스트","headcount":4,"region":"서울",
           "place":"예술의전당","schedule":"2026-12-01","imageUrl":"https://x/p.jpg","gender":"여성","height":"165cm",
           "details":{"deadline":"2026-11-20","fee":"10만원 미만","feeRange":"F0_10","age":"20~30세",
                      "rehearsal":"2026-11-25 ~ 2026-11-30","applyMethod":"앱","contact":"010","summary":"설명",
                      "requirements":{"video":true,"photo":false}}}""",
    )

    @Test fun fillsAllSteps() {
        val f = CastingEditRules.formFrom(raw)
        assertEquals("정기공연", f.title)
        assertEquals(listOf("군무", "솔리스트"), f.roles)
        assertEquals("4", f.headcount)
        assertEquals(LocalDate.of(2026, 12, 1), f.schedule)
        assertEquals(LocalDate.of(2026, 11, 20), f.deadline)
        assertEquals(LocalDate.of(2026, 11, 25), f.rehearsalStart)
        assertEquals(LocalDate.of(2026, 11, 30), f.rehearsalEnd)
        // 구간 이름만 들어 있던 fee 는 부가설명으로 보지 않는다.
        assertEquals("", f.fee)
        assertEquals("F0_10", f.feeRange)
        assertTrue(f.videoRequired)
        assertEquals("설명", f.desc)
    }

    @Test fun patchSendsOnlyChanged() {
        val o = CastingEditRules.formFrom(raw)
        assertEquals(0, CastingEditRules.patchBody(o, o).length())
        val body = CastingEditRules.patchBody(o, o.copy(title = "정기공연 2", deadlineUndecided = true, deadline = null))
        assertEquals(setOf("title", "deadline"), body.keys().asSequence().toSet())
        assertEquals("-", body.getString("deadline"))   // 미정
        // 11-10~ 등록 칸 전부 고칠 수 있다.
        val more = CastingEditRules.patchBody(o, o.copy(height = "170cm", rehearsalFee = true, feeRange = null, rehearsalEnd = LocalDate.of(2026, 12, 2)))
        assertEquals(setOf("height", "rehearsalFee", "feeRange", "rehearsal"), more.keys().asSequence().toSet())
        assertEquals("", more.getString("feeRange"))   // 비우기
        assertEquals("2026-11-25 ~ 2026-12-02", more.getString("rehearsal"))
        val req = CastingEditRules.patchBody(o, o.copy(photoRequired = true))
        assertTrue(req.getJSONObject("requirements").getBoolean("photo"))
        assertFalse(req.has("videoRequired"))
    }

    @Test fun freeTextScheduleIsKept() {
        val o = CastingEditRules.formFrom(JSONObject("""{"title":"공연","schedule":"12월 매주 토","role":"군무"}"""))
        assertNull(o.schedule)
        assertFalse(CastingEditRules.patchBody(o, o.copy(place = "대학로")).has("schedule"))
    }

    @Test fun titleRule() {
        assertEquals("공고 제목을 2자 이상 적어 주세요.", CastingEditRules.validate(CastingCreateForm(title = "가")))
        assertNull(CastingEditRules.validate(CastingCreateForm(title = "가나")))
    }
}
