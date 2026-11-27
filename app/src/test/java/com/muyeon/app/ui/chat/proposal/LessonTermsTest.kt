package com.muyeon.app.ui.chat.proposal

import com.muyeon.app.ui.quote.respond.QuoteLessonTermsInput
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonTermsTest {
    @Test fun parse_teacherPlace_isFixedForMember() {
        val t = LessonTerms.from(JSONObject("""{"durationMin":90,"place":" 강남 스튜디오 ","placeAddress":null,"placeLat":37.5,"placeLng":null,"placeOwner":"TEACHER"}"""))!!
        assertEquals(90, t.memberFixedDuration)
        assertEquals("강남 스튜디오", t.memberFixedPlace)
        assertEquals(37.5, t.placeLat!!, 0.0)
        assertNull(t.placeLng)
    }

    @Test fun memberOwnedPlace_orOldQuote_isNotFixed() {
        val member = LessonTerms.from(JSONObject("""{"durationMin":60,"place":"우리집","placeOwner":"MEMBER"}"""))!!
        assertNull(member.memberFixedPlace)
        val old = LessonTerms.from(JSONObject("""{"durationMin":null,"place":null,"placeOwner":"TEACHER"}"""))!!
        assertNull(old.memberFixedDuration)
        assertNull(old.memberFixedPlace)
        assertNull(LessonTerms.from(null))
    }

    @Test fun durationOptions_addsQuoteValueOutsideList() {
        assertEquals(listOf(30, 45, 60, 90, 120), durationOptions(45))
        assertEquals(listOf(30, 60, 90, 120), durationOptions(60))
        assertEquals(listOf(30, 60, 90, 120), durationOptions(null))
    }

    @Test fun quoteTerms_body() {
        val b = QuoteLessonTermsInput(90, "  연습실  ").putInto(JSONObject())
        assertEquals(90, b.getInt("durationMin"))
        assertEquals("연습실", b.getString("place"))
        assertFalse(QuoteLessonTermsInput(60, " ").putInto(JSONObject()).has("place"))
        assertTrue(QuoteLessonTermsInput.asksPlace("TEACHER"))
        assertFalse(QuoteLessonTermsInput.asksPlace("MEMBER"))
        assertFalse(QuoteLessonTermsInput.asksPlace(null))
    }

    @Test fun counter_doubleSubmit_sendsOnce_andFailureKeepsMessage() = runBlocking {
        var calls = 0
        val gate = CompletableDeferred<Result<Unit>>()
        val s = ProposalCounterState(1_000L, "fallback") { _, _ -> calls++; gate.await() }
        val first = async { s.submit() }
        yield()
        assertTrue(s.sending)
        assertFalse(s.submit())          // 보내는 중 두 번째 누름은 무시
        gate.complete(Result.failure(IllegalStateException("이미 끝난 제안이에요.")))
        assertFalse(first.await())
        assertEquals(1, calls)
        assertEquals("이미 끝난 제안이에요.", s.errorText)
        assertFalse(s.sending)
    }
}
