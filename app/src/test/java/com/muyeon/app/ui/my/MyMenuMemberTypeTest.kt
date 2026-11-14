package com.muyeon.app.ui.my

import org.junit.Assert.assertEquals
import org.junit.Test

/** ★ 2026-11-14 '회원유형 변경 / 수정' 맨 위 자리(예전 개인정보 관리 자리) · 중복 없음. */
class MyMenuMemberTypeTest {
    private val title = "회원유형 변경 / 수정"
    private fun item(t: String, path: String? = null, bridge: String? = null) = MyMenuItem(t, "myinfo", path, bridge)

    @Test fun replacesProfileEditAndRemovesOldRow() {
        val groups = listOf(
            listOf(item("개인정보 관리", "/profileEdit"), item("레슨 요청하기", bridge = "quoteRequest")),
            listOf(item("찜/관심", "/likes"), item("회원유형 설정 / 인증", bridge = "memberType"), item("FAQ", "/faq")),
        )
        val out = MyMenuBuilder.memberTypeOnTop(groups, title)
        assertEquals(listOf(title, "레슨 요청하기"), out[0].map { it.title })
        assertEquals(listOf("찜/관심", "FAQ"), out[1].map { it.title })
        assertEquals(1, out.flatten().count { it.bridge == "memberType" })
        assertEquals(0, out.flatten().count { it.path == "/profileEdit" })
    }

    @Test fun insertsOnTopWhenNoProfileRow() {
        val out = MyMenuBuilder.memberTypeOnTop(listOf(listOf(item("FAQ", "/faq"))), title)
        assertEquals(listOf(title, "FAQ"), out[0].map { it.title })
    }
}
