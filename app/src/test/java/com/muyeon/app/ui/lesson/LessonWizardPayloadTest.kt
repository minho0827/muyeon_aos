package com.muyeon.app.ui.lesson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 2026-10-23 레슨 개설 개편 — 위저드가 서버로 보내는 내용(유형·일정·반복)이 웹·iOS 와 같은지.
 *  서버 규칙: 날짜형(원데이·체험)은 날짜 항목만, 정규는 요일 항목만(섞이면 400), 정규 반복은 repeatWeeks(0=매주).
 */
class LessonWizardPayloadTest {

    @Test
    fun 원데이는_날짜_항목만_보내고_반복을_안_보낸다() {
        val d = LessonWizardDraft(
            title = "원데이", genre = "발레", lessonKind = "ONEDAY",
            dates = listOf("2026-11-02", "2026-10-26"), days = listOf(3),
            startTime = "14:10", duration = 75,
        )
        val p = d.toPayload()
        val sch = p.getJSONArray("schedule")
        assertEquals(2, sch.length())
        assertEquals("2026-10-26", sch.getJSONObject(0).getString("date"))
        assertEquals("15:25", sch.getJSONObject(0).getString("endTime"))
        assertFalse(sch.getJSONObject(0).has("dayOfWeek"))
        assertFalse(p.has("repeatWeeks"))
        assertFalse(p.has("isExperience"))
        assertEquals("ONEDAY", p.getString("lessonKind"))
        assertEquals("10/26(월)·11/2(월) 14:10~15:25", p.getString("scheduleText"))
    }

    @Test
    fun 정규는_요일_항목과_반복_주수를_보낸다() {
        val d = LessonWizardDraft(title = "정규", genre = "발레", lessonKind = "REGULAR", days = listOf(6, 1), repeatWeeks = 4)
        val p = d.toPayload()
        val sch = p.getJSONArray("schedule")
        assertEquals(1, sch.getJSONObject(0).getInt("dayOfWeek")) // 월~일 순
        assertEquals(6, sch.getJSONObject(1).getInt("dayOfWeek"))
        assertEquals("20:00", sch.getJSONObject(0).getString("endTime"))
        assertEquals(4, p.getInt("repeatWeeks"))
        assertEquals("매주 월·토 19:00~20:00", p.getString("scheduleText"))
    }

    @Test
    fun 예전_종료일_유지면_반복_주수를_안_보낸다() {
        val d = LessonWizardDraft(lessonKind = "REGULAR", days = listOf(3), keptEndDate = "2026-12-31", keepEndDate = true)
        assertFalse(d.toPayload().has("repeatWeeks"))
        assertEquals("2026-12-31까지", d.repeatText())
    }

    @Test
    fun 체험은_학원_수업에만_보인다() {
        val teacher = LessonWizardDraft().kindOptions(isAcademy = false, isEdit = false).map { it.first }
        val academy = LessonWizardDraft().kindOptions(isAcademy = true, isEdit = false).map { it.first }
        val teacherAtAcademy = LessonWizardDraft(bylinePartyId = 7).kindOptions(isAcademy = false, isEdit = false).map { it.first }
        assertEquals(listOf("REGULAR", "ONEDAY"), teacher)
        assertEquals(listOf("REGULAR", "ONEDAY", "EXPERIENCE"), academy)
        assertTrue("EXPERIENCE" in teacherAtAcademy)
    }

    @Test
    fun 가격_기준_계산식은_서버와_같다() {
        assertEquals("1인당 30,000원 × 3명 = 90,000원", LessonOptions.priceFormula(30_000, "PER_PERSON", 3))
        assertEquals("예약 1건당 30,000원 (3명이 함께 와도 30,000원)", LessonOptions.priceFormula(30_000, "PER_BOOKING", 3))
        assertEquals(90_000, LessonOptions.totalPrice(30_000, "PER_PERSON", 3))
        assertEquals(30_000, LessonOptions.totalPrice(30_000, "PER_BOOKING", 3))
    }

    @Test
    fun 휠_선택지_범위() {
        assertEquals("06:00", LessonOptions.wheelStartTimes.first())
        assertEquals("23:50", LessonOptions.wheelStartTimes.last())
        assertEquals(40, LessonOptions.wheelDurations.first())
        assertEquals(240, LessonOptions.wheelDurations.last())
        assertEquals(20, LessonOptions.wheelCapacities.last())
        assertEquals(500_000, LessonOptions.wheelPrices.last())
        assertEquals(5_000, LessonOptions.wheelPrices[1])
    }
}
