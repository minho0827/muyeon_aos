package com.muyeon.app.ui.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 빠른 이동 검색 — 서버가 주는 keywords·chosung 으로 기기에서 거른다(iOS AssistantSearch 와 같은 규칙). */
class AssistantSearchTest {
    private fun item(id: String, title: String, aliases: List<String>, cho: List<String>) = AssistantItem(
        id, title, null, "레슨", "NATIVE", "openLessonCreate", emptyMap(), null, null, false,
        (listOf(title) + aliases).map { it.lowercase() }, cho,
    )

    private val items = listOf(
        item("lesson.create", "레슨 등록", listOf("수업 등록"), listOf("ㄹㅅㄷㄹ", "ㅅㅇㄷㄹ")),
        item("academy.profile", "학원 프로필", listOf("학원 소개"), listOf("ㅎㅇㅍㄹㅍ", "ㅎㅇㅅㄱ")),
        item("lesson.mine", "내 레슨 관리", listOf("내 수업"), listOf("ㄴㄹㅅㄱㄹ", "ㄴㅅㅇ")),
        item("faq", "자주 묻는 질문", listOf("FAQ"), listOf("ㅈㅈㅁㄴㅈㅁ")),
    )

    @Test fun 앞글자() {
        assertEquals("lesson.create", AssistantSearch.search(items, "레").first().id)
        assertEquals("academy.profile", AssistantSearch.search(items, "학").first().id)
    }

    @Test fun 제목_앞글자가_포함보다_먼저() {
        assertEquals(listOf("lesson.create", "lesson.mine"), AssistantSearch.search(items, "레슨").map { it.id })
    }

    @Test fun 별칭과_영문() {
        assertTrue(AssistantSearch.search(items, "수업").any { it.id == "lesson.create" })
        assertEquals("faq", AssistantSearch.search(items, "faq").first().id)
    }

    @Test fun 초성() {
        assertTrue(AssistantSearch.isChosungQuery("ㄹㅅ"))
        assertEquals("lesson.create", AssistantSearch.search(items, "ㄹㅅ").first().id)
    }

    @Test fun 없는_말() {
        assertTrue(AssistantSearch.search(items, "없는메뉴").isEmpty())
    }
}
