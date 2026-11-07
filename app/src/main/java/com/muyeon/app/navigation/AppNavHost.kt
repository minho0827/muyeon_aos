package com.muyeon.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.muyeon.app.ui.main.MainScreen
import com.muyeon.app.ui.performance.PerformanceDetailScreen
import com.muyeon.app.ui.community.CommunityPostScreen
import com.muyeon.app.result.findActivity
import com.muyeon.app.ui.main.MainViewModel

/**
 * 앱 셸 바깥 NavHost — 탭 셸([Screen.Main]) + 탭 위 상세(탭바 없음). PaceERA AppNavHost 와 같은 자리.
 *   상세 화면은 각 탭 이식 때 여기 한 줄씩 붙는다(커뮤니티 글 상세 · 공연 상세).
 */
@Composable
fun AppNavHost(
    mainViewModel: MainViewModel,
    onMyLongPress: () -> Unit,
    onOpenWeb: (String) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // 푸시·링크·웹 창에서 온 상세 열기 — 같은 화면(같은 인자)이 이미 맨 위면 다시 쌓지 않는다.
    androidx.compose.runtime.LaunchedEffect(navController) {
        mainViewModel.routes.collect { route ->
            val top = navController.currentBackStackEntry
            val topRoute = top?.destination?.route?.let { pattern ->
                top.arguments?.keySet()?.fold(pattern) { acc, k -> acc.replace("{$k}", top.arguments?.get(k)?.toString().orEmpty()) } ?: pattern
            }
            if (topRoute != route) navController.navigate(route)
        }
    }
    // 플로팅(채팅 버튼) 표시는 웹 주소 기준 — 탭 위 상세에 들어가면 상세 주소를 알려 숨기고, 탭으로 돌아오면 탭 주소.
    androidx.compose.runtime.LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route
            if (route == Screen.Main.route) {
                com.muyeon.app.ui.main.MainTab.fromKey(mainViewModel.state.value.selectedKey)
                    ?.let { com.muyeon.app.ui.floating.FloatingState.updateRoute(it.webPath) }
            } else {
                com.muyeon.app.ui.floating.FloatingState.updateRoute("/" + (route ?: "detail"))
            }
        }
    }
    NavHost(navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = mainViewModel,
                onOpenDetail = { route -> navController.navigate(route) },
                onMyLongPress = onMyLongPress,
                onOpenWeb = onOpenWeb,
                onNativeAction = { action, data ->
                    context.findActivity()?.let { a ->
                        com.muyeon.app.webview.NativeActionRouter.open(a, action, org.json.JSONObject(data as Map<*, *>))
                    }
                },
            )
        }
        composable(
            Screen.CommunityPost.route,
            arguments = listOf(navArgument(Screen.CommunityPost.ARG_ID) { type = NavType.StringType }),
        ) {
            CommunityPostScreen(
                onBack = { navController.popBackStack() },
                onLogin = { id -> onOpenWeb(WebPaths.login("/community/post/$id")) },
                onEditPost = { id -> onOpenWeb("/community/post/$id/edit") },
                onOpenTeacher = { userId ->
                    context.findActivity()?.let { a ->
                        com.muyeon.app.webview.NativeActionRouter.open(a, "openPublicProfile", org.json.JSONObject().put("userId", userId.toString()))
                    }
                },
            )
        }
        composable(
            Screen.PerformanceDetail.route,
            arguments = listOf(navArgument(Screen.PerformanceDetail.ARG_ID) { type = NavType.StringType }),
        ) {
            PerformanceDetailScreen(
                onBack = { navController.popBackStack() },
                onLogin = { onOpenWeb(WebPaths.login("/performance")) },
            )
        }
    }
}
