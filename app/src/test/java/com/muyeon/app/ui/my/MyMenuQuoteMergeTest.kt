package com.muyeon.app.ui.my

import org.junit.Assert.assertEquals
import org.junit.Test

/** ★ 2026-11-15 일반 회원 MY — '레슨 요청하기'·'레슨 요청 내역'이 두 개로 보이던 것 → '내 레슨 요청' 하나. */
class MyMenuQuoteMergeTest {
    private fun item(t: String, bridge: String? = null) = MyMenuItem(t, "filled-note", "/myQuotes", bridge)

    @Test fun dropsRequestRowWhenMyQuotesExists() {
        val out = MyMenuBuilder.dropQuoteRequestBesideMyQuotes(listOf(
            listOf(item("레슨 요청하기", "quoteRequest"), item("내 레슨 요청", "myQuotes"), item("예약 내역")),
        ))
        assertEquals(listOf("내 레슨 요청", "예약 내역"), out[0].map { it.title })
    }

    @Test fun keepsRequestRowWhenNoMyQuotes() {
        val out = MyMenuBuilder.dropQuoteRequestBesideMyQuotes(listOf(listOf(item("레슨 요청하기", "quoteRequest"))))
        assertEquals(listOf("레슨 요청하기"), out[0].map { it.title })
    }
}
