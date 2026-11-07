package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.member.MyRoles
import com.muyeon.domain.model.member.RoleManageLaunch

/** 회원 유형(보유·활성). 로그인한 회원만 — 비회원은 호출하지 않는다. */
interface MemberRepository {
    suspend fun myRoles(): ApiResult<MyRoles>
    /** 서버 저장(PATCH /auth/me/active-type). 성공하면 로컬 활성유형도 바꾼다. */
    suspend fun setActiveType(type: String): ApiResult<Unit>
    /** 유형 편집 화면 열기 값. 역할 조회가 실패하면 Error — 빈 역할로 열면 보유 유형이 전부 '추가'로 보인다. */
    suspend fun roleManageLaunch(): ApiResult<RoleManageLaunch>
}
