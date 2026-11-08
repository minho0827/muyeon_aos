package com.muyeon.app.navigation

/**
 * 앱 셸(MainActivity) 안 화면 경로의 단일 출처 — 문자열 경로 하드코딩 금지(PaceERA Screen.kt 와 같은 규칙).
 *   탭 루트는 [MainTab.route] 가 이 값을 쓴다. 상세 화면은 [build] 로 인자를 채운다.
 *   새 화면 = 여기 한 줄 + AppNavHost composable 한 줄 — docs/ARCHITECTURE.md 9단계 체크리스트 8번.
 */
sealed class Screen(val route: String) {
    /** 탭 셸(탭바 + 탭 화면). */
    data object Main : Screen("main")

    // === 탭 루트(MainScreen 안쪽 NavHost) ===
    data object TabHome : Screen("tab/home")
    data object TabLessons : Screen("tab/lessons")
    data object TabPerformance : Screen("tab/performance")
    data object TabMy : Screen("tab/my")
    data object TabCasting : Screen("tab/casting")
    data object TabJobs : Screen("tab/jobs")
    data object TabSeek : Screen("tab/seek")
    data object TabSubs : Screen("tab/subs")
    data object TabSpaces : Screen("tab/spaces")
    data object TabTransfers : Screen("tab/transfers")
    // 캐스팅·무대 상세·목록 경로는 ui/castingstage/StageRoutes.SHELL("stage/…") 한 곳.

    // === 탭 위 상세(바깥 NavHost — 탭바가 없다) ===
    /** 커뮤니티 목록 — MY 안 섹션·웹 /community 링크에서 연다(탭이 아니다, 2026-11-08). */
    data object CommunityList : Screen("community")

    /** 공고 상세(보는 사람) — 채용·대타. 웹 /jobs/:id · /subs/:id 링크도 여기로. */
    data object JobDetail : Screen("jobs/{id}") {
        const val ARG_ID = "id"
        fun build(id: Long) = "jobs/$id"
    }
    data object SubDetail : Screen("subs/{id}") {
        const val ARG_ID = "id"
        fun build(id: Long) = "subs/$id"
    }

    /** 대타공고 설문 등록(채용 › 대타공고 ＋ · 웹 /subs/create). */
    data object SubWizard : Screen("subs/create?prefill={prefill}") {
        const val ARG_PREFILL = "prefill"
        fun build(prefill: String? = null) = if (prefill.isNullOrBlank()) "subs/create" else "subs/create?prefill=$prefill"
    }
    data object CommunityPost : Screen("community/post/{id}") {
        const val ARG_ID = "id"
        fun build(id: Long) = "community/post/$id"
    }

    data object PerformanceDetail : Screen("performance/{id}") {
        const val ARG_ID = "id"
        fun build(id: Long) = "performance/$id"
    }

    // === ★ 2026-11-08 웹에만 있던 메뉴 화면을 앱으로(탭 위 화면 — 뒤로 가면 있던 자리로) ===
    /** 공지사항 목록·상세(웹 /notice · /noticeDetail). */
    data object Notices : Screen("notices")
    data object NoticeDetail : Screen("notices/{id}") {
        const val ARG_ID = "id"
        fun build(id: Long) = "notices/$id"
    }
    /** FAQ(웹 /faq). */
    data object Faq : Screen("faq")

    /** 1:1 문의내역·상세·쓰기(웹 /support/inquiries · /support/inquiries/:id · /support/inquiry · /support/feedback). */
    data object SupportInquiries : Screen("support/inquiries")
    data object SupportInquiryDetail : Screen("support/inquiries/{id}") {
        const val ARG_ID = "id"
        fun build(id: Long) = "support/inquiries/$id"
    }
    data object SupportCompose : Screen("support/compose/{type}") {
        const val ARG_TYPE = "type"
        const val INQUIRY = "inquiry"
        const val FEEDBACK = "feedback"
        fun build(type: String) = "support/compose/$type"
    }

    /** 찜/관심(웹 /likes). */
    data object Likes : Screen("likes")

    /** 내가 예약한 내역(웹 /myReservations). */
    data object MyReservations : Screen("myReservations")

    /** 내가 지원한 공고(웹 /myApplications). */
    data object MyApplications : Screen("myApplications")
    /** 받은 지원(웹 /receivedApplications?kind=&postingId=) — 인자가 비면 전체. */
    data object ReceivedApplications : Screen("receivedApplications?kind={kind}&postingId={postingId}") {
        const val ARG_KIND = "kind"
        const val ARG_POSTING = "postingId"
        fun build(kind: String?, postingId: Long?) =
            if (kind.isNullOrBlank() && postingId == null) "receivedApplications"
            else "receivedApplications?kind=${kind.orEmpty()}&postingId=${postingId?.toString().orEmpty()}"
    }

    /** 설정(웹 /setting) · 차단한 사용자(웹 /blockedUsers). */
    data object Settings : Screen("settings")
    data object BlockedUsers : Screen("blockedUsers")

    /** 무용수 프로필 열람 목록·상세(웹 /dancers · /dancers/:id). */
    data object Dancers : Screen("dancers")
    data object DancerDetail : Screen("dancers/{id}") {
        const val ARG_ID = "id"
        fun build(id: Long) = "dancers/$id"
    }

    /** 개인정보 수정(웹 /profileEdit). */
    data object ProfileEdit : Screen("profileEdit")

    /** 경력·자격 확인(웹 /my/credentials). */
    data object Credentials : Screen("my/credentials")

    /** 비밀번호 변경(웹 /changePassword) · 대타 신뢰도(웹 /subTrust). */
    data object ChangePassword : Screen("changePassword")
    data object SubTrust : Screen("subTrust")
    /** 레슨 전공 설정(웹 /lessonGenres). */
    data object LessonGenres : Screen("lessonGenres")
    /** 회원탈퇴(웹 /withdraw). */
    data object Withdraw : Screen("withdraw")

    /** 상담 채팅 목록·방(웹 /support/chat · /support/chat?room=N). */
    data object SupportChatList : Screen("support/chat")
    data object SupportChatRoom : Screen("support/chat/{room}") {
        const val ARG_ROOM = "room"
        fun build(id: Long) = "support/chat/$id"
    }

    /** 홈 '전체보기' 레슨 목록(웹 /lessonList?title=&type=). */
    data object LessonList : Screen("lessonList?title={title}&type={type}") {
        const val ARG_TITLE = "title"
        const val ARG_TYPE = "type"
        fun build(title: String?, type: String?) =
            "lessonList?title=${android.net.Uri.encode(title.orEmpty())}&type=${android.net.Uri.encode(type.orEmpty())}"
    }

    // ★ 2026-11-08 묶음 2 — 학원 정보 관리(웹 /academyProfile?aiDraft=) · 캐스팅·공연 자료 업로드(웹 /my/castingMaterials · /my/performanceMaterials).
    data object AcademyEdit : Screen("academyProfile?aiDraft={aiDraft}") {
        const val ARG_AI_DRAFT = "aiDraft"
        fun build(aiDraft: String?) = if (aiDraft.isNullOrBlank()) "academyProfile" else "academyProfile?aiDraft=${android.net.Uri.encode(aiDraft)}"
    }
    data object CastingMaterials : Screen("castingMaterials/{kind}") {
        const val ARG_KIND = "kind"
        fun build(kind: String) = "castingMaterials/$kind"
    }
}
