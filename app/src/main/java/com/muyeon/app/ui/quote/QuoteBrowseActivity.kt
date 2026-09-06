package com.muyeon.app.ui.quote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.NativeWebRoute

/**
 * 견적 모아보기(강사) 풀스크린 — 웹 `openQuoteBrowse` 브릿지로 진입.
 *  iOS 는 WebViewModel.presentQuoteBrowse 로 QuoteBrowseView 를 모달 표시.
 */
class QuoteBrowseActivity : ComponentActivity() {

    companion object {
        fun start(context: Context) {
            val i = Intent(context, QuoteBrowseActivity::class.java)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val api = remember { QuoteApi(TokenManager.getAccessToken(this)) }
            QuoteBrowseScreen(
                api = api,
                onClose = { finish() },
                // 전공 등록 / 견적 수신 조건 — 네이티브 미이식 화면이라 웹 경로로(브릿지 폴백과 동일).
                onGoGenreSettings = { openWebAndFinish("/lessonGenres") },
                onGoLessonSettings = { com.muyeon.app.ui.lesson.LessonActivity.startSettings(this) },
            )
        }
    }

    private fun openWebAndFinish(path: String) = NativeWebRoute.openWebAndFinish(this, path)
}
