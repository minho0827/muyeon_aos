package com.muyeon.app.ui.quote

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-27 403 LESSON_REQUEST_LIMIT 의 data.audience == "GENERAL" 일 때만 일반 회원 한도(공고 버튼 숨김). */
class GeneralLessonLimitTest {
    private fun ex(data: JSONObject?) = ApiMessageException("limit", code = LESSON_REQUEST_LIMIT_CODE, data = data)

    @Test fun `audience GENERAL 이면 true`() {
        assertTrue(ex(JSONObject().put("audience", "GENERAL").put("limit", 2)).generalLessonLimit)
    }

    @Test fun `audience 없으면 false`() {
        assertFalse(ex(JSONObject().put("tier", "FREE").put("limit", 3)).generalLessonLimit)
    }

    @Test fun `audience 가 다른 값이면 false`() {
        assertFalse(ex(JSONObject().put("audience", "ACADEMY")).generalLessonLimit)
        assertFalse(ex(JSONObject().put("audience", "general")).generalLessonLimit)
        assertFalse(ex(JSONObject().put("audience", JSONObject.NULL)).generalLessonLimit)
    }

    @Test fun `data 가 null 이면 false`() {
        assertFalse(ex(null).generalLessonLimit)
    }
}
