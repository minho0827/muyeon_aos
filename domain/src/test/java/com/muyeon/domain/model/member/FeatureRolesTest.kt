package com.muyeon.domain.model.member

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureRolesTest {
    @After fun tearDown() { LessonProviders.onChanged = null; LessonProviders.reset() }

    @Test fun `기능별 필요 유형 — 서버 기준과 같다`() {
        assertEquals(listOf("ACADEMY", "TEAM"), FeatureRoles.required(TypeFeature.JOB_CREATE))
        assertEquals(listOf("TEACHER", "DANCER"), FeatureRoles.required(TypeFeature.SEEK_PROFILE))
        assertEquals(listOf("TEACHER", "ACADEMY", "DANCER", "TEAM"), FeatureRoles.required(TypeFeature.SUB_CREATE))
        assertEquals(listOf("TEAM"), FeatureRoles.required(TypeFeature.CASTING_CREATE))
        assertEquals(listOf("ACADEMY", "TEACHER", "DANCER"), FeatureRoles.required(TypeFeature.SPACE_CREATE))
        assertEquals(listOf("ACADEMY", "TEACHER"), FeatureRoles.required(TypeFeature.TRANSFER_CREATE))
    }

    @Test fun `레슨 제공 — 명부를 따른다`() {
        assertEquals(listOf("TEACHER", "ACADEMY"), FeatureRoles.required(TypeFeature.LESSON_PROVIDE))
        LessonProviders.update(listOf("TEACHER", "ACADEMY", "DANCER"))
        assertEquals(listOf("TEACHER", "ACADEMY", "DANCER"), FeatureRoles.required(TypeFeature.LESSON_PROVIDE))
        assertTrue(FeatureRoles.allows(TypeFeature.LESSON_PROVIDE, "dancer"))
    }

    @Test fun `allows — 대소문자·취미는 일반으로 본다`() {
        assertTrue(FeatureRoles.allows(TypeFeature.CASTING_CREATE, "team"))
        assertFalse(FeatureRoles.allows(TypeFeature.CASTING_CREATE, "HOBBY"))
        assertFalse(FeatureRoles.allows(TypeFeature.JOB_CREATE, null))
        assertEquals("강사·무용수", FeatureRoles.caption(TypeFeature.SEEK_PROFILE))
    }
}

class TypeGateRulesTest {
    private val f = TypeFeature.SUB_CREATE

    @Test fun `비회원 — 로그인`() =
        assertEquals(TypeGateDecision.Login, TypeGateRules.decide(f, loggedIn = false, activeType = "TEACHER"))

    @Test fun `지금 유형이 맞다 — 바로 진행`() =
        assertEquals(TypeGateDecision.Allowed, TypeGateRules.decide(f, true, "TEACHER", held = emptyList()))

    @Test fun `가진 유형으로 전환 — 필요 유형 순서로 고를 수 있게`() {
        val d = TypeGateRules.decide(f, true, "GENERAL", held = listOf("dancer", "TEACHER"))
        assertEquals(TypeGateDecision.Switch("GENERAL", listOf("TEACHER", "DANCER")), d)
        assertEquals("현재 회원님은 일반 유형이에요. 어느 유형으로 전환하고 작성할까요?", TypeGateRules.switchMessage(d as TypeGateDecision.Switch, f))
        val one = TypeGateRules.decide(TypeFeature.SEEK_PROFILE, true, "GENERAL", held = listOf("TEACHER")) as TypeGateDecision.Switch
        assertEquals("현재 회원님은 일반 유형이에요. 강사 유형으로 전환하고 작성할까요?", TypeGateRules.switchMessage(one, TypeFeature.SEEK_PROFILE))
        assertEquals("강사 유형으로 전환하고 작성", TypeGateRules.switchButton("TEACHER", TypeFeature.SEEK_PROFILE))
    }

    @Test fun `미보유 — 유형 추가 안내`() {
        val d = TypeGateRules.decide(TypeFeature.CASTING_CREATE, true, "GENERAL", held = listOf("TEACHER"))
        assertEquals(TypeGateDecision.AddType(listOf("TEAM")), d)
        assertEquals("공연기획 유형이 있어야 쓸 수 있어요. 유형을 추가할까요?", TypeGateRules.addTypeMessage(d as TypeGateDecision.AddType))
        val many = TypeGateRules.decide(TypeFeature.SEEK_PROFILE, true, "GENERAL") as TypeGateDecision.AddType
        assertEquals("강사·무용수 중 한 유형이 있어야 쓸 수 있어요. 유형을 추가할까요?", TypeGateRules.addTypeMessage(many))
    }

