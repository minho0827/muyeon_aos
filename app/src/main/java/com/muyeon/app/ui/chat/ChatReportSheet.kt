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
import androidx.compose.ui.text.font.FontWeight
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
                "채팅방 신고",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                lineHeight = 21.sp, color = MuyeonColors.textHead,
            )
            Text(
                "${opponentName.ifEmpty { "상대방" }}님과의 채팅방을 신고합니다.\n신고 내용은 운영팀이 확인 후 조치해요.",
                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
            )
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
                            val ok = postReport(token, roomId, reason, detail.trim())
                            sending = false
                            if (ok) onDone() else errorText = "신고를 접수하지 못했어요. 잠시 후 다시 시도해 주세요."
                        }
                    }
                    .padding(vertical = 14.dp),
            )
        }
    }
}

/** POST /reports { targetType: CHATROOM, targetId, reason, detail }. */
private suspend fun postReport(token: String?, roomId: Int, reason: String, detail: String): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("targetType", "CHATROOM")
                .put("targetId", roomId)
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
