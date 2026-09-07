package com.muyeon.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteNavBar
import kotlinx.coroutines.launch

/**
 * 차단한 사용자 목록 + 해제 — iOS `BlockedUsersView.swift` 1:1.
 *
 * ⚠️ 차단만 되고 푸는 곳이 없으면 실수로 차단한 상대를 영영 복구하지 못한다.
 *   설정(MY)이 웹이라, 차단이 일어나는 채팅 목록 우측 상단에 진입점을 둔다.
 */
@Composable
fun BlockedUsersScreen(api: ChatApi, onBack: () -> Unit) {
    var users by remember { mutableStateOf<List<BlockedUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // 실패해도 기존 목록은 유지 — 일시적 오류로 '차단한 사용자 없음'으로 위장하지 않는다.
        api.blockedUsers().onSuccess { users = it }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "차단한 사용자", onBack = onBack)

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            users.isEmpty() -> Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 30.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Outlined.DoNotDisturbOn, null, tint = MuyeonColors.secondary,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    "차단한 사용자가 없어요",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                Text(
                    "채팅방 우측 상단 ⋮ 에서 상대를 차단할 수 있어요.",
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                    color = MuyeonColors.secondary, textAlign = TextAlign.Center,
                )
            }
            else -> LazyColumn(Modifier.weight(1f)) {
                items(users.size, key = { i -> users[i].userId }) { i ->
                    val u = users[i]
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        QuoteAvatar(u.image, u.name, 48.dp)
                        Text(
                            u.name,
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                            lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                        )
                        Text(
                            "차단 해제",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            lineHeight = 16.sp, color = MuyeonColors.primary,
                            modifier = Modifier.clip(RoundedCornerShape(50))
                                .border(1.dp, MuyeonColors.border, RoundedCornerShape(50))
                                .clickable {
                                    scope.launch {
                                        api.unblockUser(u.userId)
                                            .onSuccess { users = users.filterNot { it.userId == u.userId } }
                                            .onFailure { toast = it.message ?: "해제하지 못했어요." }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                    HorizontalDivider(color = MuyeonColors.border, modifier = Modifier.padding(start = 76.dp))
                }
            }
        }
    }

    toast?.let { msg ->
        com.muyeon.app.ui.quote.QuoteDialog(
            "알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null },
        )
    }
}
