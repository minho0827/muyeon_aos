package com.muyeon.network.api

/**
 * 모든 서버 API 경로의 단일 출처(SSOT). 화면·저장소에서 경로 문자열 하드코딩 금지.
 *   서버: muyeon-backend(NestJS), nginx 가 `/api` → 백엔드로 넘긴다. 응답은 봉투 없이 객체 그대로.
 *   새 경로는 출처(컨트롤러 파일)를 주석으로 남긴다 — docs/ARCHITECTURE.md 9단계 체크리스트 1번.
 */
object ApiEndpoints {
    const val API = "api"

    /** app-menus.controller.ts — surface=TAB|MY|SHORTCUT, 유형은 X-Active-Type. */
    object AppMenus {
        const val LIST = "$API/app-menus"
    }
}
