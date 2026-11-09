package com.muyeon.domain.common

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 계정 바뀜 신호 — 로그아웃·다른 계정 로그인만 바뀜, 같은 회원 토큰 갱신은 아님. */
class SessionEventsTest {
    private val calls = mutableListOf<Pair<String?, String?>>()

    @Before fun setUp() {
        SessionEvents.resetForTest()
        SessionEvents.register { prev, cur -> calls += prev to cur }
    }

    @After fun tearDown() = SessionEvents.resetForTest()

    @Test fun 시작은_바뀜이_아니다() {
        SessionEvents.start("7")
        assertEquals(0, SessionEvents.state.value.epoch)
        assertTrue(SessionEvents.state.value.loggedIn)
        assertTrue(calls.isEmpty())
    }

    @Test fun 같은_회원_토큰_갱신은_무시() {
        SessionEvents.start("7")
        assertFalse(SessionEvents.onAccount("7"))
        assertEquals(0, SessionEvents.state.value.epoch)
        assertTrue(calls.isEmpty())
    }

    @Test fun 로그아웃은_앞_계정을_넘기고_번호를_올린다() {
        SessionEvents.start("7")
        assertTrue(SessionEvents.onAccount(null))
        assertEquals(listOf<Pair<String?, String?>>("7" to null), calls)
        assertEquals(1, SessionEvents.state.value.epoch)
        assertFalse(SessionEvents.state.value.loggedIn)
        // 두 번 지워도(여러 길이 겹침) 한 번만.
        assertFalse(SessionEvents.onAccount(null))
        assertEquals(1, calls.size)
    }

    @Test fun 비회원에서_로그인은_앞_계정이_없다() {
        assertTrue(SessionEvents.onAccount("9"))
        assertNull(calls.single().first)
        assertEquals("9", calls.single().second)
    }

    @Test fun 다른_계정으로_바로_바뀜() {
        SessionEvents.start("7")
        SessionEvents.onAccount("8")
        assertEquals(listOf<Pair<String?, String?>>("7" to "8"), calls)
        assertEquals("8", SessionEvents.state.value.userKey)
    }

    @Test fun 지우개가_실패해도_나머지는_돈다() {
        var ran = false
        SessionEvents.register { _, _ -> error("x") }
        SessionEvents.register { _, _ -> ran = true }
        SessionEvents.start("1")
        SessionEvents.onAccount(null)
        assertTrue(ran)
    }
}

/** 화면 기억값 키 분류 — 회원 것만 지우고 기기 화면값은 남긴다. */
class SessionKeysTest {
    private class MapStore : KeyValueStore {
        val m = mutableMapOf<String, String>()
        override fun getString(key: String) = m[key]
        override fun putString(key: String, value: String?) { if (value == null) m.remove(key) else m[key] = value }
    }

    @Test fun 회원키와_기기키는_겹치지_않는다() {
        assertTrue(SessionKeys.PER_USER.intersect(SessionKeys.DEVICE.toSet()).isEmpty())
        assertEquals(SessionKeys.PER_USER.size, SessionKeys.PER_USER.toSet().size)
        assertEquals(SessionKeys.DEVICE.size, SessionKeys.DEVICE.toSet().size)
    }

    @Test fun 미리_누른_찜은_회원것() = assertTrue("scrap.pending" in SessionKeys.PER_USER)

    @Test fun 공연칸_마지막본시각은_기기것() = assertTrue("tabUpdates.performance.lastSeen" in SessionKeys.DEVICE)

    @Test fun clearPerUser_는_회원키만_지운다() {
        val s = MapStore()
        (SessionKeys.PER_USER + SessionKeys.DEVICE).forEach { s.putString(it, "v") }
        SessionKeys.clearPerUser(s)
        SessionKeys.PER_USER.forEach { assertNull(s.getString(it)) }
        SessionKeys.DEVICE.forEach { assertEquals("v", s.getString(it)) }
    }
}