    @Test fun `인증 대기 — 심사 중 안내(가진 유형이 있으면 전환이 먼저)`() {
        val d = TypeGateRules.decide(TypeFeature.JOB_CREATE, true, "GENERAL", held = emptyList(), pending = listOf("academy"))
        assertEquals(TypeGateDecision.Pending(listOf("ACADEMY")), d)
        assertEquals("원장 인증을 심사하고 있어요. 승인되면 바로 이용할 수 있어요.", TypeGateRules.pendingMessage(d as TypeGateDecision.Pending))
        val sw = TypeGateRules.decide(TypeFeature.JOB_CREATE, true, "GENERAL", held = listOf("TEAM"), pending = listOf("ACADEMY"))
        assertEquals(TypeGateDecision.Switch("GENERAL", listOf("TEAM")), sw)
    }
}

class WriteMenuRulesTest {
    @Test fun `일반 회원 — 채용 4개 모두, 못 쓰는 항목엔 필요 유형`() {
        val items = WriteMenuRules.jobs(loggedIn = true, activeType = "GENERAL")
        assertEquals(listOf(TypeFeature.JOB_CREATE, TypeFeature.SEEK_PROFILE, TypeFeature.SUB_CREATE, TypeFeature.CASTING_CREATE), items.map { it.feature })
        assertTrue(items.none { it.usable })
        assertEquals(listOf("원장·공연기획", "강사·무용수", "강사·원장·무용수·공연기획", "공연기획"), items.map { it.caption })
        assertEquals("지금 일반 유형이에요", WriteMenuRules.header("GENERAL"))
    }

    @Test fun `강사 — 쓸 수 있는 항목은 캡션 없음`() {
        val items = WriteMenuRules.jobs(true, "TEACHER").associateBy { it.feature }
        assertTrue(items.getValue(TypeFeature.SEEK_PROFILE).usable)
        assertNull(items.getValue(TypeFeature.SUB_CREATE).caption)
        assertFalse(items.getValue(TypeFeature.JOB_CREATE).usable)
    }

    @Test fun `비회원 — 버튼 숨김(빈 목록)`() {
        assertTrue(WriteMenuRules.jobs(false, "GENERAL").isEmpty())
        assertTrue(WriteMenuRules.spaces(false, "GENERAL", transfersFirst = false).isEmpty())
    }

    @Test fun `공간 탭 — 일반 회원도 2개, 보고 있는 칸이 먼저`() {
        val a = WriteMenuRules.spaces(true, "GENERAL", transfersFirst = true)
        assertEquals(listOf(TypeFeature.TRANSFER_CREATE, TypeFeature.SPACE_CREATE), a.map { it.feature })
        assertEquals(listOf("원장·강사", "원장·강사·무용수"), a.map { it.caption })
    }
}

class MyTypeHintRulesTest {
    @Test fun `전문 유형을 가졌는데 일반 — 강사 원장 무용수 공연기획 순 첫째`() {
        assertEquals("ACADEMY", MyTypeHintRules.bubbleType(true, "GENERAL", listOf("GENERAL", "TEAM", "ACADEMY"), false))
        assertEquals("강사 유형도 있어요. MY를 두 번 누르면 바뀌어요", MyTypeHintRules.text("TEACHER"))
    }

    @Test fun `안 띄우는 경우 — 일반 아님·닫음·비회원·전문 유형 없음`() {
        assertNull(MyTypeHintRules.bubbleType(true, "TEACHER", listOf("TEACHER"), false))
        assertNull(MyTypeHintRules.bubbleType(true, "GENERAL", listOf("TEACHER"), true))
        assertNull(MyTypeHintRules.bubbleType(false, "GENERAL", listOf("TEACHER"), false))
        assertNull(MyTypeHintRules.bubbleType(true, "GENERAL", listOf("GENERAL", "HOBBY"), false))
    }
}
