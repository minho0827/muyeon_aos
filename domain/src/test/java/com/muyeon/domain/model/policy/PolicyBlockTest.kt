package com.muyeon.domain.model.policy

import org.junit.Assert.assertEquals
import org.junit.Test

/** (b4) 웹 Policy.js renderMd 와 같은 규칙. */
class PolicyBlockTest {
    @Test fun `제목·소제목·목록·문단·표`() {
        val md = """
            ## 제1조
            본문
            - 하나
            - 둘
            ### 소제목
            | 항목 | 내용 |
            |---|---|
            | 이름 | 홍길동 |

            끝
        """.trimIndent()
        assertEquals(
            listOf(
                PolicyBlock.Heading("제1조"), PolicyBlock.Paragraph("본문"), PolicyBlock.Bullets(listOf("하나", "둘")),
                PolicyBlock.SubHeading("소제목"), PolicyBlock.Bullets(listOf("이름 · 홍길동")), PolicyBlock.Paragraph("끝"),
            ),
            PolicyBlock.parse(md),
        )
    }
}
