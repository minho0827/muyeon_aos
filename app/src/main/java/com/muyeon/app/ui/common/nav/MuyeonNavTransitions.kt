package com.muyeon.app.ui.common.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
    // ★ 2026-11-08 앱 셸(탭 위 화면)·캐스팅·무대는 PaceERA AppNavHost 와 같은 전환 — 들어갈 때도 밀려 들어온다
    //  (페이드 300ms + 가로 밀기). 기존 화면 NavHost 는 pop 두 개만 계속 쓴다.
    private val pace = 300

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { fadeIn(tween(pace)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(pace)) }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { fadeOut(tween(pace)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(pace)) }

    val shellPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { fadeIn(tween(pace)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(pace)) }

    val shellPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { fadeOut(tween(pace)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(pace)) }

    /** 하단 탭끼리 — PaceERA MainScreen 안쪽 NavHost 와 같은 짧은 페이드(180ms). */
    val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { fadeIn(tween(180)) }
    val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { fadeOut(tween(180)) }

    private val spec = tween<IntOffset>(300, easing = FastOutSlowInEasing)

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { slideInHorizontally(spec) { -it * 3 / 10 } }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { slideOutHorizontally(spec) { it } }
}
