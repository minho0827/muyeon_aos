package com.muyeon.app.ui.castingstage

import com.muyeon.app.ui.common.setEdgeContent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.muyeon.app.result.ResultActivity
import com.muyeon.app.result.launchScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.components.*
import com.muyeon.app.ui.castingstage.screens.*

/**
 * '캐스팅·무대' 네이티브 화면(10-26) — 웹 하단 탭 '캐스팅·무대'(openCastingStage). iOS CastingStageView 와 같은 구성.
 *  위에서 아래로: 상단바 → 자동 넘김 배너 → 아이콘 칸 11개 → 위에 붙는 글자 탭 → 섹션(가로 카드 + 전체보기) → 맨 위로.
 *  캐스팅·소식 상세, 전체보기 목록, 지난 호는 웹으로 넘기지 않고 화면 안 내비게이션(NavHost)으로 연다(10-26, CastingStageDetail.kt).
 */
class CastingStageActivity : ResultActivity() {
    companion object {
        private const val EXTRA_ROUTE = "route"

        /** route — 링크로 열 때 첫 화면 위에 쌓을 화면(linkRoute). null 이면 첫 화면. */
        fun start(context: Context, route: String? = null) =
            context.launchScreen(Intent(context, CastingStageActivity::class.java).putExtra(EXTRA_ROUTE, route))

        /**
         * 웹 common/nativeLink.js 의 openCastingStage payload → NavHost 경로(10-27, iOS StageRoute.initialPath 와 같다).
         *  castingId → 공고 상세 · screen=castingList·issues · issueNo(+category) → 그 호(그 칸) 소식. 이상한 값은 null(첫 화면).
         */
        fun linkRoute(castingId: String?, screen: String?, issueNo: String?, category: String?): String? {
            castingId?.toIntOrNull()?.takeIf { it > 0 }?.let { return "casting/$it" }
            when (screen) {
                "castingList" -> return "castingList"
                "issues" -> return "issues"
            }
            val no = issueNo?.toIntOrNull()?.takeIf { it > 0 } ?: return null
            return if (category != null && StageCategory.NEWS.any { it.code == category }) "newsList/$no/$category" else "issue/$no"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeContent {
            val nav = rememberNavController()
            com.muyeon.app.analytics.TrackNavigation("CastingStage", nav)
            val r = StageRoutes.ACTIVITY
            // 링크로 열었으면 첫 화면 위에 그 화면을 한 번 쌓는다 — 뒤로 가면 캐스팅·무대 첫 화면.
            val linkRoute = remember { if (savedInstanceState == null) intent.getStringExtra(EXTRA_ROUTE) else null }
            val t = com.muyeon.app.ui.common.nav.MuyeonNav
            NavHost(nav, startDestination = r.home, enterTransition = t.enter, exitTransition = t.exit, popEnterTransition = t.shellPopEnter, popExitTransition = t.shellPopExit) {
                castingStageGraph(nav, r, includeHome = true, onExit = { finish() },
                    onCreated = { addResultKeys(com.muyeon.app.result.ResultKeys.JOB_POSTINGS) })
            }
            androidx.compose.runtime.LaunchedEffect(Unit) { linkRoute?.let { nav.navigate(r.link(it)) } }
        }
    }
}
