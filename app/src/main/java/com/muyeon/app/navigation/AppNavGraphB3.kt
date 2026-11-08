package com.muyeon.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.muyeon.app.result.ResultKeys

/**
 * ★ 2026-11-08 묶음 3 — 탭 위 앱 화면: 대타공고 수정 · 커뮤니티 글쓰기/수정. AppNavHost 가 한 줄로 붙인다.
 *   저장 뒤 이동은 웹과 같다 — 대타 수정: 그 공고 상세(/subs/:id), 글쓰기·글 수정: 커뮤니티 목록(/community, 작성 화면은 기록에 남기지 않는다).
 */
internal fun NavGraphBuilder.b3Graph(navController: NavHostController) {
    composable(Screen.SubEdit.route, arguments = listOf(navArgument(Screen.SubEdit.ARG_ID) { type = NavType.StringType })) { e ->
        val id = e.arguments?.getString(Screen.SubEdit.ARG_ID)?.toLongOrNull() ?: 0L
        com.muyeon.app.ui.subwizard.SubWizardScreen(
            editId = id,
            onClose = { navController.popBackStack() },
            onCreated = {
                AppResults.publish(setOf(ResultKeys.JOB_POSTINGS))
                navController.navigate(Screen.SubDetail.build(id)) { popUpTo(Screen.SubEdit.route) { inclusive = true } }
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
                // 웹: navigate("/community", { replace: true }) — 아래에 커뮤니티 목록이 있으면 거기까지 돌아가고(목록은 결과 키로 새로 받는다), 없으면 목록으로 바꾼다.
                if (!navController.popBackStack(Screen.CommunityList.route, inclusive = false)) {
                    navController.navigate(Screen.CommunityList.route) { popUpTo(Screen.CommunityWrite.route) { inclusive = true } }
                }
            },
        )
    }
}
