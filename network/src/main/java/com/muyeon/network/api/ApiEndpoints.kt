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

    /** community.controller.ts */
    object Community {
        const val POSTS_PAGE = "$API/community/posts/page"                        // GET category·sort·cursor·limit
        const val POST = "$API/community/posts/{id}"                              // GET(?comments=0) · DELETE
        const val POST_LIKE = "$API/community/posts/{id}/like"                    // POST → { liked, likeCount }
        const val COMMENTS_PAGE = "$API/community/posts/{id}/comments/page"
        const val REPLIES_PAGE = "$API/community/posts/{id}/comments/{parentId}/replies/page"
        const val COMMENTS = "$API/community/posts/{id}/comments"                 // POST { body, parentId? }
        const val COMMENT = "$API/community/posts/{id}/comments/{commentId}"      // PATCH { body } · DELETE
        const val COMMENT_LIKE = "$API/community/posts/{id}/comments/{commentId}/like"
    }

    /** reports.controller.ts · blocks.controller.ts */
    object Safety {
        const val REPORTS = "$API/reports"                                        // POST { targetType, targetId, reason, detail? }
        const val BLOCK = "$API/users/{id}/block"                                 // POST 차단
    }

    /** auth.controller.ts */
    object Auth {
        const val ACTIVE_TYPE = "$API/auth/me/active-type"   // PATCH { type }
        const val UI_FLAGS = "$API/auth/me/ui-flags"         // GET { flags } · PATCH { key: true }
    }

    /** MY 화면이 읽는 것 — 출처 파일은 각 줄 끝 주석. */
    object My {
        const val ME = "$API/auth/me"                                         // auth.controller.ts
        const val NOTI_UNREAD = "$API/notifications/unread-count"             // notifications.controller.ts → number
        const val QUOTE_UNREAD = "$API/quotes/unread-count"                   // quotes.controller.ts → { count }
        const val MEMBERSHIP = "$API/monetization/membership/me"              // monetization.controller.ts
        const val PENDING_POSTINGS = "$API/me/pending-postings/summary"       // → { total, roles:[{role,status}] }
        const val CASTING_MATERIAL_ACCESS = "$API/casting-materials/access"   // → { allowed }
        const val SUPPORT_CHAT_ELIGIBILITY = "$API/support-chat/eligibility"  // → { eligible, hasRooms }
    }

    /** 홈(웹 Home·TeacherHome·HomeClassic) — 출처는 줄 끝 주석. */
    object Home {
        const val LESSON_PREVIEW = "$API/lesson-products/home-preview"   // lesson-products.controller.ts
        const val MY_LESSONS = "$API/lesson-products/mine"
        const val JOBS = "$API/jobs"                                     // jobs.controller.ts
        const val SUBS = "$API/subs"                                     // subs.controller.ts
        const val SPACES = "$API/spaces"                                 // spaces.controller.ts
        const val TRANSFERS = "$API/studio-transfers/page"               // studio-transfers.controller.ts
        const val NOTICES = "$API/notices"                               // notices.controller.ts
        const val QUOTES = "$API/quotes/home"                            // quotes.controller.ts
        const val RECOMMEND_BLOCKS = "$API/quotes/recommend-blocks"      // POST { teacherId }
        const val ACADEMY_PROFILE = "$API/auth/me/academy"               // auth.controller.ts
        const val RECOMMENDED_TEACHERS = "$API/teachers/recommended"     // profiles.controller.ts
        const val MY_POSTINGS = "$API/me/postings"
        const val TEACHER_PREVIEW = "$API/home/teacher-preview"          // home.controller.ts
        const val FEED_PAGE = "$API/home/feed/page"
        const val MY_APPLICATIONS = "$API/me/applications"
        const val MY_VIEWED_JOBS = "$API/jobs/views/mine"
        const val ROLE_HERO = "$API/home/role-hero"                      // 회원유형 화면 배경(랜덤 1장)
    }

    /** profiles.controller.ts */
    object Teachers {
        const val PAGE = "$API/teachers/page"                // GET lesson·genre|field·region·regionKind·sort·cursor·limit
    }

    /** regions.controller.ts */
    object Regions {
        const val NEAREST = "$API/regions/nearest"           // GET lat·lng → Region | null
    }
}
