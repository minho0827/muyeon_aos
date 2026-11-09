package com.muyeon.app.ui.castingstage

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.muyeon.app.ui.castingstage.data.CastingStageApi
import com.muyeon.app.ui.castingstage.model.StageCategory
import com.muyeon.app.ui.castingstage.model.StageNewsItem
import com.muyeon.app.ui.castingstage.screens.CastingStageScreen
import com.muyeon.app.ui.castingstage.screens.StageCastingDetailScreen
import com.muyeon.app.ui.castingstage.screens.StageCastingListScreen
import com.muyeon.app.ui.castingstage.screens.StageIssuesScreen
import com.muyeon.app.ui.castingstage.screens.StageNewsDetailScreen
import com.muyeon.app.ui.castingstage.screens.StageNewsListScreen
import com.muyeon.app.utils.TokenManager

/**
 * ★ 2026-11-08 캐스팅·무대 화면 묶음 — 앱 셸(MainActivity) 바깥 NavHost 와 CastingStageActivity 가 같이 쓴다.
 *   셸에서는 첫 화면이 하단 탭(Screen.TabCasting)이고 상세·목록은 탭 위 화면(prefix "stage/")으로 밀려 들어온다.
 *   액티비티(예전 진입 — 다른 네이티브 화면에서 링크)는 prefix "" 로 첫 화면까지 같이 쓴다.
 */
class StageRoutes(private val prefix: String) {
    val home = "${prefix}home"
    val casting = "${prefix}casting/{id}"
    val news = "${prefix}news/{id}"
    val castingList = "${prefix}castingList"
    val create = "${prefix}create"
    val newsList = "${prefix}newsList/{no}/{code}"
    val issues = "${prefix}issues"
    val issue = "${prefix}issue/{no}"

    fun casting(id: Int) = "${prefix}casting/$id"
    fun news(id: Int) = "${prefix}news/$id"
    fun newsList(no: Int, code: String) = "${prefix}newsList/$no/$code"
    fun issue(no: Int) = "${prefix}issue/$no"

    /** [CastingStageActivity.linkRoute] 값(prefix 없음) → 이 묶음 경로. */
    fun link(route: String) = prefix + route

    /** 경로가 이 묶음 화면인가(채팅 버튼 숨김 등). */
    fun owns(route: String?) = prefix.isNotEmpty() && route?.startsWith(prefix) == true

    companion object {
        /** 앱 셸 바깥 NavHost 용. */
        val SHELL = StageRoutes("stage/")
        /** CastingStageActivity 안 NavHost 용(예전 경로 그대로). */
        val ACTIVITY = StageRoutes("")
    }
}

/** 화면 사이에 같이 쓰는 값 — 소식 상세는 목록에서 받은 값을 그대로 넘긴다(소식 하나만 주는 API 가 없다). */
object CastingStageSession {
    val newsCache = mutableMapOf<Int, Pair<StageNewsItem, StageCategory>>()
    /** 공고를 새로 올리면 올라간다 — 첫 화면·전체 목록이 다시 불러온다. */
    var reloadKey by mutableIntStateOf(0)
    /** 분류 목록(서버 · 못 받으면 기본) — 본문이 받아 두면 목록·배너·링크가 같이 쓴다. */
    var categories by androidx.compose.runtime.mutableStateOf(StageCategory.FALLBACK)
}

@Composable
fun rememberStageApi(): CastingStageApi {
    val context = LocalContext.current
    return remember { CastingStageApi(TokenManager.getAccessToken(context)) }
}

/** 공연팀으로 활동 중일 때만 ＋(공연 공고 등록) — 웹 canAs(activeType, "casting.create") 와 같다. */
@Composable
fun stageCanCreate(): Boolean {
    val context = LocalContext.current
    return com.muyeon.app.webview.ActiveRole.current(context) == "TEAM"
}

fun NavController.openStageNews(r: StageRoutes, item: StageNewsItem, cat: StageCategory) {
    CastingStageSession.newsCache[item.id] = item to cat
    navigate(r.news(item.id))
}

