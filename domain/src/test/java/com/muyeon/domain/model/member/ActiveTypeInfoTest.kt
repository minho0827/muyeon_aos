package com.muyeon.domain.model.member

import com.muyeon.domain.usecase.auth.CompleteRoleOnboardingUseCase
import com.muyeon.domain.usecase.dancer.DancersUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveTypeInfoTest {
    @Test fun `짧은 이름 — 일반·강사·무용수·원장·공연기획`() {
        assertEquals(listOf("일반", "강사", "무용수", "원장", "공연기획"),
            listOf("GENERAL", "TEACHER", "DANCER", "ACADEMY", "TEAM").map { ActiveTypeInfos.of(it).short })
        // 관문·글쓰기 메뉴 이름도 같은 표를 읽는다.
        assertEquals("공연기획", FeatureRoles.label("TEAM"))
        assertEquals("일반", FeatureRoles.label("HOBBY"))
    }

    @Test fun `긴 이름 — 학원·원장 · 공연팀·기획자, MY 머리말은 일반회원`() {
        assertEquals("학원·원장", ActiveTypeInfos.of("academy").long)
        assertEquals("공연팀·기획자", ActiveTypeInfos.of("TEAM").long)
        assertEquals("일반", ActiveTypeInfos.of(null).long)
        assertEquals("일반회원", ActiveTypeInfos.of("HOBBY").title)
        assertEquals("공간 보유자", ActiveTypeInfos.find("SPACE")?.long)
        assertNull(ActiveTypeInfos.find("HOBBY"))
    }

    @Test fun `인증 방식 — 본인 인증은 강사·무용수, 서류는 학원·공연팀(공간 포함)`() {
        assertEquals(setOf("TEACHER", "DANCER"), ActiveTypeInfos.IDENTITY_ROLES)
        assertEquals(setOf("ACADEMY", "TEAM"), ActiveTypeInfos.DOCUMENT_ROLES)
        assertEquals(setOf("TEACHER", "DANCER"), CompleteRoleOnboardingUseCase.IDENTITY_ROLES)
        assertEquals(setOf("ACADEMY", "TEAM"), CompleteRoleOnboardingUseCase.VERIFICATION_REQUIRED_ROLES)
        assertTrue(ActiveTypeInfos.needsDocuments("SPACE"))
        assertTrue(ActiveTypeInfos.needsDocuments("academy"))
        assertFalse(ActiveTypeInfos.needsDocuments("TEACHER"))
        assertFalse(ActiveTypeInfos.needsDocuments("HOBBY"))
    }

    @Test fun `아이콘 열쇠·서류 안내`() {
        assertEquals(listOf("person", "school", "dancer", "academy", "team"),
            listOf("GENERAL", "TEACHER", "DANCER", "ACADEMY", "TEAM").map { ActiveTypeInfos.of(it).iconKey })
        assertEquals("사업자등록증 또는 임대차계약서를 첨부해 주세요.", ActiveTypeInfos.SPACE.docGuide)
    }

    @Test fun `새 기능 필요 유형 — 캐스팅 지원·제안·채용 채팅`() {
        assertEquals(listOf("DANCER"), FeatureRoles.required(TypeFeature.CASTING_APPLY))
        assertEquals(listOf("TEAM"), FeatureRoles.required(TypeFeature.CASTING_OFFER))
        assertEquals(listOf("ACADEMY", "TEAM", "TEACHER"), FeatureRoles.required(TypeFeature.RECRUIT_CHAT))
        // 무용수 상세 열람 유형과 하나로 묶였다.
        assertEquals(setOf("TEAM", "ACADEMY", "TEACHER"), DancersUseCase.READ_TYPES)
        assertFalse(FeatureRoles.allows(TypeFeature.CASTING_APPLY, "TEACHER"))
        val d = TypeGateRules.decide(TypeFeature.CASTING_APPLY, true, "TEACHER", held = listOf("TEACHER", "DANCER")) as TypeGateDecision.Switch
        assertEquals("현재 회원님은 강사 유형이에요. 무용수 유형으로 전환하고 지원할까요?", TypeGateRules.switchMessage(d, TypeFeature.CASTING_APPLY))
    }
}
