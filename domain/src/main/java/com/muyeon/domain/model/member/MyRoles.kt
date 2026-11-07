package com.muyeon.domain.model.member

/**
 * 내 유형 — `GET /me/roles` 의 `{ held, activeType }`(roles.service.ts getMyRoles).
 *   [held] 는 인증까지 끝난 실제 보유 유형. 고를 수 있는 유형([switchable])은 일반 + 보유(취미·공간 제외) — 웹 switchableTypes 와 같다.
 */
data class MyRoles(val held: List<String>, val activeType: String) {
    val switchable: List<String>
        get() = listOf(ActiveType.GENERAL) + held.map { it.uppercase() }
            .filter { it != ActiveType.HOBBY && it != ActiveType.GENERAL && it != ActiveType.SPACE }
            .distinct()
}
