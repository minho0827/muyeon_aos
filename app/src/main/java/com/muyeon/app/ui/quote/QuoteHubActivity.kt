package com.muyeon.app.ui.quote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.NativeWebRoute

/**
 * 견적 허브 — iOS `QuoteHubView.swift` + 하위 상세 화면을 담는 컨테이너.
 *  iOS 는 NavigationView push, Android 는 navigation-compose NavHost 로 동일한 스택을 만든다.
 *  웹 `openMyQuotes` 브릿지로 진입(iOS presentMyQuotes 대응).
 *
 *  ⚠️ 채팅(ChatRoomView)·공개프로필(PublicProfileView)은 아직 이식 전 —
 *   프로필은 웹 경로 폴백, 채팅은 웹에도 화면이 없어 안내 토스트(AppBridgeInterface 폴백 규약과 동일).
 */
class QuoteHubActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_IS_PRO = "isPro"
        private const val EXTRA_TAB = "tab"
        private const val EXTRA_QUOTE_ID = "quoteId"
        private const val EXTRA_RESPONSE_ID = "responseId"

        /**
         * @param quoteId 지정 시 목록을 건너뛰고 그 요청의 상세부터 표시(알림 딥링크).
         * @param responseId 알림으로 방금 온 견적 강조(iOS highlightResponseId).
         */
        fun start(context: Context, isPro: Boolean, initialTab: Int = 0, quoteId: Int? = null, responseId: Int? = null) {
            val i = Intent(context, QuoteHubActivity::class.java)
                .putExtra(EXTRA_IS_PRO, isPro)
                .putExtra(EXTRA_TAB, initialTab)
                .putExtra(EXTRA_QUOTE_ID, quoteId ?: 0)
                .putExtra(EXTRA_RESPONSE_ID, responseId ?: 0)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isPro = intent.getBooleanExtra(EXTRA_IS_PRO, false)
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
                composable("hub") {
                    QuoteHubScreen(
                        api = api,
                        isPro = isPro,
                        initialTab = initialTab,
                        onClose = { finish() },
                        onOpenReceived = { quoteId -> nav.navigate("received/$quoteId") },
                        onOpenSent = { item -> sentDetail = item; nav.navigate("sent") },
                    )
                }
                composable("received/{quoteId}") { entry ->
                    val quoteId = entry.arguments?.getString("quoteId")?.toIntOrNull() ?: 0
                    ReceivedQuoteDetailScreen(
                        api = api,
                        quoteId = quoteId,
                        onBack = { if (!nav.popBackStack()) finish() },
                        onOpenProfile = ::openTeacherProfile,
                        onOpenChat = ::openChat,
                        highlightResponseId = deepResponseId,
                    )
                }
                composable("sent") {
                    val item = sentDetail
                    if (item == null) nav.popBackStack()
                    else SentQuoteDetailScreen(
                        api = api,
                        item = item,
                        onBack = { nav.popBackStack() },
                        onOpenChat = ::openChat,
                    )
                }
            }
        }
    }

    /** 강사 공개 프로필 — 네이티브 미이식이라 웹 경로로(브릿지 openPublicProfile 폴백과 동일 경로). */
    private fun openTeacherProfile(userId: Int) {
        openWebAndFinish("/teachers/$userId")
    }

    /** 채팅방 — 네이티브·웹 모두 미이식. 무반응 금지 규약에 따라 안내만. */
    private fun openChat(@Suppress("UNUSED_PARAMETER") roomId: Int) {
        Toast.makeText(this, "채팅은 곧 제공될 기능이에요.", Toast.LENGTH_SHORT).show()
    }

    /** 웹 화면으로 이동하고 허브 종료. */
    private fun openWebAndFinish(path: String) = NativeWebRoute.openWebAndFinish(this, path)
}
