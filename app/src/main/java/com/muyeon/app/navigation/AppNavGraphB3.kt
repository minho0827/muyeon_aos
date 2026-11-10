package com.muyeon.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.muyeon.app.result.ResultKeys

/**
 * ★ 2026-11-08 묶음 3 — 탭 위 앱 화면: 대타공고 수정 · 커뮤니티 글쓰기/수정. AppNavHost 가 한 줄로 붙인다.
 *   저장 뒤 이동 — 대타 수정: 그 공고 상세(웹 /subs/:id), 새 글: 커뮤니티 목록(웹 /community, 작성 화면은 기록에 남기지 않는다), 글 수정: 그 글 상세.
 */
internal fun NavGraphBuilder.b3Graph(navController: NavHostController) {
    composable(Screen.SubEdit.route, arguments = listOf(navArgument(Screen.SubEdit.ARG_ID) { type = NavType.StringType })) { e ->
        val id = e.arguments?.getString(Screen.SubEdit.ARG_ID)?.toLongOrNull() ?: 0L
        com.muyeon.app.ui.subwizard.SubWizardScreen(
            editId = id,
            onClose = { navController.popBackStack() },
            onCreated = {
                AppResults.publish(setOf(ResultKeys.JOB_POSTINGS))
                // 그 공고 상세에서 열었으면 거기로 돌아간다(상세가 다시 읽는다) — 아니면 상세로 바꾼다.
                val prev = navController.previousBackStackEntry
                if (prev?.destination?.route == Screen.SubDetail.route && prev.arguments?.getString("id") == id.toString()) navController.popBackStack()
                else navController.navigate(Screen.SubDetail.build(id)) { popUpTo(Screen.SubEdit.route) { inclusive = true } }
            },
        )
    }
    composable(
        Screen.CommunityWrite.route,
        arguments = listOf(navArgument(Screen.CommunityWrite.ARG_EDIT) { type = NavType.StringType; nullable = true; defaultValue = null }),
    ) { e ->
        val editId = e.arguments?.getString(Screen.CommunityWrite.ARG_EDIT)?.toLongOrNull()
        com.muyeon.app.ui.communitywrite.CommunityWriteScreen(
            editId = editId,
            onBack = { navController.popBackStack() },
            onDone = {
                // 글 수정 — 고친 글 상세로 돌아간다(상세는 다시 보일 때 새로 읽는다). 웹은 목록(/community)으로 가지만 iOS 와 같게 상세로.
                // 새 글 — 웹 navigate("/community", { replace: true }): 아래에 커뮤니티 목록이 있으면 거기로(결과 키로 새로 받는다), 없으면 목록으로 바꾼다.
                if (editId != null) navController.popBackStack()
                else if (!navController.popBackStack(Screen.CommunityList.route, inclusive = false)) {
                    navController.navigate(Screen.CommunityList.route) { popUpTo(Screen.CommunityWrite.route) { inclusive = true } }
                }
            },
        )
    }
}
