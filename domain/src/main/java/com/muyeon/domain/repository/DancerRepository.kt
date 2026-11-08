package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.dancer.CastingCallOption
import com.muyeon.domain.model.dancer.DancerCard
import com.muyeon.domain.model.dancer.DancerFilter
import com.muyeon.domain.model.dancer.DancerProfile

/** 무용수 프로필 열람 — 목록(공개) · 상세(공연팀·학원·강사) · 캐스팅 제안 · 채팅방. */
interface DancerRepository {
    suspend fun page(filter: DancerFilter, cursor: String?): ApiResult<CursorPage<DancerCard>>
    suspend fun detail(id: Long): ApiResult<DancerProfile>
    suspend fun offer(id: Long, callId: Long?): ApiResult<Unit>
    /** 1:1 방 → roomId. 한도 초과는 Error.code = RECRUIT_CHAT_LIMIT. */
    suspend fun directRoom(userId: Long): ApiResult<Long>
    suspend fun myOpenCastingCalls(): ApiResult<List<CastingCallOption>>
}
