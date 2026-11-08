package com.muyeon.domain.usecase.academy

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.academy.AcademyLinkedLesson
import com.muyeon.domain.model.academy.AcademyOwner
import com.muyeon.domain.model.academy.AcademyProfileForm
import com.muyeon.domain.repository.AcademyProfileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-08 묶음 2 — 웹 AcademyProfile save 규칙(키워드 8개·학원명 다듬기·필수 순서). */
class AcademyProfileUseCaseTest {
    private class Fake : AcademyProfileRepository {
        var saved: AcademyProfileForm? = null
        override suspend fun load() = ApiResult.Success(AcademyProfileForm())
        override suspend fun save(form: AcademyProfileForm): ApiResult<Unit> { saved = form; return ApiResult.Success(Unit) }
        override suspend fun owner() = ApiResult.Success(AcademyOwner(1, "홍길동"))
        override suspend fun lessons(academyId: Long) = ApiResult.Success(emptyList<AcademyLinkedLesson>())
    }
    private val full = AcademyProfileForm(academyName = "  루체 발레  ", academyImages = listOf("/a.jpg"), academyIntro = "소개", wantFields = listOf("BASIC"))

    @Test fun `키워드는 쉼표로 나눠 다듬고 최대 8개`() {
        assertEquals(listOf("a", "b"), AcademyProfileUseCase.toKeywords(" a, ,b ,"))
        assertEquals(8, AcademyProfileUseCase.toKeywords((1..12).joinToString(",")).size)
    }

    @Test fun `필수 확인 순서 — 학원명 · 사진 · 소개 · 장르 또는 분야`() {
        val uc = AcademyProfileUseCase(Fake())
        assertEquals(AcademyProfileUseCase.Missing.NAME, uc.missing(AcademyProfileForm()))
        assertEquals(AcademyProfileUseCase.Missing.IMAGES, uc.missing(AcademyProfileForm(academyName = "x")))
        assertEquals(AcademyProfileUseCase.Missing.INTRO, uc.missing(full.copy(academyIntro = " ")))
        assertEquals(AcademyProfileUseCase.Missing.WANT, uc.missing(full.copy(wantFields = emptyList())))
        assertNull(uc.missing(full))
        assertNull(uc.missing(full.copy(wantFields = emptyList(), wantGenres = listOf("발레"))))
    }

    @Test fun `저장 — 학원명 다듬고 키워드 글을 목록으로`() = runBlocking {
        val repo = Fake()
        val r = AcademyProfileUseCase(repo).save(full, "유아 전문, 입시")
        assertTrue(r is ApiResult.Success)
        assertEquals("루체 발레", repo.saved!!.academyName)
        assertEquals(listOf("유아 전문", "입시"), repo.saved!!.academyKeywords)
    }

    @Test fun `빠진 칸이 있으면 서버로 보내지 않는다`() = runBlocking {
        val repo = Fake()
        val r = AcademyProfileUseCase(repo).save(AcademyProfileForm(), "")
        assertTrue(r is ApiResult.Error)
        assertNull(repo.saved)
    }
}
