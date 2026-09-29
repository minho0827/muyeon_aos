package com.muyeon.app.ui.quote

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.result.OnResultKeys
import com.muyeon.app.result.ResultActivity
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.result.ReturnResultKeys
import com.muyeon.app.result.ResultLauncher
import com.muyeon.app.result.launchScreen
import com.muyeon.app.result.rememberResultLauncher
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.NativeWebRoute

/**
 * 견적 허브 — iOS `QuoteHubView.swift` + 하위 상세 화면을 담는 컨테이너.
 *  iOS 는 NavigationView push, Android 는 navigation-compose NavHost 로 동일한 스택을 만든다.
 *  웹 `openMyQuotes` 브릿지로 진입(iOS presentMyQuotes 대응).
 *
 *  ⚠️ 채팅(ChatRoomView)·공개프로필(PublicProfileView)은 아직 이식 전 —
 *   프로필은 웹 경로 폴백, 채팅은 웹에도 화면이 없어 안내 토스트(AppBridgeInterface 폴백 규약과 동일).
 *
 *  결과: QUOTES·RESERVATIONS(채택·취소·삭제로 요청·예약이 바뀔 수 있다).
 *  NavHost 안: 받은 요청 상세 → 목록 복귀 시 QUOTES 로 목록 재조회 / 상세 ← 채팅 복귀 시 상세 재조회.
 */
class QuoteHubActivity : ResultActivity() {

    override val defaultResultKeys = setOf(ResultKeys.QUOTES, ResultKeys.RESERVATIONS)

    companion object {
        private const val EXTRA_IS_PRO = "isPro"
        private const val EXTRA_TAB = "tab"
        private const val EXTRA_QUOTE_ID = "quoteId"
        private const val EXTRA_RESPONSE_ID = "responseId"

        /**
         * @param quoteId 지정 시 목록을 건너뛰고 그 요청의 상세부터 표시(알림 딥링크).
         * @param responseId 알림으로 방금 온 견적 강조(iOS highlightResponseId).
         */
        fun intent(
            context: Context, isPro: Boolean, initialTab: Int = 0, quoteId: Int? = null, responseId: Int? = null,
        ): Intent = Intent(context, QuoteHubActivity::class.java)
            .putExtra(EXTRA_IS_PRO, isPro)
            .putExtra(EXTRA_TAB, initialTab)
            .putExtra(EXTRA_QUOTE_ID, quoteId ?: 0)
            .putExtra(EXTRA_RESPONSE_ID, responseId ?: 0)

        fun start(context: Context, isPro: Boolean, initialTab: Int = 0, quoteId: Int? = null, responseId: Int? = null) =
            context.launchScreen(intent(context, isPro, initialTab, quoteId, responseId))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isPro = intent.getBooleanExtra(EXTRA_IS_PRO, false)
        // 보낸 견적(강사측)은 강사·학원 전용 — 무용수 유형이면 안내 후 종료. 받은 견적(고객측) 조회는 허용.
        if (isPro && !ActiveRole.allowLessonProvider(this)) { finish(); return }
        val initialTab = intent.getIntExtra(EXTRA_TAB, 0)
        val deepQuoteId = intent.getIntExtra(EXTRA_QUOTE_ID, 0)
        val deepResponseId = intent.getIntExtra(EXTRA_RESPONSE_ID, 0).takeIf { it > 0 }

        setContent {
            val nav = rememberNavController()
            val token = remember { TokenManager.getAccessToken(this) }
            val api = remember { QuoteApi(token) }
            // 보낸견적 상세는 목록 항목을 통째로 넘긴다(iOS SentQuoteDetailView(item:) 동일).
            //  SentQuoteItem 은 Parcelable 이 아니라 라우트 인자 대신 홀더로 전달.
            var sentDetail by remember { mutableStateOf<SentQuoteItem?>(null) }

            // 딥링크(알림)로 quoteId 가 오면 상세부터 — 뒤로가면 허브 목록.
            val start = if (deepQuoteId > 0) "received/$deepQuoteId" else "hub"

            NavHost(nav, startDestination = start) {
                composable("hub") { entry ->
                    // 상세에서 채택·취소·삭제하고 돌아오면 목록을 다시 읽는다(결과 키 QUOTES).
                    //  rememberSaveable 이라 상세에 가 있는 동안에도 값이 살아 있다.
                    var hubReload by rememberSaveable { mutableIntStateOf(0) }
                    entry.OnResultKeys { keys -> if (ResultKeys.QUOTES in keys) hubReload++ }
                    QuoteHubScreen(
                        api = api,
                        isPro = isPro,
                        initialTab = initialTab,
                        onClose = { finish() },
                        onOpenReceived = { quoteId -> nav.navigate("received/$quoteId") },
                        onOpenSent = { item -> sentDetail = item; nav.navigate("sent") },
                        onOpenRecommendBlocks = { nav.navigate("recommendBlocks") },
                        reloadSignal = hubReload,
                    )
                }
                composable("recommendBlocks") {
                    RecommendBlocksScreen(
                        api = api,
                        onBack = { if (!nav.popBackStack()) finish() },
                        onOpenTeacher = ::openTeacherProfile,
                    )
                }
                composable("received/{quoteId}") { entry ->
                    nav.ReturnResultKeys(entry, ResultKeys.QUOTES)
                    val quoteId = entry.arguments?.getString("quoteId")?.toIntOrNull() ?: 0
                    // 채팅(채택·바로채팅)에서 돌아오면 상세(응답 상태·채택 여부)를 다시 읽는다.
                    var detailReload by rememberSaveable { mutableIntStateOf(0) }
                    val chatLauncher = rememberResultLauncher { detailReload++ }
                    ReceivedQuoteDetailScreen(
                        api = api,
                        quoteId = quoteId,
                        onBack = { if (!nav.popBackStack()) finish() },
                        onOpenProfile = ::openTeacherProfile,
                        onOpenChat = { roomId -> openChat(roomId, chatLauncher) },
                        highlightResponseId = deepResponseId,
                        reloadSignal = detailReload,
                    )
                }
                composable("sent") {
                    val item = sentDetail
                    if (item == null) nav.popBackStack()
                    else SentQuoteDetailScreen(
                        api = api,
                        item = item,
                        onBack = { nav.popBackStack() },
                        onOpenChat = { roomId -> openChat(roomId, null) },
                    )
                }
            }
        }
    }

    /** 강사 공개 프로필 — 네이티브 미이식이라 웹 경로로(브릿지 openPublicProfile 폴백과 동일 경로). */
    private fun openTeacherProfile(userId: Int) {
        openWebAndFinish("/teachers/$userId")
    }

    /** 채팅방 — 네이티브 이식 완료(B). 채택/바로채팅이 실제 방으로 이어진다. launcher 가 있으면 결과를 받는다. */
    private fun openChat(roomId: Int, launcher: ResultLauncher?) {
        if (roomId <= 0) {
            Toast.makeText(this, "채팅방을 여는 데 실패했어요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (launcher != null) launcher.launch(ChatActivity.roomIntent(this, roomId))
        else ChatActivity.startRoom(this, roomId)
    }

    /** 웹 화면으로 이동하고 허브 종료. */
    private fun openWebAndFinish(path: String) = NativeWebRoute.openWebAndFinish(this, path)
}
