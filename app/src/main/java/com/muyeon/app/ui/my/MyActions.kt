package com.muyeon.app.ui.my

import com.muyeon.domain.model.member.ActiveType

/**
 * MY 메뉴 브리지 이름 → 앱 화면 액션(웹 MyHome onClick 과 같은 표 — 액션·데이터 키는 iOS·웹 계약 그대로).
 *   null 이면 앱 화면이 없는 메뉴 — 웹 창으로 [MyMenuItem.path].
 */
object MyActions {
    data class Native(val action: String, val data: Map<String, String> = emptyMap())

    fun of(bridge: String?, activeType: String): Native? = when (bridge) {
        "dancerProfile" -> Native("openResumeList", mapOf("mode" to "dancer"))
        "mySpaceBookings" -> Native("openMySpaceBookings")
        "mySpaces" -> Native("openMySpaces")
        "myStudioTransfers" -> Native("openMyStudioTransfers")
        "myJobPostings" -> Native("openMyJobPostings")
        "lessonCalendar" -> Native("openLessonCalendar")
        "quoteBrowse" -> Native("openQuoteBrowse")
        // 제안을 받는 쪽(강사·원장)이면 isPro — 웹 canReceiveQuoteAs.
        "myQuotes" -> Native("openMyQuotes", mapOf("isPro" to (activeType == ActiveType.TEACHER || activeType == ActiveType.ACADEMY).toString()))
        "quoteRequest" -> Native("openQuoteWizard")
        "lessonSlotManage" -> Native("openLessonSlotManage")
        "lessonQuoteHub" -> Native("openLessonQuoteHub", mapOf("role" to activeType))
        "studioOps" -> Native("openStudioOps")
        "studioMembers" -> Native("openStudioMembers")
        "studioSales" -> Native("openStudioSales")
        "studioSchedule" -> Native("openStudioSchedule")
        "academyTeachers" -> Native("openAcademyTeachers")
        "academyInvites" -> Native("openAcademyInvites")
        "notifications" -> Native("openNotifications")
        "notificationSettings" -> Native("openNotificationSettings")
        "membership" -> Native("openMembership", mapOf("featureType" to "MEMBERSHIP", "memberType" to activeType))
        else -> null
    }

    /** 회원유형 화면 — 탭 셸(MainViewModel)이 역할 원문을 받아 연다. */
    const val MEMBER_TYPE = "memberType"
}
