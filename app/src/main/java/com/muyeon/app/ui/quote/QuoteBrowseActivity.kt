package com.muyeon.app.ui.quote

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 견적 요청 둘러보기(응답)는 강사·학원 전용 — 무용수 유형이면 안내 후 종료(2026-09-26 정책).
        if (!ActiveRole.allowLessonProvider(this)) { finish(); return }
        setContent {
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
