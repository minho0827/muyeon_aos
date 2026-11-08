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
        const val LESSON_PAGE = "$API/lesson-products/page"              // GET genre·isExperience·cursor·limit → { items, hasMore, nextCursor }
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
    /** 채용 탭 목록 — jobs.controller.ts · profiles.controller.ts(seekers) · subs.controller.ts. */
    object Recruit {
        const val JOBS_PAGE = "$API/jobs/page"                    // GET region·career·salary(쉼표)·cursor·limit → { items, hasMore, nextCursor }
        const val JOBS_FOR_ME = "$API/jobs/recommended-for-me"    // GET limit — 강사·무용수 '나에게 맞는 공고'
        const val SEEKERS_PAGE = "$API/seekers/page"              // GET types·fields·targets·careers(쉼표)·cursor·limit
        const val RECRUIT_PREFILL = "$API/quotes/recruit-prefill/{id}"  // quotes.controller.ts — 본인·1시간
        const val SUBS = "$API/subs"                              // GET region·field·target·salary·scheduleType·minClassCount → 배열(커서 없음)
    }

    object Teachers {
        const val PAGE = "$API/teachers/page"                // GET lesson·genre|field·region·regionKind·sort·cursor·limit
    }

    /** ★ 2026-11-08 고객센터 — content.controller.ts(공지·FAQ, 공개) · support-inquiries.controller.ts(1:1 문의·의견, 로그인). */
    object Support {
        const val NOTICES = "$API/notices"                            // GET → Notice[](고정 먼저 → 최신)
        const val NOTICE = "$API/notices/{id}"                        // GET(조회수 +1)
        const val FAQS = "$API/faqs"                                  // GET ?category= → Faq[]
        const val FAQ_CATEGORIES = "$API/faqs/categories"             // GET → string[]
        const val INQUIRIES = "$API/support-inquiries"                // POST { type: INQUIRY|FEEDBACK, content(1~1000) }
        const val MY_INQUIRIES = "$API/support-inquiries/mine"        // GET → 내 1:1 문의(INQUIRY)만
        const val INQUIRY = "$API/support-inquiries/{id}"             // GET 상세(본인) · DELETE
    }

    /** ★ 2026-11-08 내가 예약한 내역(웹 MyReservations) — lesson-booking.controller.ts · lessons.controller.ts. */
    object Reservations {
        const val HISTORY_PAGE = "$API/me/lesson-reservations/history/page"   // GET category(PERSONAL|ACADEMY|CANCELED)·period·cursor·limit
        const val COMPLETED_PAGE = "$API/lessons/me/completed-lessons/page"   // GET period·cursor·limit — 레슨 완료(후기)
    }

    /** ★ 2026-11-08 지원 내역 — me.controller.ts(내가 지원한 공고 · 받은 지원). kind = JOB|SUB|CASTING. */
    object Applications {
        const val MINE_PAGE = "$API/me/applications/page"                                   // GET tab(ALL|PROGRESS|CONFIRMED|REJECTED)·period·cursor·limit → { items, counts, … }
        const val MINE_WITHDRAW = "$API/me/applications/{kind}/{id}/withdraw"               // POST 지원 취소(확정 전)
        const val MINE_ARCHIVE = "$API/me/applications/{kind}/{id}"                         // DELETE 내 목록에서만 숨김
        const val RECEIVED_PAGE = "$API/me/received-applications/page"                      // GET kind·postingId·archived·cursor·limit → { items, archivedCount, … }
        const val RECEIVED_ARCHIVE = "$API/me/received-applications/{kind}/{id}"            // DELETE 휴지통으로
        const val RECEIVED_RESTORE = "$API/me/received-applications/{kind}/{id}/restore"    // POST 복원
        const val RECEIVED_PURGE = "$API/me/received-applications/{kind}/{id}/permanent"    // DELETE 휴지통에서 영구 삭제
    }

    /** regions.controller.ts */
    object Regions {
        const val NEAREST = "$API/regions/nearest"           // GET lat·lng → Region | null
    }
}
