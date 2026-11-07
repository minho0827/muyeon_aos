package com.muyeon.domain.repository

import com.muyeon.domain.model.my.MyMembership
import com.muyeon.domain.model.my.PendingPostings

/** 회원 MY 부가 정보 — 웹 MyHome 이 부르는 것과 같은 API. 실패는 null/0(화면을 막지 않는다). */
interface MyPageRepository {
    suspend fun myName(): String?
    suspend fun notiUnread(): Int
    suspend fun quoteUnread(): Int
    suspend fun membership(): MyMembership?
    suspend fun pendingPostings(): PendingPostings?
    suspend fun castingMaterialAllowed(): Boolean
    suspend fun supportChatEligible(): Boolean
}

/**
 * 로그아웃 — 구현은 :app(기기 토큰·활성유형·소켓·웹 저장소 로그인 정보를 함께 정리).
 *   웹은 열릴 때 자기 저장소의 토큰을 앱에 다시 넣으므로 웹 쪽도 지워야 한다.
 */
interface SessionController {
    fun logout()
}
