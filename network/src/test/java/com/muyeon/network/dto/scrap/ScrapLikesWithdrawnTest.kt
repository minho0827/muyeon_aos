package com.muyeon.network.dto.scrap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-19 찜 목록 탈퇴 표시 — 강사·무용수만 withdrawn 을 옮긴다. */
class ScrapLikesWithdrawnTest {
    private fun entry(type: String, withdrawn: Boolean?) =
        ScrapEntryDto(type, 1L, ScrapSummaryDto(name = "탈퇴한 회원", withdrawn = withdrawn)).toDomain()!!

    @Test fun `강사·무용수 탈퇴 표시`() {
        assertTrue(entry("TEACHER", true).withdrawn)
        assertTrue(entry("DANCER", true).withdrawn)
        assertEquals("탈퇴한 회원", entry("DANCER", true).title)
    }

    @Test fun `없거나 다른 종류면 false`() {
        assertFalse(entry("TEACHER", null).withdrawn)
        assertFalse(entry("TEACHER", false).withdrawn)
        assertFalse(entry("LESSON", true).withdrawn)
    }
}
