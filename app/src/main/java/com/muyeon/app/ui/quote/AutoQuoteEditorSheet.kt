package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/** 메시지 글자 수 한도(iOS·웹과 같다). */
const val AUTO_QUOTE_MESSAGE_MAX = 300

/** 추가/편집 — iOS AutoQuoteEditor(이름/금액/메시지 300자 + 예시 문구). */
@Composable
fun AutoQuoteEditorSheet(
    state: AutoQuoteEditorState,
    busy: Boolean,
    onSave: (title: String, message: String, price: String) -> Unit,
    onCancel: () -> Unit,
    /** ★ 2026-11-06 저장 전 지금 쓰는 내용으로 미리보기. */
    onPreview: ((title: String, message: String, price: String) -> Unit)? = null,
) {
    var title by remember(state) { mutableStateOf(state.title) }
    var message by remember(state) { mutableStateOf(state.message) }
    var price by remember(state) { mutableStateOf(state.price) }
    val canSave = !busy && message.trim().isNotEmpty()

    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MuyeonColors.surface)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                if (state.id == null) "새 메시지" else "메시지 편집",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                lineHeight = 20.sp, color = MuyeonColors.textHead,
            )
            EditorField("이름 (선택)") {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, singleLine = true,
                    placeholder = { Text("예: 기본 인사, 주말 전용", fontFamily = customFontFamily, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            EditorField("자동 견적 금액") {
                OutlinedTextField(
                    value = price, onValueChange = { price = it }, singleLine = true,
                    placeholder = { Text("예: 회당 60,000원", fontFamily = customFontFamily, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            EditorField("자동 응답 메시지", trailing = { QuoteMessageExamplesButton() }) {
                Column {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { if (it.length <= AUTO_QUOTE_MESSAGE_MAX) message = it },
                        placeholder = {
                            Text(
                                "예: 안녕하세요! 발레 전문 강사예요. 편하게 상담 도와드릴게요 :)",
                                fontFamily = customFontFamily, fontSize = 14.sp,
                            )
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    )
                    Text(
                        "${message.length}/$AUTO_QUOTE_MESSAGE_MAX",
                        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp,
                        color = MuyeonColors.secondary, textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "취소",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp))
                        .clickable(onClick = onCancel).padding(vertical = 13.dp),
                )
                if (onPreview != null) {
                    Text(
                        "미리보기",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp))
                            .clickable { onPreview(title, message, price) }.padding(vertical = 13.dp),
                    )
                }
                Text(
                    if (state.id == null) "추가" else "저장",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = Color.White, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canSave) MuyeonColors.primary else Color.Gray.copy(alpha = 0.4f))
                        .clickable(enabled = canSave) { onSave(title.trim(), message.trim(), price.trim()) }
                        .padding(vertical = 13.dp),
                )
            }
        }
    }
}

@Composable
private fun EditorField(
    label: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
        content()
    }
}
