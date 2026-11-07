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

    /** roles.controller.ts — 내 보유 유형·활성유형. */
    object Roles {
        const val MINE = "$API/me/roles"
    }

    /** home controller — 회원유형 화면 배경(랜덤 1장). */
    object Home {
        const val ROLE_HERO = "$API/home/role-hero"
    }

    /** scraps.controller.ts — 목록은 me/scraps, 토글은 영역별 경로. */
    object Scraps {
        const val MINE = "$API/me/scraps"                     // GET ?targetType=
        const val TOGGLE = "$API/{segment}/{id}/scrap"        // POST 찜 / DELETE 해제
    }

    /** performances.controller.ts */
    object Performances {
        const val PAGE = "$API/performances/page"             // GET genre·subGenre·tag·cursor·limit
        const val DETAIL = "$API/performances/{id}"
    }

    /** auth.controller.ts */
    object Auth {
        const val ACTIVE_TYPE = "$API/auth/me/active-type"   // PATCH { type }
    }
}
