package com.muyeon.network.dto

import com.muyeon.network.dto.credential.MyCredentialsDto
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-10 경력·자격 신청 철회 — id · withdrawnAt · WITHDRAWN(다시 등록 가능, 철회 불가). */
class CredentialDtoTest {
    private val adapter = Moshi.Builder().build().adapter(MyCredentialsDto::class.java)

    @Test fun `철회한 항목 — id · withdrawnAt · 다시 등록`() {
        val json = """{"identity":{"verified":true},"unsortedPending":false,"items":[
            {"kind":"CAREER","label":"경력","status":"WITHDRAWN","id":3,"withdrawnAt":"2026-11-10T01:00:00.000Z","title":null},
            {"kind":"LICENSE","label":"자격","status":"APPROVED","id":5,"withdrawnAt":null,"years":3.0},
            {"kind":"EDU","label":"학력","status":"PENDING"},
            {"kind":"AWARD","label":"수상","status":"REJECTED","memo":"흐려요"}]}"""
        val items = adapter.fromJson(json)!!.toDomain().items
        val withdrawn = items[0]
        assertEquals(3L, withdrawn.id)
        assertEquals("2026-11-10T01:00:00.000Z", withdrawn.withdrawnAt)
        assertTrue(withdrawn.canSubmit)
        assertFalse(withdrawn.canWithdraw)
        assertEquals(5L, items[1].id)
        assertNull(items[1].withdrawnAt)
        assertTrue(items[1].canWithdraw)
        assertFalse(items[1].canSubmit)
        assertNull(items[2].id)          // 예전 서버 — id 없으면 종류 주소로 철회
        assertTrue(items[2].canWithdraw)
        assertFalse(items[3].canWithdraw)
        assertTrue(items[3].canSubmit)
    }
}