/** 캐스팅·무대 첫 화면. [onClose] null 이면 뒤로 버튼 없이(하단 탭). */
@Composable
fun CastingStageHome(nav: NavController, r: StageRoutes, onClose: (() -> Unit)?) {
    val canCreate = stageCanCreate()
    CastingStageScreen(
        api = rememberStageApi(),
        onClose = onClose,
        onOpenCasting = { nav.navigate(r.casting(it)) },
        onOpenNews = { item, cat -> nav.openStageNews(r, item, cat) },
        onCastingList = { nav.navigate(r.castingList) },
        onNewsList = { no, code -> nav.navigate(r.newsList(no, code)) },
        onIssues = { nav.navigate(r.issues) },
        onCreate = if (canCreate) ({ nav.navigate(r.create) }) else null,
        reloadKey = CastingStageSession.reloadKey,
    )
}

/**
 * 첫 화면([includeHome])·상세·목록·등록. [onExit] 더 돌아갈 화면이 없을 때(액티비티 닫기).
 *  [onCreated] 공고를 올렸을 때 결과 알림(셸: AppResults · 액티비티: 결과 키).
 */
fun NavGraphBuilder.castingStageGraph(
    nav: NavController,
    r: StageRoutes,
    includeHome: Boolean,
    onExit: () -> Unit,
    onCreated: () -> Unit,
) {
    val back: () -> Unit = { if (!nav.popBackStack()) onExit() }
    if (includeHome) composable(r.home) { CastingStageHome(nav, r, onClose = back) }
    composable(r.casting, listOf(navArgument("id") { type = NavType.IntType })) { e ->
        val context = LocalContext.current
        StageCastingDetailScreen(rememberStageApi(), e.arguments?.getInt("id") ?: 0, onBack = back, onOpenUrl = { openUrl(context, it) },
            onWriteResume = { com.muyeon.app.ui.resume.ResumeActivity.startEdit(context, null, "dancer") })
    }
    composable(r.news, listOf(navArgument("id") { type = NavType.IntType })) { e ->
        val context = LocalContext.current
        val hit = CastingStageSession.newsCache[e.arguments?.getInt("id") ?: 0]
        if (hit != null) StageNewsDetailScreen(hit.first, hit.second, onBack = back, onOpenUrl = { openUrl(context, it) })
        else LaunchedEffect(Unit) { back() }
    }
    composable(r.castingList) {
        val canCreate = stageCanCreate()
        StageCastingListScreen(rememberStageApi(), onBack = back, onOpen = { nav.navigate(r.casting(it)) },
            onCreate = if (canCreate) ({ nav.navigate(r.create) }) else null, reloadKey = CastingStageSession.reloadKey)
    }
    // 공연 공고 등록(공연팀) — 등록하면 방금 올린 공고 상세로 바꿔 띄운다(웹은 캐스팅 화면으로 간다).
    composable(r.create) {
        val context = LocalContext.current
        com.muyeon.app.ui.castingstage.create.CastingCreateScreen(
            api = rememberStageApi(),
            onClose = back,
            onCreated = { id ->
                CastingStageSession.reloadKey++
                onCreated()
                nav.popBackStack()
                if (id != null) nav.navigate(r.casting(id))
            },
            onOpenMembership = { com.muyeon.app.ui.membership.MembershipActivity.start(context) },
            onOpenMyPostings = { com.muyeon.app.ui.jobposting.JobPostingActivity.startList(context) },
        )
    }
    composable(r.newsList, listOf(navArgument("no") { type = NavType.IntType })) { e ->
        StageNewsListScreen(rememberStageApi(), e.arguments?.getInt("no") ?: 0, e.arguments?.getString("code"), onBack = back,
            onOpen = { item, cat -> nav.openStageNews(r, item, cat) })
    }
    composable(r.issues) { StageIssuesScreen(rememberStageApi(), onBack = back, onOpen = { nav.navigate(r.issue(it)) }) }
    composable(r.issue, listOf(navArgument("no") { type = NavType.IntType })) { e ->
        StageNewsListScreen(rememberStageApi(), e.arguments?.getInt("no") ?: 0, null, onBack = back,
            onOpen = { item, cat -> nav.openStageNews(r, item, cat) })
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
