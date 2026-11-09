package com.muyeon.app.ui.castingstage

import com.muyeon.app.ui.castingstage.model.StageCategory
import com.muyeon.app.ui.castingstage.model.StageCategory.Companion.KIND_CASTING
import com.muyeon.app.ui.castingstage.model.StageCategory.Companion.KIND_NEWS
import com.muyeon.app.ui.castingstage.model.StageNewsItem
import org.junit.Assert.assertEquals
import org.junit.Test

/** ★ 11-09 서버 분류 목록 — 기본 목록·순서·보일 소식 칸. */
class StageCategoryTest {
    private fun c(code: String, label: String = code, kind: String = KIND_NEWS) = StageCategory(code, label, kind)
    private fun n(id: Int) = StageNewsItem(id, "t$id", "", null, "https://x", null, null, null)

    @Test fun `실패·빈 목록이면 기본 목록(국내소식·콩쿠르 없음)`() {
        val codes = listOf("CASTING", "PERFORMANCE", "ADMISSION", "SCHOOL")
        assertEquals(codes, StageCategory.resolve(null).map { it.code })
        assertEquals(codes, StageCategory.resolve(emptyList()).map { it.code })
        assertEquals("캐스팅 공고", StageCategory.resolve(null).first().label)
    }

    @Test fun `서버 순서·이름 그대로, 이상한 줄은 뺀다`() {
        val server = listOf(
            c("SCHOOL", "학교"), c("CASTING", "캐스팅", KIND_CASTING), c("", "빈 코드"), c("X", "모르는 종류", "ETC"),
            c("SCHOOL", "겹침"), c("NEW1", ""),
        )
        val r = StageCategory.resolve(server)
        assertEquals(listOf("SCHOOL", "CASTING", "NEW1"), r.map { it.code })
        assertEquals(listOf("학교", "캐스팅", "NEW1"), r.map { it.label })
    }

    @Test fun `소식 칸은 목록에 있고 글이 있을 때만, 목록 순서로`() {
        val cats = listOf(c("CASTING", kind = KIND_CASTING), c("SCHOOL"), c("PERFORMANCE"), c("EMPTY"))
        val sections = mapOf(
            "PERFORMANCE" to listOf(n(1)), "SCHOOL" to listOf(n(2)), "EMPTY" to emptyList(),
            "DOMESTIC" to listOf(n(3)), // 숨긴 칸 — 글이 있어도 안 보인다
        )
        assertEquals(listOf("SCHOOL", "PERFORMANCE"), StageCategory.newsSections(cats, sections).map { it.first.code })
        assertEquals(emptyList<String>(), StageCategory.newsSections(cats, null).map { it.first.code })
    }
}
