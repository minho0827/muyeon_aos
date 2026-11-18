package com.muyeon.app.ui.space

import com.muyeon.app.ui.space.wizard.SpaceBizCheck
import com.muyeon.app.ui.space.wizard.SpaceBusinessInput
import com.muyeon.app.ui.space.wizard.SpaceBusinessRules
import com.muyeon.app.ui.space.wizard.SpaceBusinessStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2026-11-18 공간 사업자 확인 — 자동 확인 결과 문구·[다음] 켜짐·내 공간 줄. */
class SpaceBusinessRulesTest {
    private val ok = SpaceBizCheck(true, "", null)

    @Test fun question() {
        assertEquals("지금 운영 중이신 '루체 발레'을 공간으로 등록하시나요?", SpaceBusinessRules.question("루체 발레"))
        assertEquals("지금 운영 중이신 학원을 공간으로 등록하시나요?", SpaceBusinessRules.question(" "))
        assertEquals("지금 운영 중이신 학원을 공간으로 등록하시나요?", SpaceBusinessRules.question(null))
    }

    @Test fun checkRequestNeedsTenDigits() {
        assertNull(SpaceBusinessRules.checkRequest("123-45-678", ""))
        assertEquals("1234567890" to null, SpaceBusinessRules.checkRequest("123-45-67890", "2020"))
        assertEquals("1234567890" to "20200115", SpaceBusinessRules.checkRequest("1234567890", "20200115"))
    }

    @Test fun checkMessages() {
        assertEquals("휴업자예요." to true, SpaceBusinessRules.checkMessage(SpaceBizCheck(false, "휴업자예요.", null), false))
        assertEquals(SpaceBusinessRules.OWNER_MATCHED to false, SpaceBusinessRules.checkMessage(SpaceBizCheck(true, "", true), false))
        // 불일치·개업일자 없음 → 안내 없음(불일치 경고 금지).
        assertNull(SpaceBusinessRules.checkMessage(SpaceBizCheck(true, "", false), false))
        assertNull(SpaceBusinessRules.checkMessage(ok, false))
        assertNull(SpaceBusinessRules.checkMessage(null, false))
        assertEquals(SpaceBusinessRules.CHECK_DELAYED to true, SpaceBusinessRules.checkMessage(null, true))
    }

    @Test fun canSubmit() {
        assertTrue(SpaceBusinessRules.canSubmitOwn("1234567890", "", ok, checking = false, uploading = false))
        assertTrue(SpaceBusinessRules.canSubmitOwn("1234567890", "20200115", SpaceBizCheck(true, "", false), false, false))
        assertFalse(SpaceBusinessRules.canSubmitOwn("1234567890", "", SpaceBizCheck(false, "x", null), false, false))
        assertFalse(SpaceBusinessRules.canSubmitOwn("1234567890", "2020", ok, false, false))
        assertFalse(SpaceBusinessRules.canSubmitOwn("1234567890", "", ok, checking = true, uploading = false))
        assertFalse(SpaceBusinessRules.canSubmitOwn("1234567890", "", null, false, false))
    }

    @Test fun inputJson() {
        assertEquals("""{"source":"ACADEMY"}""", SpaceBusinessInput.ACADEMY.toJson().toString())
        val o = SpaceBusinessInput.own("123-45-67890", "2020", listOf("https://a/1.jpg")).toJson()
        assertEquals("OWN", o.getString("source"))
        assertEquals("1234567890", o.getString("number"))
        assertFalse(o.has("startDate"))
        assertEquals("BUSINESS_REGISTRATION", o.getJSONArray("docs").getJSONObject(0).getString("type"))
    }

    @Test fun mineLines() {
        assertNull(SpaceBusinessRules.mineLine(null))
        assertNull(SpaceBusinessRules.mineLine(SpaceBusinessStatus("VERIFIED", "OWN", "7890", null, null)))
        assertNull(SpaceBusinessRules.mineLine(SpaceBusinessStatus(null, null, null, null, null)))
        val due = SpaceBusinessRules.mineLine(SpaceBusinessStatus(null, null, null, "2026-11-30T15:30:00.000Z", null))!!
        assertEquals("사업자 확인이 필요해요 · 12월 1일까지", due.text)  // UTC 15:30 = 한국 다음 날 00:30
        assertEquals("확인하기", due.action)
        assertEquals("사업자 확인 중이에요", SpaceBusinessRules.mineLine(SpaceBusinessStatus("PENDING", "OWN", null, null, null))!!.text)
        val rej = SpaceBusinessRules.mineLine(SpaceBusinessStatus("REJECTED", "OWN", null, null, "상호가 달라요"))!!
        assertEquals("사업자 확인이 반려됐어요 · 상호가 달라요", rej.text)
        assertEquals("다시 확인하기", rej.action)
    }

    @Test fun statusFromJson() {
        val s = SpaceBusinessStatus.from(JSONObject("""{"status":null,"source":null,"numberTail":null,"dueAt":"2026-12-01","memo":null}"""))!!
        assertNull(s.status)
        assertEquals("12월 1일", SpaceBusinessRules.dueLabel(s.dueAt))
    }

    @Test fun savedMessages() {
        assertEquals("공간을 등록했어요.", SpaceBusinessRules.savedMessage(false, "VERIFIED"))
        assertEquals("공간을 등록했어요. 관리자 확인 후 공개돼요. 보통 1영업일 안에 확인해요.", SpaceBusinessRules.savedMessage(false, "PENDING"))
        assertEquals("수정했어요.", SpaceBusinessRules.savedMessage(true, null))
    }
}
