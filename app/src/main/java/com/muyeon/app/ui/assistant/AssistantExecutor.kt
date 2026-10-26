package com.muyeon.app.ui.assistant

import android.app.Activity
import android.content.Intent
import com.muyeon.app.webview.NativeActionRouter
import com.muyeon.app.webview.NativeWebRoute
import com.muyeon.app.webview.WebViewActivity
import org.json.JSONObject

/**
 * AI 도우미 창에서 고른 기능 실행. iOS AssistantExecutor 와 같은 규칙.
 *  ★ 새 브릿지를 만들지 않는다 — 웹 버튼과 같은 표(NativeActionRouter.open)로 화면을 연다.
 *  ★ 허용 목록의 '화면 여는 동작'만. 토큰·권한·파일 같은 응답형 브릿지(Android* 인터페이스)는 부르지 않는다.
 *  ★ 웹 경로는 WebViewActivity 를 CLEAR_TOP|SINGLE_TOP 으로 다시 앞에 세우고 EXTRA_GO_PATH 로 보낸다
 *    → onNewIntent → evalWhenReady(웹 준비 확인 대기열)로 __nativeGo. 웹이 아직 준비 전이어도 유실되지 않는다.
 *    네이티브 화면은 CLEAR_TOP 경로를 쓰지 않는다(지금 화면 위에 그냥 연다).
 */
object AssistantExecutor {
    /** 서버 기능 목록과 같은 표(백엔드 assistant.catalog.defaults.ts KNOWN_NATIVE_ACTIONS). */
    val allowedActions = setOf(
        "openAcademyInvites", "openAcademyProfile", "openAcademyTeachers", "openAutoQuoteHistory", "openAutoQuoteTemplates",
        "openChatList", "openLessonCalendar", "openLessonCreate", "openLessonQuoteHub", "openLessonSlotManage", "openMembership",
        "openMembershipPerformance", "openMyJobPostings", "openMyQuotes", "openNotifications", "openNotificationSettings",
        "openPublicProfile", "openQuoteBrowse", "openQuoteWizard", "openResumeEdit", "openResumeList", "openReviewList",
        "openStudioMembers", "openStudioOps", "openStudioSales", "openStudioSchedule",
        "openCastingStage", // 10-26 전체 메뉴 '캐스팅·무대'
    )

    sealed interface Outcome {
        data object Opened : Outcome
        data object NeedsUpdate : Outcome
        data class Flow(val flow: String) : Outcome
    }

    /** 이 앱이 앱 화면으로 열 수 있는 메뉴인가(허용 목록). */
    fun canOpenNative(item: AssistantItem): Boolean = item.nativeAction != null && item.nativeAction in allowedActions

    fun run(activity: Activity, item: AssistantItem): Outcome {
        if (item.kind == "FLOW") return item.flow?.let { Outcome.Flow(it) } ?: Outcome.NeedsUpdate
        com.muyeon.app.analytics.Analytics.click("assistant:${item.id}")
        val action = item.nativeAction
        if (action != null && action in allowedActions) {
            val data = JSONObject().apply { item.nativeParams.forEach { (k, v) -> put(k, v) } }
            if (NativeActionRouter.open(activity, action, data)) return Outcome.Opened
        }
        val path = item.webPath
        if (path != null && path.startsWith("/")) {
            activity.startActivity(
                Intent(activity, WebViewActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(NativeWebRoute.EXTRA_GO_PATH, path),
            )
            return Outcome.Opened
        }
        return Outcome.NeedsUpdate
    }
}
