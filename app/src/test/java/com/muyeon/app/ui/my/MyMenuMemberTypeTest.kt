package com.muyeon.app.ui.my

import com.muyeon.domain.model.member.LessonProviders
import com.muyeon.domain.model.menu.AppMenuItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    // ★ 2026-11-16 무용수 레슨 제공 — 명부(LessonProviders)에 무용수가 있을 때만 강사와 같은 레슨 메뉴.
    @After fun resetProviders() = LessonProviders.reset()

    private val str: (Int) -> String = { it.toString() }
    private fun build(type: String, server: List<AppMenuItem>? = null) = MyMenuBuilder.build(type, server, null, str).flatten()
    private val dancerServer = listOf(
        AppMenuItem("my.1", "/myPostings", "filled-note", "내 공고", bridge = "myJobPostings", groupNo = 1, order = 0),
        AppMenuItem("my.2", "/lessonHub", "filled-note", "개인레슨 관리", bridge = MyMenuBuilder.B_LESSON_HUB, groupNo = 1, order = 1),
        AppMenuItem("my.3", "/myQuotes", "filled-note", "받은 견적", bridge = MyMenuBuilder.B_MY_QUOTES, groupNo = 1, order = 2),
        AppMenuItem("my.4", "/availableQuotes", "filled-note", "견적 찾기", bridge = "quoteBrowse", groupNo = 1, order = 3),
        AppMenuItem("my.5", "/lessonSlots", "filled-note", "예약 시간", bridge = "lessonSlotManage", groupNo = 1, order = 4),
        AppMenuItem("my.6", "/faq", "faq", "FAQ", groupNo = 2, order = 0),
    )

    @Test fun dancerDefaultMenuHasNoLessonHubWhenOff() {
        assertFalse(build("DANCER").any { it.bridge == MyMenuBuilder.B_LESSON_HUB })
    }

    @Test fun dancerDefaultMenuGetsLessonHubWhenProvider() {
        LessonProviders.update(listOf("TEACHER", "ACADEMY", "DANCER"))
        val items = build("DANCER")
        assertEquals(1, items.count { it.bridge == MyMenuBuilder.B_LESSON_HUB })
        // 강사와 같은 제목('개인레슨 관리')
        assertEquals(com.muyeon.app.R.string.my_menu_lesson_hub.toString(), items.first { it.bridge == MyMenuBuilder.B_LESSON_HUB }.title)
        // 무용수 고유 메뉴(이력서·내 공고)는 그대로
        assertTrue(items.any { it.bridge == "dancerProfile" })
        assertTrue(items.any { it.bridge == MyMenuBuilder.B_JOB_POSTINGS })
    }

    @Test fun dancerServerMenuBlocksLessonItemsWhenOff() {
        val items = build("DANCER", dancerServer)
        val bridges = items.mapNotNull { it.bridge }
        assertFalse(MyMenuBuilder.B_LESSON_HUB in bridges)
        // ★ 2026-11-16 고객 쪽 '내 레슨 요청'은 남긴다(유형과 상관없이 레슨을 요청할 수 있다).
        assertTrue(MyMenuBuilder.B_MY_QUOTES in bridges)
        assertFalse("lessonSlotManage" in bridges)
        assertFalse("quoteBrowse" in bridges)
    }

    @Test fun dancerServerMenuKeepsLessonItemsLikeTeacherWhenProvider() {
        LessonProviders.update(listOf("TEACHER", "ACADEMY", "DANCER"))
        val dancer = build("DANCER", dancerServer).mapNotNull { it.bridge }
        assertTrue(MyMenuBuilder.B_LESSON_HUB in dancer)
        assertTrue(MyMenuBuilder.B_MY_QUOTES in dancer)
        assertTrue("lessonSlotManage" in dancer)
        // 강사와 같이 '견적 찾기'는 레슨 관리 안에 있어 따로 두지 않는다.
        assertFalse("quoteBrowse" in dancer)
        val teacher = build("TEACHER", dancerServer).mapNotNull { it.bridge }
        // 레슨 메뉴는 강사와 같다(양도는 원래 강사·학원만이라 뺀다).
        val lesson = setOf(MyMenuBuilder.B_LESSON_HUB, MyMenuBuilder.B_MY_QUOTES, "lessonSlotManage", "quoteBrowse")
        assertEquals(teacher.filter { it in lesson }.toSet(), dancer.filter { it in lesson }.toSet())
    }

    @Test fun teacherMenuUnchangedByDancerSwitch() {
        val before = build("TEACHER")
        LessonProviders.update(listOf("TEACHER", "ACADEMY", "DANCER"))
        assertEquals(before, build("TEACHER"))
    }
}

/** ★ 2026-11-16 고객 쪽(레슨 요청)은 유형으로 막지 않는다 — 서버가 넣은 '내 레슨 요청'을 어떤 유형에서도 남긴다. */
class MyMenuCustomerSideTest {
    @After fun resetProviders() = LessonProviders.reset()

    private val str: (Int) -> String = { it.toString() }
    private val server = listOf(
        AppMenuItem("my.0", "/myQuotes", "filled-note", "내 레슨 요청", bridge = MyMenuBuilder.B_MY_QUOTES, groupNo = 0, order = 0),
        AppMenuItem("my.1", "/faq", "faq", "FAQ", groupNo = 1, order = 0),
    )

    @Test fun keepsServerMyQuotesForEveryType() {
        for (t in listOf("GENERAL", "TEACHER", "ACADEMY", "DANCER", "TEAM")) {
            val bridges = MyMenuBuilder.build(t, server, null, str).flatten().mapNotNull { it.bridge }
            assertTrue("$t 에서 내 레슨 요청이 빠졌어요", MyMenuBuilder.B_MY_QUOTES in bridges)
        }
    }
}
