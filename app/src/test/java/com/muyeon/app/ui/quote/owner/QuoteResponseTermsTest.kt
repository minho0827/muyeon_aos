package com.muyeon.app.ui.quote.owner

import com.muyeon.app.ui.quote.SentQuoteItem
import com.muyeon.app.ui.quote.respond.MyQuoteResponse
import com.muyeon.app.ui.quote.respond.QuoteLessonTermsInput
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-27 보낸 제안의 회당 수업 길이·장소 — 읽기 · 고치기 PATCH 본문. */
class QuoteResponseTermsTest {
    private val sent = QuoteResponseDraft(50000, "PER_SESSION", null, "안녕하세요", durationMin = 90, place = "강남 스튜디오")

    @Test fun sentList_and_detail_parseTerms() {
        val o = JSONObject("""{"id":3,"quoteId":12,"status":"SENT","durationMin":90,"place":"강남 스튜디오",
            "placeAddress":null,"placeLat":37.5,"placeLng":127.0,"placeOwner":"TEACHER"}""")
        val item = SentQuoteItem.from(o)
        assertEquals(90, item.terms.durationMin)
        assertEquals("강남 스튜디오", item.terms.place)
        assertEquals("TEACHER", item.terms.placeOwner)
        assertEquals(127.0, item.terms.placeLng!!, 0.0)
        val mine = MyQuoteResponse.from(o)
        assertEquals(90, mine.terms.durationMin)
        val draft = QuoteResponseDraft.of(50000, "PER_SESSION", "NONE", 0, "m", item.terms)
        assertEquals(90, draft.durationMin)
        assertEquals("강남 스튜디오", draft.place)
    }

    @Test fun oldServer_hasEmptyTerms() {
        val item = SentQuoteItem.from(JSONObject("""{"id":3,"quoteId":12}"""))
        assertNull(item.terms.durationMin)
        assertNull(QuoteResponseUpdate.from(JSONObject("""{"id":3,"status":"SENT"}""")).terms)
    }

    @Test fun patch_onlyChangedTerms() {
        val p = sent.copy(durationMin = 60).patchFrom(sent)!!
        assertEquals(60, p.getInt("durationMin"))
        assertFalse(p.has("place"))
        assertFalse(p.has("message"))
        assertNull(sent.copy(place = " 강남 스튜디오 ").patchFrom(sent))
    }

    @Test fun patch_clearsWithNull() {
        val p = sent.copy(durationMin = null, place = null).patchFrom(sent)!!
        assertTrue(p.isNull("durationMin"))
        assertTrue(p.isNull("place"))
    }

    @Test fun durationRule_sameForSendAndEdit() {
        assertTrue(QuoteLessonTermsInput.durationRequired(academyRequest = false))
        assertFalse(QuoteLessonTermsInput.durationRequired(academyRequest = true))
        assertFalse(QuoteLessonTermsInput(null).putInto(JSONObject()).has("durationMin"))
    }
}
