package com.muyeon.app.ui.academy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import kotlinx.coroutines.launch

/**
 * [학원] 소속 강사 관리 — iOS `AcademyTeachersView.swift` 1:1.
 *  우리 학원 코드(복사/재발급) + 소속 신청 승인·거절 + 소속 해지.
 *  웹 폴백: muyeon_front pages/muyeon/my/AcademyTeachers.js
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademyTeachersScreen(api: AcademyTeacherApi, onClose: () -> Unit) {
    var rows by remember { mutableStateOf<List<AcademyTeacher>>(emptyList()) }
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // 되돌릴 수 없는 동작은 한 번 더 묻는다(웹 ConfirmModal 과 동일 정책).
    var confirm by remember { mutableStateOf<AcademyConfirm?>(null) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val waiting = rows.filter { AcademyTeacherStatus.isWaiting(it.status) }
    val active = rows.filter { AcademyTeacherStatus.isActive(it.status) }

    suspend fun load() {
        loading = rows.isEmpty()
        rows = api.mine().getOrDefault(emptyList())
        if (code.isEmpty()) code = api.myCode().getOrDefault("")
        loading = false
    }

    /** 단건 처리 후 목록 재조회 — 성공 문구는 웹과 동일하게 맞춘다. */
    fun run(done: String, op: suspend () -> Result<Unit>) {
        if (busy) return
        busy = true
        scope.launch {
            op().onSuccess {
                infoMessage = done
                load()
                // 웹 MY 화면(소속 배지)도 갱신 — iOS presentAcademyFull dismiss 콜백과 동일.
                com.muyeon.app.webview.WebCallbacks.academyChanged(ctx)
            }.onFailure { errorMessage = it.message }
            busy = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "소속 강사 관리", onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
        } else {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp).padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // 우리 학원 코드 — 강사에게 알려주면 강사가 이 코드로 신청한다.
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MuyeonColors.primary.copy(alpha = 0.08f)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "우리 학원 코드",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textHead,
                        )
                        Text(
                            "이 코드를 강사님께 알려주세요. 강사가 코드로 신청하면 아래에서 승인할 수 있어요.",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textSub,
                        )
                        Text(
                            code.ifEmpty { "코드 없음" },
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                            lineHeight = 24.sp, color = MuyeonColors.primary, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(MuyeonColors.surface).padding(vertical = 14.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AcademyActionButton("코드 복사", filled = true, enabled = code.isNotEmpty()) {
                                copyToClipboard(ctx, code)
                                infoMessage = "코드를 복사했어요."
                            }
                            AcademyActionButton("재발급", enabled = !busy && code.isNotEmpty()) {
                                confirm = AcademyConfirm.Reissue
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AcademySectionHead("승인 대기", waiting.size)
                        if (waiting.isEmpty()) {
                            AcademyEmptyText("대기 중인 신청이 없어요.")
                        } else {
                            waiting.forEach { r ->
                                AcademyPersonCard(r) {
                                    // 승인/거절은 강사가 신청(REQUESTED)한 건에만 뜬다.
                                    //  INVITED 는 강사 수락 대기라 학원이 할 게 없다(iOS 동일).
                                    if (r.status == "REQUESTED") {
                                        AcademyActionButton("승인", filled = true, enabled = !busy) {
                                            run("소속을 승인했습니다.") { api.approve(r.id) }
                                        }
                                        AcademyActionButton("거절", destructive = true, enabled = !busy) {
                                            confirm = AcademyConfirm.Deny(r)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AcademySectionHead("소속 강사", active.size)
                        if (active.isEmpty()) {
                            AcademyEmptyText("아직 소속 강사가 없어요.\n코드를 공유해 신청을 받아보세요.")
                        } else {
                            active.forEach { r ->
                                AcademyPersonCard(r) {
                                    AcademyActionButton("소속 해지", destructive = true, enabled = !busy) {
                                        confirm = AcademyConfirm.Leave(r)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    confirm?.let { c ->
        val (title, message, confirmText) = when (c) {
            AcademyConfirm.Reissue -> Triple(
                "코드를 재발급할까요?",
                "이전 코드는 즉시 사용할 수 없어요. 이미 승인된 소속은 그대로 유지됩니다.",
                "재발급",
            )
            is AcademyConfirm.Deny -> Triple("신청을 거절할까요?", "강사님께 거절 알림이 전달돼요.", "거절")
            is AcademyConfirm.Leave -> Triple("소속을 해지할까요?", "이미 등록된 레슨은 그대로 유지돼요.", "해지")
            else -> Triple("", "", "")
        }
        QuoteDialog(
            title = title, message = message, confirmText = confirmText,
            onConfirm = {
                confirm = null
                when (c) {
                    AcademyConfirm.Reissue -> {
                        if (!busy) {
                            busy = true
                            scope.launch {
                                api.reissueCode()
                                    .onSuccess { code = it; infoMessage = "새 코드를 발급했습니다." }
                                    .onFailure { errorMessage = it.message }
                                busy = false
                            }
                        }
                    }
                    is AcademyConfirm.Deny -> run("신청을 거절했습니다.") { api.deny(c.row.id) }
                    is AcademyConfirm.Leave -> run("소속을 해지했습니다.") { api.leave(c.row.id) }
                    else -> Unit
                }
            },
            onDismiss = { confirm = null },
        )
    }
    infoMessage?.let { msg ->
        QuoteDialog("안내", msg, "확인", onConfirm = { infoMessage = null }, onDismiss = { infoMessage = null })
    }
    errorMessage?.let { msg ->
        QuoteDialog("오류", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

/** 확인 다이얼로그 대상 — iOS `Confirm` enum 대응(두 화면 공용). */
sealed class AcademyConfirm {
    data object Reissue : AcademyConfirm()
    data class Deny(val row: AcademyTeacher) : AcademyConfirm()
    data class Leave(val row: AcademyTeacher) : AcademyConfirm()
    data class Cancel(val row: AcademyTeacher) : AcademyConfirm()
}

internal fun copyToClipboard(context: Context, text: String) {
    if (text.isEmpty()) return
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("muyeon", text))
}
