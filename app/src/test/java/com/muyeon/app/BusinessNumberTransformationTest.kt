package com.muyeon.app

import androidx.compose.ui.text.AnnotatedString
import com.muyeon.app.ui.onboarding.BusinessNumberTransformation
import org.junit.Assert.assertEquals
import org.junit.Test

/** 2026-11-09 사업자등록번호 입력 — 숫자만 담고 하이픈은 보이기만, 커서는 숫자 기준으로 맞게 옮긴다. */
class BusinessNumberTransformationTest {
    private fun t(s: String) = BusinessNumberTransformation.filter(AnnotatedString(s))

    @Test fun formats() {
        assertEquals("123", t("123").text.text)
        assertEquals("123-4", t("1234").text.text)
        assertEquals("123-45-67890", t("1234567890").text.text)
    }

    @Test fun cursorAtEndStaysAtEndWhileTyping() {
        // 한 자리씩 칠 때마다 커서(끝)가 보이는 글자의 끝에 있어야 다음 숫자가 순서대로 붙는다.
        val all = "1234567890"
        for (n in 1..10) {
            val s = all.take(n)
            val tr = t(s)
            assertEquals("n=$n", tr.text.length, tr.offsetMapping.originalToTransformed(n))
            assertEquals("n=$n back", n, tr.offsetMapping.transformedToOriginal(tr.text.length))
        }
    }

    @Test fun roundTripEveryPosition() {
        val tr = t("1234567890")
        for (o in 0..10) assertEquals(o, tr.offsetMapping.transformedToOriginal(tr.offsetMapping.originalToTransformed(o)))
    }
}
