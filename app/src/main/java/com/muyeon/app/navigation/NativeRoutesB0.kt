package com.muyeon.app.navigation

import com.muyeon.app.ui.main.MainTab

/**
 * ★ 2026-11-08 묶음 0(경로표 정리) — 앱 화면이 이미 있는데 경로(푸시·링크·웹 창 이동)로 오면 웹 창이 뜨던 주소.
 *   [NativeRoutes.resolve] 가 자기 `when` 표 다음에 이 표를 본다. iOS Shell/Navigation/NativeRoutes+B0.swift 와 같은 표·같은 순서.
 *   판정은 웹 common/nativeLink.js(NATIVE_LINKS) 와 같다(쿼리 as=dancer·seekProfile·src 등 그대로).
 *   여기서 쓰는 액션 중 'Link'·'Edit'·'Detail'·'MemberType'·'AccountRecovery'·'CommunityCategory'·'LessonSettings' 는
 *   앱 안 전용(웹 브리지 계약 아님) — [com.muyeon.app.webview.NativeActionRouterB0].
 *   ★ 11-10 /casting/:id/edit 도 앱 화면(등록 흐름 수정 모드). 남겨 둔 웹: B 분류(새 화면 필요), D(앱 밖)·E(외부 인증·결제).
 */
internal object NativeRoutesB0 {
    private val ACADEMY = Regex("^/academies/(\\d+)$")
    private val JOB_EDIT = Regex("^/jobs/(\\d+)/edit$")
    private val CASTING_EDIT = Regex("^/casting/(\\d+)/edit$")
    private val LESSON_EDIT = Regex("^/lessons/(\\d+)/edit$")
    private val SPACE_EDIT = Regex("^/spaces/(\\d+)/edit$")
    private val TRANSFER_EDIT = Regex("^/studio-transfers/(\\d+)/edit$")
    private val TEACHER_REVIEWS = Regex("^/teachers/(\\d+)/reviews$")
    private val TEACHER = Regex("^/teachers/(\\d+)$")
    private val STAGE_ISSUE = Regex("^/casting/stage/(\\d+)$")
    private val STAGE_CATEGORY = Regex("^/casting/stage/(\\d+)/([A-Z_]+)$")
    private val STUDIO_MEMBER = Regex("^/studio/members/(\\d+)$")
    /** 웹 community/:category — 카테고리 코드는 대문자(QUESTION·CHAT …). /community/write 등은 걸리지 않는다. */
    private val COMMUNITY_CATEGORY = Regex("^/community/([A-Z_]+)$")
    private val POSITIVE = Regex("^[1-9]\\d*$")
    /** ★ 2026-11-09 취미 클래스는 없앴다(사용자 결정) — 예전 링크 /classes · /classes/:id 는 홈 레슨 탭으로. */
    private val CLASSES = Regex("^/classes(/[^/]+)?$")

