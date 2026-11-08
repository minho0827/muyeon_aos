package com.muyeon.domain.usecase.dancer

import com.muyeon.domain.model.dancer.DancerFilter
import com.muyeon.domain.repository.DancerRepository

/** 무용수 프로필 열람 — 목록·상세·캐스팅 제안·채팅(웹 DancerList·DancerDetail). */
class DancersUseCase(private val repo: DancerRepository) {
    suspend fun page(filter: DancerFilter, cursor: String?) = repo.page(filter, cursor)
    suspend fun detail(id: Long) = repo.detail(id)
    suspend fun offer(id: Long, callId: Long?) = repo.offer(id, callId)
    suspend fun directRoom(userId: Long) = repo.directRoom(userId)
    suspend fun myOpenCastingCalls() = repo.myOpenCastingCalls()

    companion object {
        /** 상세를 볼 수 있는 유형(웹 permissions dancer.read). */
        val READ_TYPES = setOf("TEAM", "ACADEMY", "TEACHER")
    }
}
