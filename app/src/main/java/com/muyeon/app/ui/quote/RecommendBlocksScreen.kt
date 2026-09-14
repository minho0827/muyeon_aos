package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
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
import kotlinx.coroutines.launch

/**
 * 추천에서 제외한 강사 목록 + 해제 — 채팅 `BlockedUsersScreen` 과 같은 꼴.
 *
 * ⚠️ 제외만 되고 푸는 곳이 없으면 실수로 누른 사용자가 영영 되돌리지 못한다.
 *   실제로 이 기능은 등록(POST)만 있고 해제 API 자체가 없는 채로 배포돼 있었다.
 *   제외가 일어나는 곳(추천 카드 ⋮)과 같은 화면 계층인 견적 허브 상단에 진입점을 둔다.
 *
 * 채팅 '차단한 사용자'와는 다른 목록이다 — 차단=안전(괴롭힘), 제외=취향(피드 튜닝).
 */
@Composable
fun RecommendBlocksScreen(api: QuoteApi, onBack: () -> Unit, onOpenTeacher: (Int) -> Unit) {
    var teachers by remember { mutableStateOf<List<RecommendBlockedTeacher>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // 실패해도 기존 목록은 유지 — 일시적 오류로 '제외한 강사 없음'으로 위장하지 않는다.
        api.recommendBlocks().onSuccess { teachers = it }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "추천 제외한 강사", onBack = onBack)

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            teachers.isEmpty() -> Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 30.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Outlined.VisibilityOff, null, tint = MuyeonColors.secondary,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    "추천에서 제외한 강사가 없어요",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                Text(
                    "추천 강사 카드 ⋮ 에서 '다시는 추천받지 않기'를 고를 수 있어요.",
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                    color = MuyeonColors.secondary, textAlign = TextAlign.Center,
                )
            }
            else -> LazyColumn(Modifier.weight(1f)) {
                items(teachers.size, key = { i -> teachers[i].teacherId }) { i ->
                    val t = teachers[i]
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        QuoteAvatar(t.image, t.name, 48.dp)
                        Text(
                            t.name,
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                            lineHeight = 18.sp, color = MuyeonColors.textHead,
                            modifier = Modifier.weight(1f).clickable { onOpenTeacher(t.teacherId) },
                        )
                        Text(
                            "해제",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            lineHeight = 16.sp, color = MuyeonColors.primary,
                            modifier = Modifier.clip(RoundedCornerShape(50))
                                .border(1.dp, MuyeonColors.border, RoundedCornerShape(50))
                                .clickable {
                                    scope.launch {
                                        api.unblockRecommendation(t.teacherId)
                                            .onSuccess { teachers = teachers.filterNot { x -> x.teacherId == t.teacherId } }
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
        QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}
