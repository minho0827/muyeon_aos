package com.muyeon.app.ui.review

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** 내 후기 수정 — 서버 myReview 로 작성 화면 채우기. */
class ReviewEditPrefillTest {
    @Test fun parsesMyReview() {
        val l = ReviewList.from(JSONObject("""{"items":[],"myReview":{"rating":4,"ratings":{"teaching":5,"communication":4,"facility":3},
            "revisit":1,"content":"좋아요","lessonType":"PRIVATE","purpose":"HOBBY","tags":["KIND"],"images":[]}}"""))
        val m = l.myReview!!
        assertEquals(true, m.revisit)
        assertEquals(mapOf("teaching" to 5, "communication" to 4, "facility" to 3), reviewInitialRatings(m))
        assertEquals(listOf("KIND"), m.tags)
    }

    @Test fun oldSingleRatingFillsAllAxes() {
        val m = ReviewList.MyReview(emptyMap(), 3, false, null, null, null, null, emptyList(), emptyList())
        assertEquals(mapOf("teaching" to 3, "communication" to 3, "facility" to 3), reviewInitialRatings(m))
    }
}
