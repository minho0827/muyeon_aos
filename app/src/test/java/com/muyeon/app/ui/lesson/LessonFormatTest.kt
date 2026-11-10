package com.muyeon.app.ui.lesson

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2026-11-10 온라인 레슨 = formats 값 "온라인"(isOnline 칸은 서버가 버린다). iOS LessonFormatTests 와 같은 경우. */
class LessonFormatTest {

    private fun formats(p: JSONObject) = p.getJSONArray("formats").let { a -> (0 until a.length()).map { a.getString(it) } }

    @Test
    fun 온라인이면_formats_에_온라인을_넣고_장소명은_온라인_레슨() {
        val p = LessonWizardDraft(isOnline = true, formats = listOf("private")).toPayload()
        assertEquals(listOf("private", "온라인"), formats(p))
        assertEquals("온라인 레슨", p.getString("place"))
        assertFalse(p.has("isOnline"))
    }

    @Test
    fun 온라인을_끄면_온라인만_빼고_다른_값은_둔다() {
        val p = LessonWizardDraft(isOnline = false, formats = listOf("온라인", "group"), place = "무용연 스튜디오").toPayload()
        assertEquals(listOf("group"), formats(p))
        assertEquals("무용연 스튜디오", p.getString("place"))
    }

    @Test
    fun 온라인을_두_번_넣지_않는다() {
        assertEquals(listOf("private", "온라인"), LessonFormat.applyOnline(listOf("온라인", "private"), true))
        assertEquals("줌", LessonFormat.payloadPlace("줌", true))
    }

    @Test
    fun 불러올_때는_formats_온라인이나_장소명_온라인_레슨이면_온라인() {
        assertTrue(LessonFormat.isOnline(listOf("온라인"), "스튜디오"))
        assertTrue(LessonFormat.isOnline(null, "온라인 레슨"))
        assertFalse(LessonFormat.isOnline(listOf("private"), "스튜디오"))
        assertFalse(LessonFormat.isOnline(null, null))
    }

    @Test
    fun 수정_프리필이_formats_를_들고_와서_온라인을_판정한다() {
        val d = LessonProductDetail.from(JSONObject("""{"place":"스튜디오","address":"서울","formats":["group","온라인"]}""")).toDraft()
        assertTrue(d.isOnline)
        assertEquals(listOf("group", "온라인"), d.formats)
        val offline = LessonProductDetail.from(JSONObject("""{"place":"스튜디오","address":"","formats":["private"]}""")).toDraft()
        assertFalse(offline.isOnline) // 주소가 비어도 formats·장소명이 아니면 오프라인
    }

    @Test
    fun 라벨은_온라인을_포함하고_모르는_값은_그대로() {
        assertEquals("개인 레슨, 온라인, etc", LessonFormat.labels(listOf("private", "온라인", "etc")))
        assertNull(LessonFormat.labels(emptyList()))
        assertTrue(LessonFormat.options.any { it.first == "온라인" })
        assertFalse(LessonFormat.manualOptions.any { it.first == "온라인" })
    }
}