    /** [p] = 끝 / 를 뗀 경로, [query] = 쿼리(풀지 않은 값), [hasQuery] = '?' 가 있었다. */
    fun resolve(p: String, query: Map<String, String>, hasQuery: Boolean): NativeRoutes.Target? {
        fun action(name: String, vararg kv: Pair<String, String?>) =
            NativeRoutes.Target.Action(name, kv.mapNotNull { (k, v) -> v?.takeIf { it.isNotEmpty() }?.let { k to it } }.toMap())

        when (p) {
            "/fieldVisibility" -> return action("openFieldVisibilityLink", "as" to query["as"])
            // 웹 nativeLink: id 가 없거나 양의 정수일 때만(이상한 id 는 웹이 안내한다).
            "/resumeEdit" -> {
                val id = query["id"]
                if (id != null && !POSITIVE.matches(id)) return null
                return action("openResumeEditLink", "as" to query["as"], "id" to id, "seekProfile" to query["seekProfile"], "redirect" to redirect(p, query))
            }
            "/lessonSettings" -> return action("openLessonSettings")
            "/spaces/create" -> return action("openSpaceEditor")
            "/studio-transfers/create" -> return action("openStudioTransferEditor")
            "/casting/stage" -> return action("openCastingStage", "screen" to "issues")
            // 회원유형 관리 — ?verify=ROLE 이면 그 유형 인증. 서버 역할 반려 푸시는 예전 주소 /memberSwitch 를 보낸다.
            "/my/memberType" -> return action("openMemberType", "verify" to query["verify"], "redirect" to redirect(p, query))
            "/memberSwitch" -> return action("openMemberType", "redirect" to p)
            "/accountRecovery" -> return action("openAccountRecovery", "type" to if (query["type"] == "pw") "pw" else "id")
            // 강사 프로필 보완 푸시·알림톡(?seek 없음) — 구직 프로필과 같은 규칙: 이력서가 있으면 관리, 없으면 등록.
            "/profileManage" -> return action("openSeekProfile")
            "/pricing" -> return action("openMembership")
            // iOS 와 같다 — 서버 푸시가 보내는 견적 둘러보기 주소.
            "/availableQuotes" -> return action("openQuoteBrowse")
            // 역할 승인 푸시 — 홈 탭(iOS 와 같다).
            "/home" -> if (query["roleApproved"] != null) return NativeRoutes.Target.Tab(MainTab.HOME)
            // 찜(웹 MyScraps) — 앱 찜 화면.
            "/myScraps" -> return NativeRoutes.Target.Route(Screen.Likes.route, auth = true)
        }
        ACADEMY.find(p)?.let { return action("openAcademyProfile", "academyId" to it.groupValues[1], "src" to query["src"]) }
        CASTING_EDIT.find(p)?.let { return action("openCastingStage", "castingId" to it.groupValues[1], "screen" to "edit") }
        JOB_EDIT.find(p)?.let { return action("openJobPostingEdit", "jobId" to it.groupValues[1], "redirect" to p) }
        LESSON_EDIT.find(p)?.let { return action("openLessonCreate", "lessonId" to it.groupValues[1]) }
        SPACE_EDIT.find(p)?.let { return action("openSpaceEditor", "id" to it.groupValues[1]) }
        TRANSFER_EDIT.find(p)?.let { return action("openStudioTransferEditor", "id" to it.groupValues[1]) }
        TEACHER_REVIEWS.find(p)?.let { return action("openReviewList", "teacherId" to it.groupValues[1]) }
        // 쿼리 있는 강사 상세 — ?review=1 은 후기 쓰기(이름은 화면이 공개 프로필에서 읽는다), 그 외(?src= 등)는 공개 프로필.
        if (hasQuery) TEACHER.find(p)?.let {
            return if (query["review"] == "1") action("openReviewWrite", "teacherId" to it.groupValues[1])
            else action("openPublicProfile", "userId" to it.groupValues[1], "src" to query["src"])
        }
        STAGE_CATEGORY.find(p)?.let { return action("openCastingStage", "issueNo" to it.groupValues[1], "category" to it.groupValues[2]) }
        STAGE_ISSUE.find(p)?.let { return action("openCastingStage", "issueNo" to it.groupValues[1]) }
        STUDIO_MEMBER.find(p)?.let { return action("openStudioMemberDetail", "memberId" to it.groupValues[1], "redirect" to p) }
        COMMUNITY_CATEGORY.find(p)?.let { return action("openCommunityCategory", "category" to it.groupValues[1]) }
        if (CLASSES.matches(p)) return NativeRoutes.Target.Tab(MainTab.LESSONS)
        return null
    }

    /** 로그인 뒤 돌아올 주소(쿼리 포함). */
    private fun redirect(p: String, query: Map<String, String>): String =
        if (query.isEmpty()) p else p + "?" + query.entries.joinToString("&") { "${it.key}=${it.value}" }
}
