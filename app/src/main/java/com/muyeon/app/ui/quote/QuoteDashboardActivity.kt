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
import com.muyeon.app.utils.TokenManager
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

        setContent {
            val token = remember { TokenManager.getAccessToken(this) }
            val api = remember { QuoteApi(token) }
            val autoApi = remember { AutoQuoteApi(token) }

            if (isPro) {
                // 자동응답 켜짐/꺼짐을 먼저 조회해 카드 뱃지로 표시(진입 전 상태 확인). 실패해도 화면은 그대로.
                val autoOn by produceState(initialValue = false, autoApi) {
                    value = autoApi.activeId() != null
                }
                QuoteDashboardScreen(
                    api = api,
                    title = if (role == "ACADEMY") "레슨 관리 (학원)" else "레슨 관리 (강사)",
                    role = "teacher",
                    functions = proFunctions(role, autoOn),
                    onClose = { finish() },
                    onTileAction = { id ->
                        when (id) {
                            "today", "pending" -> openWebAndFinish("/lessonCalendar")
                            "new" -> QuoteBrowseActivity.start(this)
                            "sent", "accepted" -> QuoteHubActivity.start(this, isPro = true, initialTab = 1)
                        }
                    },
                    onUpcomingTap = { item ->
                        item.lessonId?.let { openWebAndFinish("/lessons/$it") }
                    },
                )
            } else {
                QuoteDashboardScreen(
                    api = api,
                    title = "견적 허브",
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

    /** 강사·학원 — "레슨 관리"/"견적 관리" 2섹션(iOS presentLessonQuoteHub items 순서 동일). */
    private fun proFunctions(role: String, autoOn: Boolean): List<QuoteDashFunction> {
        val gLesson = "레슨 관리"
        val gQuote = "견적 관리"
        val list = mutableListOf(
            QuoteDashFunction(Icons.AutoMirrored.Filled.Note, "내 레슨 관리", "개설·수정·삭제", gLesson) { openWebAndFinish("/myLessons") },
            QuoteDashFunction(Icons.Filled.CreateNewFolder, "레슨 개설", "새 레슨 등록", gLesson) { openWebAndFinish("/lessons/create") },
            QuoteDashFunction(Icons.Filled.CalendarMonth, "예약 가능 시간 관리", "요일·시간·정원", gLesson) { openWebAndFinish("/myLessons") },
            QuoteDashFunction(Icons.Filled.Inbox, "받은 견적 확인하기", "받은 견적·채택", gQuote) {
                QuoteHubActivity.start(this, isPro = true, initialTab = 0)
            },
            QuoteDashFunction(Icons.AutoMirrored.Filled.Send, "보낸 견적 확인하기", "보낸 견적 현황", gQuote) {
                QuoteHubActivity.start(this, isPro = true, initialTab = 1)
            },
            QuoteDashFunction(
                Icons.Outlined.ChatBubbleOutline, "자동응답 설정", "자동 메시지 관리", gQuote,
                badge = if (autoOn) "켜짐" else "꺼짐", badgeOn = autoOn,
            ) { QuoteAutoTemplatesActivity.start(this) },
        )
        // 레슨 설정(강사프로필 관리 + 레슨 탭 노출 토글)은 강사 전용 — 학원 유형에겐 효과가 없는 화면.
        if (role != "ACADEMY") {
            list.add(QuoteDashFunction(Icons.Filled.Settings, "레슨 설정", "노출·장르", gLesson) { com.muyeon.app.ui.lesson.LessonActivity.startSettings(this) })
        }
        return list
    }

    /** 일반회원 — iOS presentCustomerQuoteDashboard functions 동일(그룹 없음 = 리스트). */
    private fun customerFunctions(): List<QuoteDashFunction> = listOf(
        QuoteDashFunction(Icons.Filled.Inbox, "견적 요청 내역", "받은 견적 확인·채택") {
            QuoteHubActivity.start(this, isPro = false, initialTab = 0)
        },
        QuoteDashFunction(Icons.Filled.EditNote, "레슨 요청하기", "새 견적 문진 작성") {
            QuoteWizardActivity.start(this, null, null)
        },
        QuoteDashFunction(Icons.Filled.CalendarMonth, "예약 내역", "공간·레슨·취소 내역") {
            openWebAndFinish("/myReservations")
        },
    )

    private fun openWebAndFinish(path: String) = NativeWebRoute.openWebAndFinish(this, path)
}
