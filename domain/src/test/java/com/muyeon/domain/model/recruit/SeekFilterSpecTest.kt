package com.muyeon.domain.model.recruit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekFilterSpecTest {
    private val all = SeekFilter(types = setOf("DANCER"), fields = setOf("발레"), targets = setOf("ADULT"), careers = setOf("Y3"))

    @Test fun `강사는 유형·분야, 그 밖은 분야·대상·경력`() {
        assertEquals(listOf(SeekFilterField.TYPES, SeekFilterField.FIELDS), SeekFilterSpec.forType("teacher").fields)
        assertTrue(SeekFilterSpec.forType("TEACHER").isTeacherView)
        assertEquals(listOf(SeekFilterField.FIELDS, SeekFilterField.TARGETS, SeekFilterField.CAREERS), SeekFilterSpec.forType("ACADEMY").fields)
        assertFalse(SeekFilterSpec.forType(null).isTeacherView)
    }

    @Test fun `유형이 바뀌면 안 보이는 칸 값은 지운다`() {
        assertEquals(SeekFilter(types = setOf("DANCER"), fields = setOf("발레")), SeekFilterSpec.forType("TEACHER").clean(all))
        assertEquals(SeekFilter(fields = setOf("발레"), targets = setOf("ADULT"), careers = setOf("Y3")), SeekFilterSpec.forType("GENERAL").clean(all))
        assertEquals(2, SeekFilterSpec.forType("TEACHER").count(all))
        assertEquals(3, SeekFilterSpec.forType("TEAM").count(all))
    }

    @Test fun `시트 값과 필터 오가기`() {
        val spec = SeekFilterSpec.forType("TEACHER")
        val m = spec.selection(all)
        assertEquals(setOf("DANCER"), m["types"])
        assertEquals(SeekFilter(types = setOf("DANCER"), fields = setOf("발레")), spec.fromSelection(m))
    }
}
