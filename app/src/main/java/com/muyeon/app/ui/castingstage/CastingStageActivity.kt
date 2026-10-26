package com.muyeon.app.ui.castingstage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.muyeon.app.result.ResultActivity
import com.muyeon.app.result.launchScreen
import com.muyeon.app.utils.TokenManager
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
        fun start(context: Context) = context.launchScreen(Intent(context, CastingStageActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val api = remember { CastingStageApi(TokenManager.getAccessToken(this)) }
            val nav = rememberNavController()
            com.muyeon.app.analytics.TrackNavigation("CastingStage", nav)
            // 소식 상세는 목록에서 받은 값을 그대로 넘긴다(소식 하나만 주는 API 가 없다).
            val newsCache = remember { mutableMapOf<Int, Pair<StageNewsItem, StageCategory>>() }
            val openUrl: (String) -> Unit = { url -> runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
            fun openNews(item: StageNewsItem, cat: StageCategory) { newsCache[item.id] = item to cat; nav.navigate("news/${item.id}") }
            fun back() { if (!nav.popBackStack()) finish() }
            NavHost(nav, startDestination = "home") {
                composable("home") {
                    CastingStageScreen(
                        api = api,
                        onClose = { finish() },
                        onOpenCasting = { nav.navigate("casting/$it") },
                        onOpenNews = ::openNews,
                        onCastingList = { nav.navigate("castingList") },
                        onNewsList = { no, code -> nav.navigate("newsList/$no/$code") },
                        onIssues = { nav.navigate("issues") },
                    )
                }
                composable("casting/{id}", listOf(navArgument("id") { type = NavType.IntType })) { e ->
                    StageCastingDetailScreen(api, e.arguments?.getInt("id") ?: 0, onBack = ::back, onOpenUrl = openUrl,
                        onWriteResume = { com.muyeon.app.ui.resume.ResumeActivity.startEdit(this@CastingStageActivity, null, "dancer") })
                }
                composable("news/{id}", listOf(navArgument("id") { type = NavType.IntType })) { e ->
                    val hit = newsCache[e.arguments?.getInt("id") ?: 0]
                    if (hit != null) StageNewsDetailScreen(hit.first, hit.second, onBack = ::back, onOpenUrl = openUrl) else back()
                }
                composable("castingList") { StageCastingListScreen(api, onBack = ::back, onOpen = { nav.navigate("casting/$it") }) }
                composable("newsList/{no}/{code}", listOf(navArgument("no") { type = NavType.IntType })) { e ->
                    StageNewsListScreen(api, e.arguments?.getInt("no") ?: 0, e.arguments?.getString("code"), onBack = ::back, onOpen = ::openNews)
                }
                composable("issues") { StageIssuesScreen(api, onBack = ::back, onOpen = { nav.navigate("issue/$it") }) }
                composable("issue/{no}", listOf(navArgument("no") { type = NavType.IntType })) { e ->
                    StageNewsListScreen(api, e.arguments?.getInt("no") ?: 0, null, onBack = ::back, onOpen = ::openNews)
                }
            }
        }
    }
}
