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
        const val BLOCK = "$API/users/{id}/block"                                 // POST 차단 · DELETE 해제
        const val MY_BLOCKS = "$API/me/blocks"                                    // GET → [{ id, name, image, blockedAt }]
    }

    /** auth.controller.ts */
    object Auth {
        const val ACTIVE_TYPE = "$API/auth/me/active-type"   // PATCH { type }
        const val UI_FLAGS = "$API/auth/me/ui-flags"         // GET { flags } · PATCH { key: true }
        // ★ 2026-11-08 로그인·아이디 찾기·비밀번호 재설정(웹 Login.js · AccountRecovery.js) — auth.controller.ts
        const val LOGIN = "$API/auth/login"                                  // POST { email, password } → { accessToken, sessionId, user }
        const val FIND_ID = "$API/auth/find-id"                              // POST { name, phone } → { email } · 404 '일치하는 회원 정보가 없습니다.'
        const val EMAIL_SEND_CODE = "$API/auth/email/send-code"              // POST { email, purpose: RESET } (메일 발송)
        const val RESET_PASSWORD_EMAIL = "$API/auth/reset-password-email"    // POST { email, code, newPassword } → { ok }
        const val TERMS_AGREE = "$API/auth/terms/agree"                      // POST { agreements } (약관 게이트 — 로그인 뒤 termsAgreed=false)
        // ★ 2026-11-08 [b1] 가입·소셜 추가정보(웹 Register.js · SocialSignup.js) — auth.controller.ts · identity.controller.ts · roles.controller.ts
        const val EMAIL_VERIFY_CODE = "$API/auth/email/verify-code"          // POST { email, code, purpose: SIGNUP }
        const val REGISTER = "$API/auth/register"                            // POST { email, password, name, phone?, identityToken?, agreements } → { accessToken, sessionId, user }
        const val SOCIAL_COMPLETE = "$API/auth/me/social-complete"           // PATCH { name, identityToken? } → 갱신된 user
        const val NICE_CONFIG = "$API/identity/nice/config"                  // GET → { available, required }
        const val NICE_START = "$API/identity/nice/start"                    // POST { returnTo } → { authUrl } (가입 전 본인확인)
        const val NICE_RESULT = "$API/identity/nice/result"                  // GET ?token= → { name, phone(가림) }
        const val ROLES = "$API/me/roles"                                    // POST { role } (가입 직후 회원유형 — 웹 __onRoleComplete callAddRole)
        const val ROLE_VERIFICATION = "$API/me/roles/{role}/verification"    // POST { documents, academyName?, businessRegistrationNumber?, businessStartDate? } → { autoApproved }
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

    /** ★ 2026-11-08 무용수 프로필 열람 — profiles.controller.ts · chat(direct room) · me.controller.ts(postings). */
    object Dancers {
        const val PAGE = "$API/dancers/page"                 // GET major·gender·hasVideo·hasAward·cursor·limit
        const val DETAIL = "$API/dancers/{id}"               // GET(공연팀·학원·강사만 — 403)
        const val OFFER = "$API/dancers/{id}/offer"          // POST { callId? } 캐스팅 제안(공연팀)
        const val DIRECT_ROOM = "$API/chat/rooms/direct"     // POST { targetUserId, targetType } → { roomId } · 403 RECRUIT_CHAT_LIMIT
        const val MY_POSTINGS = "$API/me/postings"           // GET → 내 공고(kind=CASTING 만 고른다)
    }

    /** ★ 2026-11-08 개인정보 수정(웹 ProfileEdit) — auth.controller.ts · identity.controller.ts. */
    object Account {
        const val ME = "$API/auth/me"                                   // GET 내 정보 · PATCH { name?, image }
        const val LINK_IDENTITY = "$API/auth/me/identity"               // POST { identityToken } → 갱신된 내 정보
        const val NICE_LINK_START = "$API/identity/nice/link/start"     // POST { returnTo } → { authUrl } (로그인 회원 본인확인)
        const val WITHDRAWAL_INFO = "$API/auth/me/withdrawal-info"      // GET 탈퇴 안내(사유·정리할 내역·삭제 건수 — 서버가 판정)
        const val WITHDRAW = "$API/auth/me/withdraw"                    // POST { reasonCode, reasonText?, agreements, password|confirmText } · 실패 code WITHDRAW_BLOCKED
        const val PROFILE = "$API/auth/me/profile"                      // GET 강사프로필(user.profile) · PATCH { genres } (역할 부여 없음)
        const val PASSWORD = "$API/auth/me/password"                    // PATCH { newPassword }
        const val SUB_TRUST = "$API/me/sub-trust"                       // GET 대타 신뢰도 { points90d, cancels90d, restrictedUntil, history }
    }

    /** ★ 2026-11-08 경력·자격 확인(웹 CredentialsManage) — credentials.controller.ts · uploads.controller.ts(비공개 서류). */
    object Credentials {
        const val MINE = "$API/me/credentials"                 // GET → { identity:{verified}, unsortedPending, items:[…] }
        const val SUBMIT = "$API/me/credentials/{kind}"        // POST { keys } → 같은 모양
        const val UPLOAD_DOCUMENT = "$API/uploads/document"    // POST multipart file(이미지·PDF) → { key }
    }

    /** ★ 2026-11-08 상담 채팅(웹 supportChat/SupportChat.js) — support-chat.controller.ts. 응답 모양이 넓어 저장소가 직접 읽는다. */
    object SupportChat {
        const val ELIGIBILITY = "$API/support-chat/eligibility"           // GET { eligible, hasRooms }
        const val SUMMARY = "$API/support-chat/summary"                   // GET { delayNoticeOn, delayNoticeText }
        const val NOTICES = "$API/support-chat/notices"                   // GET 알림 배열
        const val NOTICES_READ_ALL = "$API/support-chat/notices/read-all" // POST
        const val ROOMS = "$API/support-chat/rooms"                       // GET 내 상담 목록 · POST 새 문의 → { room }
        const val MESSAGES = "$API/support-chat/rooms/{id}/messages"      // GET ?after= · POST { content, after } → view { room, menu, messages }
        const val CHOOSE = "$API/support-chat/rooms/{id}/choose"          // POST { nodeId } | { action: BACK|HOME }, after
        const val HANDOFF = "$API/support-chat/rooms/{id}/handoff"        // POST { after }
        const val CLOSE = "$API/support-chat/rooms/{id}/close"            // POST { after }
        const val READ = "$API/support-chat/rooms/{id}/read"              // POST
    }

    /** regions.controller.ts */
    object Regions {
        const val NEAREST = "$API/regions/nearest"           // GET lat·lng → Region | null
        const val LIST = "$API/regions"                      // (b4) GET → [{ code, sido, sigungu, name }] 시군구 전체
    }

    /** ★ 2026-11-08 (b4) alerts.controller.ts — 관심 조건 알림(웹 /alerts). */
    object AlertPrefs {
        const val ME = "$API/me/alert-prefs"                 // GET → { regions, fields, genres, targets, types, push } · PATCH 같은 모양
    }

    /** ★ 2026-11-08 (b4) policy-documents.controller.ts — 약관·정책 문서(비로그인). */
    object Policies {
        const val DOC = "$API/policies/{docKey}"                           // GET → { title, version, effectiveDate, body, upcoming, history }
        const val VERSION = "$API/policies/{docKey}/versions/{version}"    // GET → { title, version, effectiveDate, status, changeSummary, body }
    }

    /** ★ 2026-11-08 묶음 2 — 학원 정보 관리(웹 my/AcademyProfile.js). auth.controller.ts(me/academy) · profiles.controller.ts(academies/:id). */
    object AcademyProfile {
        const val ME = "$API/auth/me/academy"              // GET 학원 정보 · PATCH(ACADEMY 활성 유형) 전체 칸
        const val OWNER = "$API/auth/me"                    // GET { id, name } — 대표원장(본인확인 실명) · 내 학원 번호
        const val PUBLIC = "$API/academies/{id}"            // GET 공개 프로필 — 미리보기의 '수업 안내'(lessons)
    }

    /** ★ 2026-11-08 묶음 2 — 캐스팅·공연 자료 업로드(웹 my/CastingMaterials.js) — casting-materials.controller.ts. */
    object CastingMaterial {
        const val LIST = "$API/casting-materials"           // GET ?kind=CASTING|PERFORMANCE → { items } · POST { kind, imageUrls, instagramUrl?, linkUrl? }
    }

    /**
     * ★ 2026-11-08 묶음 3 — 대타공고 수정(subs.controller.ts GET·PATCH :id — 작성자만) ·
     *   커뮤니티 글쓰기/수정(community.controller.ts POST /community/posts · PATCH /community/posts/:id — 본인 글만).
     */
    object Write {
        const val SUB = "$API/subs/{id}"                          // GET 상세(수정 화면 채우기) · PATCH UpdateSubDto(부분)
        const val POSTS = "$API/community/posts"                  // POST { category, title, body, images? }
        const val POST = "$API/community/posts/{id}"              // PATCH { category, title, body, images }
    }

    /**
     * ★ 2026-11-08 홈 [레슨] '레슨을 찾는 회원들' — quotes.controller.ts(로그인 필요, 비회원 공개 목록 API 는 없다).
     *   HOME 은 모든 유형(최근 OPEN 10건 · 1:1 지정 요청 제외), AVAILABLE 은 강사·학원 활동 유형만(20건 페이지).
     */
    object QuoteFeed {
        const val HOME = "$API/quotes/home"                   // GET → [{ id, categoryId, region, answers(days·time·format), createdAt, isDemo, isMine, canRespond }]
        const val AVAILABLE = "$API/quotes/available"         // GET categoryId·page → { items(Quote), hasMore, page, total }
    }
}
