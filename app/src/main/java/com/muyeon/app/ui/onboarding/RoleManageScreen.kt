package com.muyeon.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import org.json.JSONObject

/**
 * 회원유형 관리 — iOS `Onboarding/Role/RoleManageView.swift` 이식.
 *  계정 1개 = 역할 N개(대표역할 없음). '전환'이 아니라 유형 '관리'.
 *
 * ⚠️ 실제 API 는 **웹 콜백**이 수행한다(`__onRoleManageAdd`/`__onRoleRemove`/
 *   `__onRoleManageVerify`/`__onActiveTypeChanged`). 이 화면은 UI + 낙관적 갱신만 담당하고,
 *   서버 반영은 웹뷰로 돌아가 콜백을 호출해야 한다 — iOS 와 같은 계약이라 임의로 REST 를 부르면 안 된다.
 */
data class RoleOption(val code: String, val emoji: String, val title: String, val business: Boolean)

private val ROLE_OPTIONS = listOf(
    RoleOption("TEACHER", "🩰", "강사", false),
    RoleOption("DANCER", "💃", "무용수", false),
    RoleOption("ACADEMY", "🏫", "학원·원장", true),
    RoleOption("TEAM", "🎭", "공연팀·기획자", true),
    RoleOption("SPACE", "🏢", "공간보유자", true),
    RoleOption("HOBBY", "✨", "일반회원", false),
)

