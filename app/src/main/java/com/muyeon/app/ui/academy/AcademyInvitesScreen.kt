package com.muyeon.app.ui.academy

import com.muyeon.app.result.findActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import kotlinx.coroutines.launch

/**
 * [강사] 학원 소속 신청/관리 — iOS `AcademyInvitesView.swift` 1:1.
 *  학원 코드로 신청 + 대기 중 신청 취소 + 소속 해지.
 *  웹 폴백: muyeon_front pages/muyeon/my/AcademyInvites.js
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademyInvitesScreen(api: AcademyTeacherApi, onClose: () -> Unit) {
    var rows by remember { mutableStateOf<List<AcademyTeacher>>(emptyList()) }
    var codeInput by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<AcademyConfirm?>(null) }
    val ctx = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val requested = rows.filter { it.status == "REQUESTED" }
    val active = rows.filter { AcademyTeacherStatus.isActive(it.status) }
    val canSubmit = !busy && codeInput.trim().isNotEmpty()

    suspend fun load() {
        loading = rows.isEmpty()
        rows = api.invites().getOrDefault(emptyList())
        loading = false
    }

    fun run(done: String, clearCode: Boolean = false, op: suspend () -> Result<Unit>) {
        if (busy) return
        busy = true
        scope.launch {
            op().onSuccess {
                infoMessage = done
                if (clearCode) codeInput = ""
                load()
                // 결과 키(ACADEMY)로 웹 소속 배지를 갱신 — 닫힐 때 WebViewActivity 가 한 번 보낸다.
                (ctx.findActivity() as? com.muyeon.app.result.ResultActivity)
                    ?.addResultKeys(com.muyeon.app.result.ResultKeys.ACADEMY)
            }.onFailure { errorMessage = it.message }
            busy = false
        }
    }

    LaunchedEffect(Unit) { load() }

    // 10-27 목록 화면 틀(ui/common/MuyeonListKit) — ‹ + 왼쪽 굵은 제목, 회색 바탕 위 흰 카드(iOS 와 같다).
    Column(Modifier.fillMaxSize().background(com.muyeon.app.ui.common.MuyeonList.pageBg)) {
        com.muyeon.app.ui.common.MuyeonListHeader("학원 소속", onBack = onClose)

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
                        .padding(horizontal = 16.dp).padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth().clip(com.muyeon.app.ui.common.MuyeonList.cardShape)
                            .background(MuyeonColors.surface).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "학원 코드로 신청",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textHead,
                        )
                        Text(
                            "학원에서 받은 코드를 입력해 주세요. 학원이 승인하면 소속돼요.",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textSub,
                        )
                        OutlinedTextField(
                            value = codeInput,
                            // 코드는 대문자 계약이라 입력도 대문자로 고정한다(iOS textInputAutocapitalization(.characters)).
                            onValueChange = { codeInput = it.uppercase() },
                            placeholder = {
                                Text(
                                    "예) MUYEON-A3K9F7",
                                    fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.chevron,
                                )
                            },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row {
                            AcademyActionButton(
                                if (busy) "신청 중…" else "소속 신청",
                                filled = true, enabled = canSubmit,
                            ) {
                                keyboard?.hide()
                                run("소속을 신청했어요. 학원이 승인하면 소속돼요.", clearCode = true) {
                                    api.joinByCode(codeInput.trim())
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AcademySectionHead("승인 대기 중", requested.size)
                        if (requested.isEmpty()) {
                            AcademyEmptyText("대기 중인 신청이 없어요.")
                        } else {
                            requested.forEach { r ->
                                AcademyPersonCard(r) {
                                    AcademyActionButton("신청 취소", destructive = true, enabled = !busy) {
                                        confirm = AcademyConfirm.Cancel(r)
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AcademySectionHead("소속 학원", active.size)
                        if (active.isEmpty()) {
                            AcademyEmptyText("소속된 학원이 없어요.\n학원 코드를 입력해 신청해보세요.")
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
            is AcademyConfirm.Cancel -> Triple(
                "신청을 취소할까요?", "다시 신청하려면 코드를 새로 입력해야 해요.", "신청 취소",
            )
            is AcademyConfirm.Leave -> Triple("소속을 해지할까요?", "이미 등록된 레슨은 그대로 유지돼요.", "해지")
            else -> Triple("", "", "")
        }
        QuoteDialog(
            title = title, message = message, confirmText = confirmText,
            onConfirm = {
                confirm = null
                when (c) {
                    is AcademyConfirm.Cancel -> run("신청을 취소했어요.") { api.cancel(c.row.id) }
                    is AcademyConfirm.Leave -> run("소속을 해지했어요.") { api.leave(c.row.id) }
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
