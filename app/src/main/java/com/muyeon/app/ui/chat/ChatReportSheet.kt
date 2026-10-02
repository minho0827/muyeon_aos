package com.muyeon.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * 채팅방 신고 — iOS `ChatReportSheet.swift` 1:1.
 *  POST /reports (targetType=CHATROOM, targetId=roomId).
 *  message 를 넘기면 메시지 단위 신고 (targetType=CHAT_MESSAGE, targetId=메시지 id).
 *  사유 코드는 웹/관리자 신고 관리 화면 라벨과 1:1로 맞춘다(변경 금지).
 */
private val REPORT_REASONS = listOf(
    "INAPPROPRIATE" to "욕설·부적절한 대화",
    "SPAM" to "스팸·광고",
    "FAKE_POSTING" to "허위 정보·사기 의심",
    "ETC" to "기타",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatReportSheet(
    roomId: Int,
    opponentName: String,
    token: String?,
    message: ChatMessage? = null,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var reason by remember { mutableStateOf("INAPPROPRIATE") }
    var detail by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 기타는 상세 필수(iOS canSubmit 동일).
    val canSubmit = !sending && (reason != "ETC" || detail.trim().isNotEmpty())

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (message == null) "채팅방 신고" else "메시지 신고",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                lineHeight = 21.sp, color = MuyeonColors.textHead,
            )
            Text(
                if (message == null) {
                    "${opponentName.ifEmpty { "상대방" }}님과의 채팅방을 신고합니다.\n신고 내용은 운영팀이 확인 후 조치해요."
                } else {
                    "${opponentName.ifEmpty { "상대방" }}님이 보낸 아래 메시지를 신고합니다.\n신고 내용은 운영팀이 확인 후 조치해요."
                },
                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
            )
            // 신고 대상 메시지 미리보기(서버가 준 내용 그대로 — 마스킹/숨김 처리 포함)
            message?.let { m ->
                Text(
                    when (m.type) { "IMAGE" -> "사진"; "VIDEO" -> "동영상"; else -> m.content },
                    fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 19.sp,
                    color = MuyeonColors.textHead, maxLines = 3,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF2F2F7)).padding(10.dp),
                )
            }
            Text(
                "신고 사유",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead,
            )
            REPORT_REASONS.forEach { (code, label) ->
                Row(
                    Modifier.fillMaxWidth().clickable { reason = code }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 18.sp,
                        color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                    )
                    if (reason == code) {
                        Icon(Icons.Filled.Check, null, tint = MuyeonColors.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Text(
                "상세 내용 (기타는 필수)",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead,
            )
            OutlinedTextField(
                value = detail,
                onValueChange = { detail = it },
                placeholder = {
                    Text(
                        "상황을 자세히 적어 주시면 처리에 도움이 돼요.",
                        fontFamily = customFontFamily, fontSize = 14.sp,
                    )
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            )
            errorText?.let {
                Text(it, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.danger)
            }
            Text(
                if (sending) "신고 접수 중…" else "신고하기",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (canSubmit) MuyeonColors.primary else Color.Gray.copy(alpha = 0.4f))
                    .clickable(enabled = canSubmit) {
                        sending = true
                        errorText = null
                        scope.launch {
                            val ok = postReport(
                                token,
                                targetType = if (message == null) "CHATROOM" else "CHAT_MESSAGE",
                                targetId = message?.id ?: roomId,
                                reason = reason,
                                detail = detail.trim(),
                            )
                            sending = false
                            if (ok) onDone() else errorText = "신고를 접수하지 못했어요. 잠시 후 다시 시도해 주세요."
                        }
                    }
                    .padding(vertical = 14.dp),
            )
            ReportContactLine()
        }
    }
}

/** 긴급 연락처 — 전화·메일은 탭하면 바로 연결. */
@Composable
private fun ReportContactLine() {
    val linkStyle = TextLinkStyles(SpanStyle(color = MuyeonColors.primary, textDecoration = TextDecoration.Underline))
    Text(
        buildAnnotatedString {
            append("긴급 신고·문의: 고객센터 ")
            withLink(LinkAnnotation.Url("tel:050219297300", linkStyle)) { append("0502-1929-7300") }
            append(" · ")
            withLink(LinkAnnotation.Url("mailto:muyeon.app@gmail.com", linkStyle)) { append("muyeon.app@gmail.com") }
            append(" (신고는 24시간 이내 처리됩니다)")
        },
        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp, color = MuyeonColors.textSub,
    )
}

/** POST /reports { targetType: CHATROOM | CHAT_MESSAGE, targetId, reason, detail }. */
private suspend fun postReport(token: String?, targetType: String, targetId: Int, reason: String, detail: String): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("targetType", targetType)
                .put("targetId", targetId)
                .put("reason", reason)
                .apply { if (detail.isNotEmpty()) put("detail", detail) }
            val req = Request.Builder()
                .url(BuildConfig.API_BASE_URL + "/api/reports")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            OkHttpClient().newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }
