package com.muyeon.app.navigation

import com.muyeon.app.ui.main.MainTab

/**
 * 웹 주소 → 앱 화면 표(앱으로 옮긴 화면만). 푸시·링크·웹 창 이동·네이티브 복귀(NativeWebRoute)가 모두 이 표를 본다.
 *   여기 없는 주소는 웹 창으로 연다. 새 앱 화면이 생기면 한 줄 — docs/ARCHITECTURE.md §1.
 */
object NativeRoutes {
    sealed interface Target {
        data class Tab(val tab: MainTab) : Target
        /** 탭 위 앱 화면(바깥 NavHost 경로). */
        data class Route(val route: String) : Target
        /**
         * ★ 2026-11-08 웹 페이지가 열리자마자 네이티브 화면으로 넘기는 주소(이력서 관리·레슨 일정).
         *   앱 셸에서 웹 창을 거치면 웹 창 + 네이티브 화면이 두 겹으로 떴다 — 웹 창 없이 바로 네이티브를 연다.
         */
        data class Action(val action: String, val data: Map<String, String> = emptyMap()) : Target
    }

    private val COMMUNITY_POST = Regex("^/community/post/(\\d+)/?$")
    private val PERFORMANCE = Regex("^/performance/(\\d+)/?$")
    private val JOB = Regex("^/jobs/(\\d+)/?$")
    private val SUB = Regex("^/subs/(\\d+)/?$")
    private val TEACHER = Regex("^/teachers/(\\d+)/?$")
    private val LESSON = Regex("^/lessons/(\\d+)/?$")
    private val CHAT_ROOM = Regex("^/chat/(\\d+)/?$")
    private val TRANSFER = Regex("^/studio-transfers/(\\d+)/?$")
    private val MEMBERSHIP = Regex("^/membership/[A-Za-z_]+/?$")
    private val RESERVATION = Regex("^/reservations/(\\d+)/?$")

    fun resolve(path: String?): Target? {
        val clean = path?.substringBefore('#') ?: return null
        val p = clean.substringBefore('?')
        val query = clean.substringAfter('?', "").split('&').filter { it.contains('=') }.associate { it.substringBefore('=') to it.substringAfter('=') }
        when (p.trimEnd('/')) {
            "/profileResume", "/resume" -> return Target.Action("openResumeList", mapOf("mode" to if (query["as"] == "dancer") "dancer" else "teacher"))
            "/lessonCalendar" -> return Target.Action("openLessonCalendar", query["lessonId"]?.let { mapOf("lessonId" to it) } ?: emptyMap())
            // ★ 2026-11-08 채용공고 작성 · 구직 프로필 등록 — 앱 화면이 있다(웹 창 대신).
            "/jobs/create" -> return Target.Action("openJobPostingForm")
            "/profileManage" -> if (query["seek"] == "1") return Target.Action("openSeekProfile")
            // ★ 2026-11-08 전체 메뉴 점검 — 앱 화면이 이미 있는데 웹으로 가던 주소.
            "/myLessons" -> return Target.Action("openLessonManage")
            "/casting/create" -> return Target.Action("openCastingStage", mapOf("screen" to "create"))
            "/casting/all" -> return Target.Action("openCastingStage", mapOf("screen" to "castingList"))
            "/teachers" -> return Target.Action("openHomeTeachers")
            "/lessons/create" -> return Target.Action("openLessonCreate")
            // ★ 2026-11-08 받은/보낸 견적 — 앱 화면(openMyQuotes)이 있는데 웹으로 가던 주소(견적 요청 완료 '견적 보기' 등).
            "/myQuotes" -> return Target.Action("openMyQuotes", query["tab"]?.let { mapOf("tab" to it) } ?: emptyMap())
            // ★ 2026-11-08 앱 화면(NativeActionRouter 표)이 이미 있는데 웹으로 가던 주소 — 전체 메뉴·링크·푸시.
            "/notifications" -> return Target.Action("openNotifications")
            "/notificationSettings" -> return Target.Action("openNotificationSettings")
            "/myMembership" -> return Target.Action("openMembership")
            "/myMembership/performance" -> return Target.Action("openMembershipPerformance")
            "/my/academyTeachers" -> return Target.Action("openAcademyTeachers")
            "/my/academyInvites" -> return Target.Action("openAcademyInvites")
            "/mySpaces" -> return Target.Action("openMySpaces")
            "/mySpaceBookings" -> return Target.Action("openMySpaceBookings")
            "/myStudioTransfers" -> return Target.Action("openMyStudioTransfers")
            "/studio" -> return Target.Action("openStudioOps")
            "/studio/members" -> return Target.Action("openStudioMembers")
            "/studio/sales" -> return Target.Action("openStudioSales")
            "/studio/schedule" -> return Target.Action("openStudioSchedule")
            "/quoteBrowse" -> return Target.Action("openQuoteBrowse")
            "/lessonSlots" -> return Target.Action("openLessonSlotManage", query["lessonProductId"]?.let { mapOf("lessonProductId" to it) } ?: emptyMap())
            "/lessonHub" -> return Target.Action("openLessonQuoteHub")
            "/chat" -> return Target.Action("openChatList")
            // ★ 2026-11-08 고객센터 — 공지사항(웹 /noticeDetail 은 state 로 id 를 받는다 — 링크는 ?id=)·FAQ.
            "/notice" -> return Target.Route(Screen.Notices.route)
            "/noticeDetail" -> return Target.Route(query["id"]?.toLongOrNull()?.let { Screen.NoticeDetail.build(it) } ?: Screen.Notices.route)
            "/faq" -> return Target.Route(Screen.Faq.route)
            // ★ 2026-11-08 홈 '전체보기' 레슨 목록 — 쿼리(title·type)가 있어도 앱 화면.
            "/lessonList" -> return Target.Route(Screen.LessonList.build(query["title"]?.let(android.net.Uri::decode), query["type"]))
        }
        COMMUNITY_POST.find(p)?.let { return Target.Route(Screen.CommunityPost.build(it.groupValues[1].toLong())) }
        if (p.trimEnd('/') == "/community" && !clean.contains('?')) return Target.Route(Screen.CommunityList.route)
        // 대타공고 등록 — 설문식 앱 화면(레슨 요청 [네, 강사 구해요]에서 온 ?prefill= 도).
        if (p.trimEnd('/') == "/subs/create") {
            val prefill = clean.substringAfter('?', "").split('&').firstOrNull { it.startsWith("prefill=") }?.substringAfter('=')
            return Target.Route(Screen.SubWizard.build(prefill))
        }
        TEACHER.find(p)?.let { if (!clean.contains('?')) return Target.Action("openPublicProfile", mapOf("userId" to it.groupValues[1])) }
        MEMBERSHIP.find(p)?.let { return Target.Action("openMembership") }
        CHAT_ROOM.find(p)?.let { return Target.Action("openChatRoom", mapOf("roomId" to it.groupValues[1])) }
        TRANSFER.find(p)?.let { return Target.Action("openStudioTransferDetail", mapOf("id" to it.groupValues[1])) }
        // ★ 2026-11-08 레슨 예약 상세 — 앱 화면(openLessonReservationDetail)이 있다(결제 완료 뒤 이동 등).
        RESERVATION.find(p)?.let { return Target.Action("openLessonReservationDetail", mapOf("reservationId" to it.groupValues[1])) }
        LESSON.find(p)?.let { return Target.Action("openLessonDetail", mapOf("lessonId" to it.groupValues[1])) }
        JOB.find(p)?.let { return Target.Route(Screen.JobDetail.build(it.groupValues[1].toLong())) }
        SUB.find(p)?.let { return Target.Route(Screen.SubDetail.build(it.groupValues[1].toLong())) }
        PERFORMANCE.find(p)?.let { return Target.Route(Screen.PerformanceDetail.build(it.groupValues[1].toLong())) }
        // 탭 루트는 쿼리가 없을 때만(쿼리 = 웹 화면 상태 지정 — 웹이 그린다).
        if (clean.contains('?')) return null
        return MainTab.fromWebPath(p)?.takeIf { it.nativeReady }?.let { Target.Tab(it) }
    }
}
