package com.muyeon.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** GET /me/roles 본문 읽기 — 보유(held)·심사 중(roles[].status=PENDING)·지금 유형. */
class RolesSnapshotTest {
    @Test fun `보유와 심사 중을 나눠 읽는다`() {
        val json = """{"held":["TEACHER"],"roles":[{"role":"TEACHER","status":"APPROVED"},{"role":"ACADEMY","status":"PENDING"}],"activeType":"GENERAL"}"""
        assertEquals(RolesSnapshot("GENERAL", listOf("TEACHER"), listOf("ACADEMY")), RolesSnapshot.parse(json))
    }

    @Test fun `활성유형이 없거나 취미면 일반`() {
        assertEquals("GENERAL", RolesSnapshot.parse("""{"held":[],"activeType":null}""")?.activeType)
        assertEquals("GENERAL", RolesSnapshot.parse("""{"held":[],"activeType":"HOBBY"}""")?.activeType)
    }

    @Test fun `본문이 없거나 깨지면 null`() {
        assertNull(RolesSnapshot.parse(null))
        assertNull(RolesSnapshot.parse("not json"))
    }
}
