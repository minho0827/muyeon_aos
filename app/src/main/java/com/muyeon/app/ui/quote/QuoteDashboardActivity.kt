package com.muyeon.app.ui.quote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.NativeWebRoute

/**
 * 레슨·견적 관리 허브 / 일반회원 견적 허브 — iOS `WebViewModel+Hub.swift` 의
 *  presentLessonQuoteHub / presentCustomerQuoteDashboard 1:1.
 *  웹 `openLessonQuoteHub` 브릿지로 진입.
 *
 * ⚠️ 레슨 영역(내 레슨 관리·개설·예약시간·캘린더·레슨 설정)은 아직 네이티브 미이식 —
 *  해당 카드는 웹 경로로 폴백한다(AppBridgeInterface 폴백 경로와 동일).
 */
class QuoteDashboardActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ROLE = "role"          // TEACHER | ACADEMY | (그 외 = 일반회원)

        fun start(context: Context, role: String?) {
            val i = Intent(context, QuoteDashboardActivity::class.java).putExtra(EXTRA_ROLE, role ?: "")
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val role = intent.getStringExtra(EXTRA_ROLE).orEmpty()
        val isPro = role == "TEACHER" || role == "ACADEMY"
        // 레슨 운영 허브는 강사·학원 전용 — 무용수 유형이면 안내 후 종료(2026-09-26 정책).
        if (isPro && !ActiveRole.allowLessonProvider(this)) { finish(); return }

        setContent {
            val token = remember { TokenManager.getAccessToken(this) }
            val api = remember { QuoteApi(token) }
            val autoApi = remember { AutoQuoteApi(token) }

            if (isPro) {
                // 개인레슨 관리 — 3탭(수강생 상담·찾기·레슨 소개). 카드 그리드 허브를 대체한다.
                //  기능을 고르는 중간 화면을 없애고 탭을 열면 바로 그 일의 목록이 보인다.
                val productApi = remember { com.muyeon.app.ui.lesson.LessonProductApi(token) }
                var sentDetail by remember { mutableStateOf<SentQuoteItem?>(null) }
                val detail = sentDetail
                if (detail != null) {
                    SentQuoteDetailScreen(
                        api = api,
                        item = detail,
                        onBack = { sentDetail = null },
                        onOpenChat = { roomId ->
                            com.muyeon.app.ui.chat.ChatActivity.startRoom(this, roomId)
                        },
                    )
                } else {
                    PersonalLessonManagementScreen(
                        quoteApi = api,
                        productApi = productApi,
                        roleLabel = if (role == "ACADEMY") "학원" else "강사",
                        onClose = { finish() },
                        onOpenSent = { sentDetail = it },
                        onAutoReply = { QuoteAutoTemplatesActivity.start(this) },
                        onCreateLesson = { com.muyeon.app.ui.lesson.LessonActivity.startCreate(this) },
                        onEditLesson = { id -> com.muyeon.app.ui.lesson.LessonActivity.startEdit(this, id) },
                        onSlots = { id -> com.muyeon.app.ui.lesson.LessonActivity.startSlots(this, id) },
                        onGoGenreSettings = { openWebAndFinish("/lessonGenres") },
                        // 레슨 설정(노출·장르)은 강사 전용 — 학원에겐 효과가 없는 화면이다.
                        onGoLessonSettings = if (role == "ACADEMY") null else {
                            { com.muyeon.app.ui.lesson.LessonActivity.startSettings(this) }
                        },
                    )
                }
            } else {
                QuoteDashboardScreen(
                    api = api,
                    title = "레슨 요청 허브",
                    role = "customer",
                    functions = customerFunctions(),
                    onClose = { finish() },
                    onTileAction = { id ->
                        when (id) {
                            "unread", "open" -> QuoteHubActivity.start(this, isPro = false, initialTab = 0)
                            "reservations", "done" -> openWebAndFinish("/myReservations")
                        }
                    },
                    onUpcomingTap = { openWebAndFinish("/myReservations") },
                )
            }
        }
    }

    /** 일반회원 — iOS presentCustomerQuoteDashboard functions 동일(그룹 없음 = 리스트). */
    //  무용수 유형은 '레슨 요청하기'를 숨긴다(지난 요청 내역·예약 내역 조회는 허용).
    private fun customerFunctions(): List<QuoteDashFunction> = listOfNotNull(
        QuoteDashFunction(Icons.Filled.Inbox, "레슨 요청 내역", "받은 제안 확인·채택", activityKey = "receivedQuotes") {
            QuoteHubActivity.start(this, isPro = false, initialTab = 0)
        },
        if (ActiveRole.isDancer(this)) null
        else QuoteDashFunction(Icons.Filled.EditNote, "레슨 요청하기", "새 견적 문진 작성") {
            QuoteWizardActivity.start(this, null, null)
        },
        QuoteDashFunction(Icons.Filled.CalendarMonth, "예약 내역", "공간·레슨·취소 내역") {
            openWebAndFinish("/myReservations")
        },
    )

    private fun openWebAndFinish(path: String) = NativeWebRoute.openWebAndFinish(this, path)
}
