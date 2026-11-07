package com.muyeon.app.ui.recruit

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.muyeon.app.R
import kotlinx.coroutines.flow.SharedFlow

/** 채용 탭 화면이 여는 곳 — 셸(MainScreen)이 채운다. 상세는 아직 웹 창(목록 먼저 앱으로 — 2026-11-08). */
class RecruitActions(
    val openPath: (String) -> Unit,
    val native: (String, Map<String, String>) -> Unit,
    val login: () -> Unit,
    /** 채용 안 위쪽 탭(구인 ↔ 구직). */
    val selectTab: (com.muyeon.app.ui.main.MainTab) -> Unit,
)

/** 찜 토스트·문구·로그인 — 세 목록이 같이 쓴다. */
@Composable
fun RecruitEventsEffect(events: SharedFlow<RecruitEvent>, actions: RecruitActions) {
    val context = LocalContext.current
    LaunchedEffect(events) {
        events.collect { e ->
            when (e) {
                is RecruitEvent.ScrapToast -> Toast.makeText(context, if (e.added) R.string.common_scrap_added else R.string.common_scrap_removed, Toast.LENGTH_SHORT).show()
                is RecruitEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
                RecruitEvent.LoginRequired -> actions.login()
            }
        }
    }
}
