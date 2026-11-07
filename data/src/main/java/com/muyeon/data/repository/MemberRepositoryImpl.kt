package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.member.MyRoles
import com.muyeon.domain.model.member.RoleManageLaunch
import com.muyeon.domain.repository.MemberRepository
import com.muyeon.network.api.MemberApi
import com.muyeon.network.dto.member.ActiveTypeBody
import com.squareup.moshi.Moshi

class MemberRepositoryImpl(
    private val api: MemberApi,
    private val moshi: Moshi,
    private val activeType: ActiveTypeStore,
) : MemberRepository {
    override suspend fun myRoles(): ApiResult<MyRoles> =
        safeApiCall(moshi) { api.myRoles() }.map { it.toDomain() }

    override suspend fun setActiveType(type: String): ApiResult<Unit> =
        safeUnitCall(moshi) { api.setActiveType(ActiveTypeBody(type)) }
            .onSuccess { activeType.save(type) }

    override suspend fun roleManageLaunch(): ApiResult<RoleManageLaunch> =
        when (val roles = safeApiCall(moshi) { api.myRolesRaw() }.map { it.string() }) {
            is ApiResult.Error -> roles
            is ApiResult.Success -> {
                // 배경 사진은 부가 — 실패해도 연다.
                val hero = (safeApiCall(moshi) { api.roleHero() } as? ApiResult.Success)?.data?.image
                ApiResult.Success(RoleManageLaunch(roles.data, hero?.takeIf { it.isNotBlank() }))
            }
        }
}
