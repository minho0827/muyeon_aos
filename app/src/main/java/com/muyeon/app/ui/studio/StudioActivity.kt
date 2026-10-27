package com.muyeon.app.ui.studio

import com.muyeon.app.result.launchScreen
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.jobposting.PostingConfirmDialog
import com.muyeon.app.ui.jobposting.PostingInfoDialog
import com.muyeon.app.ui.membership.MembershipActivity
import com.muyeon.app.ui.membership.MembershipApi
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.utils.TokenManager

/**
 * 스튜디오 운영 컨테이너 — 웹 `openStudioOps` / `openStudioMembers` / `openStudioSales` /
 *  `openStudioSchedule` 브릿지 진입점. iOS `WebViewModel+Hub.presentStudioOps` 대응.
 */
class StudioActivity : com.muyeon.app.result.ResultActivity() {

    /** 결과: STUDIO_MEMBERS(회원·수강권이 바뀌었을 수 있다). */
    override val defaultResultKeys = setOf(com.muyeon.app.result.ResultKeys.STUDIO_MEMBERS)

    companion object {
        private const val EXTRA_ROUTE = "route"

        fun intent(context: Context, route: String): Intent =
            Intent(context, StudioActivity::class.java).putExtra(EXTRA_ROUTE, route)

        fun start(context: Context, route: String) = context.launchScreen(intent(context, route))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: "hub"

        setContent {
            // 학원 운영 도구는 프로 멤버십 전용(2026-10-07). 서버는 회원·수강권·매출 API 를 막지만
            //  일정 API 는 레슨 달력과 공용이라 막지 않는다 → 화면 진입 자체를 여기서 막는다.
            //  iOS StudioProGate 대응. null=확인 중.
            var gate by remember { mutableStateOf<StudioGate?>(null) }
            LaunchedEffect(Unit) {
                gate = MembershipApi(TokenManager.getAccessToken(this@StudioActivity)).myMembership().fold(
                    onSuccess = { if (it.active && it.tier == "PRO") StudioGate.ALLOWED else StudioGate.DENIED },
                    onFailure = { StudioGate.FAILED },
                )
            }
            when (gate) {
                StudioGate.ALLOWED -> StudioNav(route)
                StudioGate.DENIED -> PostingConfirmDialog(
                    title = "프로 멤버십 전용",
                    message = STUDIO_PRO_MESSAGE,
                    confirmText = "멤버십 보기",
                    dismissText = "다음에",
                    onConfirm = { MembershipActivity.start(this@StudioActivity); finish() },
                    onDismiss = { finish() },
                )
                StudioGate.FAILED -> PostingInfoDialog(
                    title = "안내",
                    message = "멤버십 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.",
                    onDismiss = { finish() },
                )
                null -> Box(Modifier.fillMaxSize().background(MuyeonColors.surface))
            }
        }
    }

    @Composable
    private fun StudioNav(route: String) {
        val nav = rememberNavController()
        com.muyeon.app.analytics.TrackNavigation("Studio", nav)
        val api = remember { StudioApi(TokenManager.getAccessToken(this)) }

        fun back() { if (!nav.popBackStack()) finish() }

        NavHost(nav, startDestination = route) {
            composable("hub") {
                StudioHub(
                    onClose = { finish() },
                    onMembers = { nav.navigate("members") },
                    onSales = { nav.navigate("sales") },
                    onSchedule = { nav.navigate("schedule") },
                )
            }
            composable("members") {
                StudioMembersScreen(api, onClose = { back() }, onOpenMember = { id -> nav.navigate("member/$id") })
            }
            composable("member/{id}") { e ->
                StudioMemberDetailScreen(api, e.arguments?.getString("id")?.toIntOrNull() ?: 0, onClose = { back() })
            }
            composable("sales") { StudioSalesScreen(api, onClose = { back() }) }
            composable("schedule") { StudioScheduleScreen(api, onClose = { back() }) }
        }
    }
}

private enum class StudioGate { ALLOWED, DENIED, FAILED }

private const val STUDIO_PRO_MESSAGE = "학원 운영 도구(수강생·수강권·매출·시간표)는 프로 멤버십에서 이용할 수 있어요."

/** 스튜디오 운영 허브 — iOS HubGridView(items 3종) 대응. */
@Composable
private fun StudioHub(
    onClose: () -> Unit,
    onMembers: () -> Unit,
    onSales: () -> Unit,
    onSchedule: () -> Unit,
) {
    // 10-27 목록 화면 틀(ui/common/MuyeonListKit) — ✕ + 왼쪽 굵은 제목, 회색 바탕 위 바로가기 카드 하나(티켓 앱 '고객센터' 모양).
    Column(Modifier.fillMaxSize().background(com.muyeon.app.ui.common.MuyeonList.pageBg)) {
        com.muyeon.app.ui.common.MuyeonListHeader("스튜디오 운영", onClose = onClose)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            com.muyeon.app.ui.common.MuyeonSectionTitle("운영 도구")
            com.muyeon.app.ui.common.MuyeonShortcutCard(
                listOf(
                    com.muyeon.app.ui.common.MuyeonShortcut(Icons.Filled.Groups, "회원 관리", onClick = onMembers),
                    com.muyeon.app.ui.common.MuyeonShortcut(Icons.Filled.Paid, "매출 현황", onClick = onSales),
                    com.muyeon.app.ui.common.MuyeonShortcut(Icons.Filled.CalendarMonth, "일정 관리", onClick = onSchedule),
                ),
            )
        }
    }
}

