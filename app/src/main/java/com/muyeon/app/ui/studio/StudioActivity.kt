package com.muyeon.app.ui.studio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.utils.TokenManager

/**
 * 스튜디오 운영 컨테이너 — 웹 `openStudioOps` / `openStudioMembers` / `openStudioSales` /
 *  `openStudioSchedule` 브릿지 진입점. iOS `WebViewModel+Hub.presentStudioOps` 대응.
 */
class StudioActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ROUTE = "route"

        fun start(context: Context, route: String) {
            val i = Intent(context, StudioActivity::class.java).putExtra(EXTRA_ROUTE, route)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: "hub"

        setContent {
            val nav = rememberNavController()
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
}

/** 스튜디오 운영 허브 — iOS HubGridView(items 3종) 대응. */
@Composable
private fun StudioHub(
    onClose: () -> Unit,
    onMembers: () -> Unit,
    onSales: () -> Unit,
    onSchedule: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "스튜디오 운영", onClose = onClose)
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HubCard(Icons.Filled.Groups, "회원 관리", "회원·수강권", Modifier.weight(1f), onMembers)
                HubCard(Icons.Filled.Paid, "매출 현황", "수강권·상품 매출", Modifier.weight(1f), onSales)
                HubCard(Icons.Filled.CalendarMonth, "일정 관리", "수업·개인일정", Modifier.weight(1f), onSchedule)
            }
        }
    }
}

@Composable
private fun HubCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.heightIn(min = 108.dp).clip(RoundedCornerShape(14.dp)).background(MuyeonColors.groupedBg)
            .clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MuyeonColors.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MuyeonColors.primary, modifier = Modifier.size(22.dp))
        }
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead, textAlign = TextAlign.Center,
        )
        Text(
            subtitle,
            fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp,
            color = MuyeonColors.textSub, textAlign = TextAlign.Center,
        )
    }
}
