package com.muyeon.app.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavHostController

/**
 * Activity 안 NavHost 의 화면 이동을 이용 행동 로그에 남긴다 — 'native:Chat.room' 처럼(Analytics.subScreen).
 *  Activity 단위 화면은 Analytics 가 자동으로 잡으므로, 한 Activity 안에서 단계가 나뉘는 흐름만 이걸 붙인다.
 */
@Composable
fun TrackNavigation(owner: String, nav: NavHostController) {
    DisposableEffect(nav) {
        val listener = NavController.OnDestinationChangedListener { _, dest, _ -> Analytics.subScreen(owner, dest.route) }
        nav.addOnDestinationChangedListener(listener)
        onDispose { nav.removeOnDestinationChangedListener(listener) }
    }
}
