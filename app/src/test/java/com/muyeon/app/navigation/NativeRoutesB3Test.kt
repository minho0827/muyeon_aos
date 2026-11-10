package com.muyeon.app.navigation

import com.muyeon.app.navigation.NativeRoutes.Target
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 묶음 3 — 작성·수정 주소가 앱 화면(로그인 필요)으로 가는지(iOS NativeRoutes+B3 와 같은 표). */
class NativeRoutesB3Test {
    private fun r(path: String) = NativeRoutes.resolve(path)
    private fun route(route: String) = Target.Route(route, auth = true)

    @Test fun subEdit() {
        assertEquals(route("subs/12/edit"), r("/subs/12/edit"))
        assertEquals(route("subs/12/edit"), r("/subs/12/edit/"))
        assertEquals(Target.Route("subs/12"), r("/subs/12"))
        assertNull(r("/subs/abc/edit"))
    }

    @Test fun communityWrite() {
        assertEquals(route("community/write"), r("/community/write"))
        assertEquals(route("community/write?edit=5"), r("/community/post/5/edit"))
        // 예전 iOS 글 수정 주소
        assertEquals(route("community/write?edit=5"), r("/community/write?id=5"))
        assertEquals(route("community/write"), r("/community/write?id=x"))
        assertEquals(Target.Route("community/post/5"), r("/community/post/5"))
    }

    @Test fun castingEditIsNative() {
        assertEquals(Target.Action("openCastingStage", mapOf("castingId" to "3", "screen" to "edit")), r("/casting/3/edit"))
    }
}
