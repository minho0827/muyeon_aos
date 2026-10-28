package com.muyeon.app.ui.jobposting

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** ★ 2026-10-28 구인 공고 상세 페이지 이미지(detailType·detailImages) 저장·불러오기. */
class JobFormDetailImagesTest {
    @Test fun `신규 - 이미지가 있으면 detailType 과 함께 보낸다`() {
        val o = JobForm(title = "t", detailType = "IMAGE", detailImages = listOf("a.jpg")).toJson()
        assertEquals("IMAGE", o.getString("detailType"))
        assertEquals("a.jpg", o.getJSONArray("detailImages").getString(0))
    }

    @Test fun `신규 - 이미지가 없으면 detailImages 를 빼고 보낸다`() {
        val o = JobForm(title = "t").toJson()
        assertFalse(o.has("detailImages"))
        assertFalse(o.has("detailType"))
    }

    @Test fun `수정 - 다 지웠으면 빈 목록을 보내 서버에서도 지운다`() {
        val o = JobForm(title = "t", detailType = "TEXT", detailImages = null).toJson(isEdit = true)
        assertEquals(0, o.getJSONArray("detailImages").length())
        assertEquals("TEXT", o.getString("detailType"))
    }

    @Test fun `불러오기 - 서버 응답의 detailType·detailImages 를 읽는다`() {
        val f = JobForm.from(JSONObject().put("title", "t").put("detailType", "IMAGE")
            .put("detailImages", JSONArray(listOf("x.jpg", "y.jpg"))))
        assertEquals("IMAGE", f.detailType)
        assertEquals(listOf("x.jpg", "y.jpg"), f.detailImages)
    }
}
