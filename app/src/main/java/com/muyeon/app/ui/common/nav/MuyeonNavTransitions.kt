package com.muyeon.app.ui.common.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

/**
 * ★ 2026-11-04 화면을 빠져나올 때(뒤로 가기·닫기)만 iOS 내비게이션처럼 — 지금 화면은 오른쪽으로 밀려 나가고,
 *  뒤 화면은 왼쪽에서 30% 만큼 따라 들어온다(기본값 페이드 대신). 들어갈 때(navigate)는 기본 전환 그대로.
 *  모든 NavHost 에 popEnterTransition = MuyeonNav.popEnter, popExitTransition = MuyeonNav.popExit 로 건다.
 *  액티비티를 닫을 때는 ResultActivity.finish 가 같은 모양(res/anim/chat_slide_*)을 쓴다.
 */
object MuyeonNav {
    private val spec = tween<IntOffset>(300, easing = FastOutSlowInEasing)

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { slideInHorizontally(spec) { -it * 3 / 10 } }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { slideOutHorizontally(spec) { it } }
}
