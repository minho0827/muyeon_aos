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
    NavHost(navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = mainViewModel,
                onOpenDetail = { route -> navController.navigate(route) },
                onMyLongPress = onMyLongPress,
                onOpenWeb = onOpenWeb,
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
