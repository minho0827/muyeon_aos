package com.muyeon.domain.usecase.member

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.member.MyRoles
import com.muyeon.domain.model.member.RoleManageLaunch
import com.muyeon.domain.repository.MemberRepository

class GetMyRolesUseCase(private val repo: MemberRepository) {
    suspend operator fun invoke(): ApiResult<MyRoles> = repo.myRoles()
}

/** 활성유형 전환 — 서버 저장이 먼저다(로컬만 바꾸면 화면 복귀 때 서버의 옛 값으로 되돌아간다, ActiveRole.saveOnServer 주석). */
class SwitchActiveTypeUseCase(private val repo: MemberRepository) {
    suspend operator fun invoke(type: String): ApiResult<Unit> = repo.setActiveType(type.uppercase())
}

class GetRoleManageLaunchUseCase(private val repo: MemberRepository) {
    suspend operator fun invoke(): ApiResult<RoleManageLaunch> = repo.roleManageLaunch()
}
