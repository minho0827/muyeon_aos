package com.muyeon.app.ui.floating

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * ★ 2026-11-23 학원 인증 이어하기 상태 — 모든 화면의 '학원 인증 마무리하기' 플로팅이 같은 값을 읽는다.
 *  탭 셸·웹 창은 FloatingOverlay 가 인증 상태를 읽을 때 함께 채우고([update]),
 *  그 밖의 앱 화면(setEdgeContent)은 화면이 앞으로 올 때마다 직접 다시 읽는다([refresh]).
 */
object ResumeFloatingStore {
    /** 마지막으로 읽은 인증 상태. null = 아직 모름 또는 로그아웃. */
    var status by mutableStateOf<VerifyStatus?>(null)
        private set

    private val mutex = Mutex()
    /** 읽는 중에 다시 읽어 달라는 신호가 오면 끝난 뒤 한 번 더 읽는다(최신 값 유실 방지). */
    @Volatile private var again = false

    fun update(v: VerifyStatus?) { status = v }

    suspend fun refresh(api: FloatingApi) {
        if (!api.isLoggedIn) { status = null; return }
        if (!mutex.tryLock()) { again = true; return }
        try {
            do {
                again = false
                api.verificationStatus()?.let { status = it }
            } while (again)
        } finally {
            mutex.unlock()
        }
    }
}

/**
 * 화면별 '여기서는 띄우지 않기' 신호 — 인증 화면 자신·가입 온보딩·인증 차단 화면에서 쓴다.
 *  액티비티마다 따로 둔다(전역 값이면 뒤에 깔린 화면의 신호가 앞 화면까지 가린다).
 */
class ResumeFloatingSuppress {
    var count by mutableIntStateOf(0)
}

val LocalResumeFloatingSuppress = staticCompositionLocalOf<ResumeFloatingSuppress?> { null }

/** 이 composable 이 화면에 있는 동안 이 액티비티의 이어하기 플로팅을 숨긴다. */
@Composable
fun SuppressResumeFloating() {
    val holder = LocalResumeFloatingSuppress.current ?: return
    DisposableEffect(holder) {
        holder.count += 1
        onDispose { holder.count -= 1 }
    }
}

/**
 * '학원 인증 마무리하기' 플로팅 — 오른쪽 위(위 바 아래). 닫기 버튼은 없다.
 *  탭하면 학원 인증 화면을 이어하기로 연다(입력해 둔 값을 채우고 첫 빈 칸으로 이동).
 *
 * 숨기는 경우: 로그아웃 · 이어할 내용 없음 · 심사중(책갈피가 대신 뜬다, 둘이 같이 뜨지 않는다) ·
 *  웹 모달 · 인증 전 화면 · 인증 화면 자신(SuppressResumeFloating) · 키보드 표시 중 · 인증 강제 차단 중.
 *
 * @param selfRefresh true = 화면이 앞으로 올 때·재조회 신호마다 직접 읽는다(setEdgeContent 화면).
 *   false = FloatingOverlay 가 읽어 [ResumeFloatingStore] 를 채운다(탭 셸·웹 창).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ResumeVerificationFloating(api: FloatingApi, selfRefresh: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    if (selfRefresh) {
        LifecycleResumeEffect(api) {
            scope.launch { ResumeFloatingStore.refresh(api) }
            onPauseOrDispose { }
        }
        // 첫 값은 위 화면 복귀 효과가 읽는다 — 재조회 신호(소켓 알림·임시 저장 등)는 그 뒤부터만.
        var firstTick by remember { mutableStateOf(true) }
        LaunchedEffect(FloatingState.refreshTick) {
            if (firstTick) { firstTick = false; return@LaunchedEffect }
            ResumeFloatingStore.refresh(api)
        }
    }

    val status = ResumeFloatingStore.status
    val resume = status?.resume
    val suppressed = (LocalResumeFloatingSuppress.current?.count ?: 0) > 0
    val show = api.isLoggedIn && resume != null && resume.role == "ACADEMY" &&
        status.status != "PENDING" &&
        !FloatingState.floatingHidden && !FloatingState.isAuthRoute &&
        !suppressed && !WindowInsets.isImeVisible &&
        !com.muyeon.app.webview.VerifyGate.gated
    if (!show || resume == null) return

    // ★ 11-23 사용자 요청: 위아래로 천천히 움직여 눈에 띄게(6dp, 1.2초 왕복) · 탭 줄을 가리지 않게 조금 아래로(상태바 + 140dp).
    val bob by androidx.compose.animation.core.rememberInfiniteTransition(label = "resumeBob").animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "resumeBobY",
    )
    Box(Modifier.fillMaxSize()) {
        val shape = RoundedCornerShape(12.dp)
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 140.dp, end = 12.dp)
                .offset { androidx.compose.ui.unit.IntOffset(0, bob.dp.roundToPx()) }
                .shadow(6.dp, shape, spotColor = Color.Black.copy(alpha = 0.18f))
                .clip(shape)
                .background(Color.White)
                .border(1.5.dp, MuyeonColors.primary, shape)
                .clickable {
                    com.muyeon.app.ui.onboarding.OnboardingActivity.startVerification(
                        context, "ACADEMY", ownerName = "", completeInApp = true, resume = true,
                    )
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // ★ 11-23 사용자 요청 — '다음: …' 줄은 빼고 이모티콘 한 줄로.
            Text(
                "학원 인증 마무리해 주세요 🙏",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.primary,
            )
        }
    }
}
