package com.muyeon.app.ui.quote

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.muyeon.app.result.ResultActivity
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.result.launchScreen
import com.muyeon.app.result.rememberResultLauncher
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.ui.lesson.LessonActivity
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
 *
 * 결과: QUOTES·LESSONS. 자식 화면(허브·위저드·레슨·자동응답·채팅)은 결과 런처로 띄워
 *  돌아오면 그 키에 해당하는 목록만 다시 읽는다(LaunchedEffect 재실행에 기대지 않는다).
 */
class QuoteDashboardActivity : ResultActivity() {

    override val defaultResultKeys = setOf(ResultKeys.QUOTES, ResultKeys.LESSONS)

    companion object {
        private const val EXTRA_ROLE = "role"          // TEACHER | ACADEMY | (그 외 = 일반회원)

        fun intent(context: Context, role: String?): Intent =
            Intent(context, QuoteDashboardActivity::class.java).putExtra(EXTRA_ROLE, role ?: "")

        fun start(context: Context, role: String?) = context.launchScreen(intent(context, role))
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

                // 탭별 재조회 신호 — 자식 결과 키를 보고 해당 탭만 올린다(타이머·재진입 의존 없음).
                var lessonReload by rememberSaveable { mutableIntStateOf(0) }
                var prefsReload by rememberSaveable { mutableIntStateOf(0) }
                var consultReload by rememberSaveable { mutableIntStateOf(0) }

                // 레슨 개설·수정·예약시간·레슨 설정 → 레슨 소개(LESSONS) · 수강생 찾기 수신 조건(QUOTE_PREFS)
                val lessonLauncher = rememberResultLauncher { keys ->
                    if (ResultKeys.LESSONS in keys) lessonReload++
                    if (ResultKeys.QUOTE_PREFS in keys) prefsReload++
                }
                // 자동응답 템플릿 — 활성 템플릿이 바뀌면 자동 제안이 상담 목록에 쌓인다.
                //  (이 화면엔 자동응답 켜짐 표시가 따로 없어 상담 탭을 다시 읽는 것으로 맞춘다.)
                val autoLauncher = rememberResultLauncher { keys ->
                    if (ResultKeys.AUTO_QUOTE in keys) consultReload++
                }
                // 보낸 제안 → 채팅: 대화·채택 상태가 바뀌었을 수 있다 → 상담 목록.
                val chatLauncher = rememberResultLauncher { consultReload++ }

                val detail = sentDetail
                if (detail != null) {
                    SentQuoteDetailScreen(
                        api = api,
                        item = detail,
                        onBack = { sentDetail = null },
                        onOpenChat = { roomId -> chatLauncher.launch(ChatActivity.roomIntent(this, roomId)) },
                    )
                } else {
                    PersonalLessonManagementScreen(
                        quoteApi = api,
                        productApi = productApi,
                        roleLabel = if (role == "ACADEMY") "학원" else "강사",
                        onClose = { finish() },
                        onOpenSent = { sentDetail = it },
                        onAutoReply = { autoLauncher.launch(QuoteAutoTemplatesActivity.intent(this)) },
                        onCreateLesson = { lessonLauncher.launch(LessonActivity.createIntent(this)) },
                        onEditLesson = { id -> lessonLauncher.launch(LessonActivity.editIntent(this, id)) },
                        onSlots = { id -> lessonLauncher.launch(LessonActivity.slotsIntent(this, id)) },
                        onGoGenreSettings = { openWebAndFinish("/lessonGenres") },
                        // 레슨 설정(노출·장르)은 강사 전용 — 학원에겐 효과가 없는 화면이다.
                        onGoLessonSettings = if (role == "ACADEMY") null else {
                            { lessonLauncher.launch(LessonActivity.settingsIntent(this)) }
                        },
                        lessonReload = lessonReload,
                        prefsReload = prefsReload,
                        consultReload = consultReload,
                        // 수강생 찾기에서 제안을 보내면 상담 탭(보낸 제안)에 곧바로 보여야 한다 — 같은 화면 안 신호.
                        onResponded = { consultReload++; addResultKeys(ResultKeys.QUOTES) },
                        onLessonsChanged = { addResultKeys(ResultKeys.LESSONS) },
                    )
                }
            } else {
                // 허브(받은 제안)·위저드(새 요청)에서 돌아오면 요약 타일·다가오는 일정을 다시 읽는다.
                var dashReload by rememberSaveable { mutableIntStateOf(0) }
                val quoteLauncher = rememberResultLauncher { keys ->
                    if (ResultKeys.QUOTES in keys || ResultKeys.RESERVATIONS in keys) dashReload++
                }
                QuoteDashboardScreen(
                    api = api,
                    title = "레슨 요청 허브",
                    role = "customer",
                    functions = customerFunctions { quoteLauncher.launch(it) },
                    onClose = { finish() },
                    onTileAction = { id ->
                        when (id) {
                            "unread", "open" -> quoteLauncher.launch(
                                QuoteHubActivity.intent(this, isPro = false, initialTab = 0),
                            )
                            "reservations", "done" -> openWebAndFinish("/myReservations")
                        }
                    },
                    onUpcomingTap = { openWebAndFinish("/myReservations") },
                    reloadSignal = dashReload,
                )
            }
        }
    }

    /** 일반회원 — iOS presentCustomerQuoteDashboard functions 동일(그룹 없음 = 리스트). */
    //  무용수 유형은 '레슨 요청하기'를 숨긴다(지난 요청 내역·예약 내역 조회는 허용).
    //  launch = 결과 런처 — 돌아오면 대시보드가 다시 읽는다.
    private fun customerFunctions(launch: (Intent) -> Unit): List<QuoteDashFunction> = listOfNotNull(
        QuoteDashFunction(Icons.Filled.Inbox, "레슨 요청 내역", "받은 제안 확인·채택", activityKey = "receivedQuotes") {
            launch(QuoteHubActivity.intent(this, isPro = false, initialTab = 0))
        },
        if (ActiveRole.isDancer(this)) null
        else QuoteDashFunction(Icons.Filled.EditNote, "레슨 요청하기", "새 견적 문진 작성") {
            launch(QuoteWizardActivity.intent(this, null, null))
        },
        QuoteDashFunction(Icons.Filled.CalendarMonth, "예약 내역", "공간·레슨·취소 내역") {
            openWebAndFinish("/myReservations")
        },
    )

    private fun openWebAndFinish(path: String) = NativeWebRoute.openWebAndFinish(this, path)
}