/** 웹이 브릿지로 전달하는 역할 데이터. */
data class RoleManagePayload(
    val granted: Set<String>,
    val status: Map<String, String>,
    val activeType: String,
) {
    companion object {
        fun parse(json: String?): RoleManagePayload {
            if (json.isNullOrEmpty()) return RoleManagePayload(emptySet(), emptyMap(), "GENERAL")
            return runCatching {
                val o = JSONObject(json)
                val granted = mutableSetOf<String>()
                val status = mutableMapOf<String, String>()
                o.optJSONArray("held")?.let { arr -> (0 until arr.length()).forEach { granted.add(arr.optString(it)) } }
                o.optJSONArray("roles")?.let { arr ->
                    (0 until arr.length()).forEach { i ->
                        val r = arr.optJSONObject(i) ?: return@forEach
                        val code = r.optString("role")
                        if (r.optBoolean("granted", false)) granted.add(code)
                        r.optString("status").takeIf { it.isNotEmpty() }?.let { status[code] = it }
                    }
                }
                RoleManagePayload(granted, status, o.optString("activeType").ifEmpty { "GENERAL" })
            }.getOrDefault(RoleManagePayload(emptySet(), emptyMap(), "GENERAL"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleManageScreen(
    payload: RoleManagePayload,
    heroImageUrl: String?,
    onClose: () -> Unit,
    onAdd: (String) -> Unit,        // 개인 추가 / 사업 승인완료 → __onRoleManageAdd
    onRemove: (String) -> Unit,     // 해제 → __onRoleRemove
    onVerify: (String) -> Unit,     // 사업 인증 필요 → 인증 화면
    onSelectActive: (String) -> Unit, // 활동 유형 선택 → __onActiveTypeChanged
) {
    var granted by remember { mutableStateOf(payload.granted) }
    var status by remember { mutableStateOf(payload.status) }
    var activeSel by remember { mutableStateOf(payload.activeType) }
    var pending by remember { mutableStateOf<RoleOption?>(null) }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "회원유형 관리", onClose = onClose)

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // 히어로 — 서버가 준 랜덤 이미지 1장
            Box(Modifier.fillMaxWidth().height(200.dp).background(Color(0xFFF2F2F7))) {
                QuoteUi.imageUrl(heroImageUrl)?.let {
                    AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }

            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "회원유형 선택",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    lineHeight = 24.sp, color = MuyeonColors.textHead,
                )
                Text(
                    "여러 유형을 함께 가질 수 있어요. 활동 유형은 하나만 선택돼요.",
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
                )

                // 3열 그리드
                ROLE_OPTIONS.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { opt ->
                            RoleCard(
                                opt = opt,
                                held = granted.contains(opt.code),
                                statusText = status[opt.code],
                                isActive = activeSel == opt.code,
                                modifier = Modifier.weight(1f),
                            ) { pending = opt }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                // 활동 유형 — 일반(GENERAL)은 항상 선택 가능
                Text(
                    "활동 유형",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    lineHeight = 19.sp, color = MuyeonColors.textHead, modifier = Modifier.padding(top = 8.dp),
                )
                val activeChoices = listOf("GENERAL" to "일반") +
                    ROLE_OPTIONS.filter { granted.contains(it.code) }.map { it.code to it.title }
                activeChoices.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (code, label) ->
                            val on = activeSel == code
                            Text(
                                label,
                                fontFamily = customFontFamily,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                                color = if (on) Color.White else MuyeonColors.textSub,
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
                                    .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                                    .clickable { activeSel = code; onSelectActive(code) }
                                    .padding(vertical = 9.dp),
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }

    // 액션 시트 — 개인=즉시 추가/해제, 사업=인증 후 추가
    pending?.let { opt ->
        val held = granted.contains(opt.code)
        val st = status[opt.code]
        ModalBottomSheet(onDismissRequest = { pending = null }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${opt.emoji} ${opt.title}",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    lineHeight = 21.sp, color = MuyeonColors.textHead,
                )
                Text(
                    when {
                        held -> "이미 보유한 유형이에요."
                        st == "PENDING" -> "인증 심사 중이에요. 승인되면 알려드릴게요."
                        opt.business -> "사업자 인증 서류를 제출하면 심사 후 추가돼요."
                        else -> "바로 추가할 수 있어요."
                    },
                    fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.textSub,
                )
                when {
                    held -> SheetButton("유형 해제", filled = false, danger = true) {
                        granted = granted - opt.code
                        if (activeSel == opt.code) activeSel = "GENERAL"
                        pending = null
                        onRemove(opt.code)
                    }
                    st == "PENDING" -> Unit   // 심사 중엔 액션 없음
                    opt.business -> SheetButton("인증하고 추가", filled = true) {
                        pending = null
                        // 낙관적으로 심사중 표시 — 실제 승인은 서버·웹 콜백이 반영.
                        status = status + (opt.code to "PENDING")
                        onVerify(opt.code)
                    }
                    else -> SheetButton("유형 추가", filled = true) {
                        granted = granted + opt.code
                        pending = null
                        onAdd(opt.code)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleCard(
    opt: RoleOption,
    held: Boolean,
    statusText: String?,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (held) MuyeonColors.primary.copy(alpha = 0.08f) else Color(0xFFF7F7F7))
            .then(if (isActive) Modifier.border(1.5.dp, MuyeonColors.primary, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(opt.emoji, fontSize = 26.sp, lineHeight = 30.sp)
        Text(
            opt.title,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textHead, textAlign = TextAlign.Center, maxLines = 1,
        )
        when {
            held -> Icon(Icons.Filled.Check, "보유", tint = MuyeonColors.primary, modifier = Modifier.size(14.dp))
            statusText == "PENDING" -> Text(
                "심사중",
                fontFamily = customFontFamily, fontSize = 10.sp, lineHeight = 12.sp, color = MuyeonColors.orange,
            )
            opt.business -> Icon(Icons.Filled.Lock, "인증 필요", tint = MuyeonColors.secondary, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun SheetButton(text: String, filled: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp,
        color = if (filled) Color.White else if (danger) MuyeonColors.danger else MuyeonColors.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    filled -> MuyeonColors.primary
                    danger -> MuyeonColors.danger.copy(alpha = 0.08f)
                    else -> MuyeonColors.primary.copy(alpha = 0.08f)
                },
            )
            .clickable(onClick = onClick).padding(vertical = 14.dp),
    )
}
