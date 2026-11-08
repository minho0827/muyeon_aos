package com.muyeon.app.ui.quote

import com.muyeon.app.ui.common.setEdgeContent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.muyeon.app.result.ResultActivity
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.result.launchScreen
import com.muyeon.app.result.rememberResultLauncher
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.NativeWebRoute
import com.muyeon.app.ui.quote.respond.QuoteRespondHost

/**
 * 견적 모아보기(강사) 풀스크린 — 웹 `openQuoteBrowse` 브릿지로 진입.
 *  iOS 는 WebViewModel.presentQuoteBrowse 로 QuoteBrowseView 를 모달 표시.
 *
 *  결과: QUOTES·QUOTE_PREFS(제안 발송·수신 조건 변경).
 */
class QuoteBrowseActivity : ResultActivity() {

    override val defaultResultKeys = setOf(ResultKeys.QUOTES, ResultKeys.QUOTE_PREFS)

    companion object {
        fun intent(context: Context): Intent = Intent(context, QuoteBrowseActivity::class.java)

        fun start(context: Context) = context.launchScreen(intent(context))

        // ★ 묶음 5 — 요청 하나(웹 /availableQuotes/:id)를 바로 연다: 요청 상세 · 견적 보내기(QuoteRespondScreen).
        private const val EXTRA_QUOTE_ID = "quoteId"
        private const val EXTRA_COMPOSE = "compose"

        /** @param compose 가입·로그인하고 돌아옴 — 요청이 열려 있으면 작성 시트를 바로 연다(웹 초안 quoteRespond:id). */
        fun requestIntent(context: Context, quoteId: Int, compose: Boolean = false): Intent =
            intent(context).putExtra(EXTRA_QUOTE_ID, quoteId).putExtra(EXTRA_COMPOSE, compose)

        fun startRequest(context: Context, quoteId: Int, compose: Boolean = false) =
            context.launchScreen(requestIntent(context, quoteId, compose))
    }

    /** 다른 화면(채팅방·유형 추가·인증)에서 돌아오면 요청·내 견적 상태를 다시 읽는다(웹 useResultRefresh). */
    private var respondReload by mutableIntStateOf(0)

    override fun onRestart() {
        super.onRestart()
        respondReload++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val quoteId = intent.getIntExtra(EXTRA_QUOTE_ID, 0)
        if (quoteId > 0) {
            // 요청 하나 — 비회원·일반회원·무용수도 공개 상세를 본다(웹과 같다). 유형 게이트는 화면 안에서.
            val compose = intent.getBooleanExtra(EXTRA_COMPOSE, false)
            setEdgeContent { QuoteRespondHost(this, quoteId, compose, respondReload) }
            return
        }
        // 견적 요청 둘러보기(응답)는 강사·학원 전용 — 무용수 유형이면 안내 후 종료(2026-09-26 정책).
        if (!ActiveRole.allowLessonProvider(this)) { finish(); return }
        setEdgeContent {
            val api = remember { QuoteApi(TokenManager.getAccessToken(this)) }
            // 레슨 설정(견적 수신 조건)에서 돌아오면 바뀐 조건으로 다시 읽는다.
            var prefsReload by rememberSaveable { mutableIntStateOf(0) }
            val settingsLauncher = rememberResultLauncher { keys ->
                if (ResultKeys.QUOTE_PREFS in keys) prefsReload++
            }
            QuoteBrowseScreen(
                api = api,
                onClose = { finish() },
                // 전공 등록 / 견적 수신 조건 — 네이티브 미이식 화면이라 웹 경로로(브릿지 폴백과 동일).
                onGoGenreSettings = { openWebAndFinish("/lessonGenres") },
                onGoLessonSettings = {
                    settingsLauncher.launch(com.muyeon.app.ui.lesson.LessonActivity.settingsIntent(this))
                },
                reloadSignal = prefsReload,
            )
        }
    }

    private fun openWebAndFinish(path: String) = NativeWebRoute.openWebAndFinish(this, path)
}
