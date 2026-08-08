package com.muyeon.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.stringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 채팅방 레슨 카드 2종 — iOS `LessonProgressCard.swift` / `LessonProposalCardBubble.swift` 이식.
 *  진행 카드는 서버 progress(메시지 무관) 기반이라 재입장(leftAt) 방에서도 동일하게 동작한다.
 */

// ============================================================
// 레슨 진행 카드 — 5단계 스테퍼(요청→견적→채택→확정→완료)
// ============================================================

private val PROGRESS_STEPS = listOf("요청", "견적", "채택", "확정", "완료")

@Composable
fun LessonProgressCard(
    progress: ChatLessonProgress,
    context: ChatQuoteContext?,
    category: String?,          // "발레 레슨"
    isExpired: Boolean,
    isProposal: Boolean = false,
    personName: String? = null,
    personImage: String? = null,
    personRole: String? = null, // "강사" | "수강생"
    onPrimary: () -> Unit,
) {
    val isTeacher = context?.isTeacher ?: false

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MuyeonColors.surface)
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 상대 프로필 헤더 — [아바타] 이름 [강사/수강생]. 역할칩이 레슨 방향을 잡아준다.
        if (personName != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                QuoteAvatar(personImage, personName, 22.dp)
                Text(
                    personName,
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textHead,
                )
                personRole?.let { role ->
                    // 상대가 수강생 = 내가 강사(내 액션 가능성 ↑) → 브랜드색 강조.
                    val mine = role == "수강생"
                    Text(
                        role,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        lineHeight = 13.sp,
                        color = if (mine) MuyeonColors.primary else MuyeonColors.secondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background((if (mine) MuyeonColors.primary else MuyeonColors.secondary).copy(alpha = 0.14f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                category ?: "레슨",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead,
            )
            if (isProposal) Chip("약속", MuyeonColors.primary)
            if (isExpired) Chip("마감", MuyeonColors.secondary)
            Spacer(Modifier.weight(1f))
            context?.priceText?.let {
                Text(
                    it,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.textHead,
                )
            }
        }

        // 5단계 스테퍼
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PROGRESS_STEPS.forEachIndexed { i, label ->
                val done = i <= progress.stepIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape)
                            .background(if (done) MuyeonColors.primary else MuyeonColors.border),
                    )
                    Text(
                        label,
                        fontFamily = customFontFamily,
                        fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 10.sp, lineHeight = 12.sp,
                        color = if (done) MuyeonColors.textHead else MuyeonColors.secondary,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                if (i != PROGRESS_STEPS.lastIndex) {
                    Box(
                        Modifier.weight(1f).height(2.dp).padding(horizontal = 2.dp)
                            .background(if (i < progress.stepIndex) MuyeonColors.primary else MuyeonColors.border),
                    )
                }
            }
        }

        // 단계·역할별 한 줄 CTA
        val cta = primaryCta(progress.step, isTeacher)
        if (cta != null) {
            Text(
                cta,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                lineHeight = 17.sp, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MuyeonColors.primary)
                    .clickable(onClick = onPrimary)
                    .padding(vertical = 10.dp),
            )
        }
    }
}

/** iOS 단계·역할별 주 액션 라벨. */
private fun primaryCta(step: String, isTeacher: Boolean): String? = when (step) {
    "RESPONDED" -> if (isTeacher) null else "견적 보기"
    "ACCEPTED" -> if (isTeacher) "일정 정하기" else "일정 확정 기다리는 중"
    "SCHEDULED" -> "일정 보기"
    "DONE" -> if (isTeacher) null else "후기 쓰기"
    else -> null
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

// ============================================================
// 레슨 약속 제안 카드 — 상태별 렌더(대기/수락/거절/취소)
// ============================================================

/** 메시지 content(JSON 문자열)로 오는 제안 카드 — iOS LessonProposalCard. */
data class LessonProposalCard(
    val proposalId: Int,
    val startAt: String,
    val durationMin: Int?,
    val place: String?,
    val memo: String?,
    val status: String,       // PROPOSED | ACCEPTED | DECLINED | CANCELED | EXPIRED
    val scheduleId: Int?,
) {
    companion object {
        fun parse(content: String): LessonProposalCard? = runCatching {
            val o = JSONObject(content)
            LessonProposalCard(
                proposalId = o.optInt("proposalId"),
                startAt = o.optString("startAt"),
                durationMin = o.intOrNull("durationMin"),
                place = o.stringOrNull("place"),
                memo = o.stringOrNull("memo"),
                status = o.optString("status"),
                scheduleId = o.intOrNull("scheduleId"),
            )
        }.getOrNull()
    }
}

/** POST /lesson-proposals/:id/{accept|decline|cancel} — iOS LessonProposalService. */
private suspend fun proposalAction(token: String?, id: Int, action: String, force: Boolean = false): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply { if (action == "accept") put("force", force) }
            val req = Request.Builder()
                .url(BuildConfig.API_BASE_URL + "/api/lesson-proposals/$id/$action")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            OkHttpClient().newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

@Composable
fun LessonProposalBubble(
    contentJson: String,
    isTeacherSide: Boolean,   // 열람자가 이 방의 강사인지(= 제안자)
    token: String?,
    onChanged: () -> Unit,
) {
    val card = remember(contentJson) { LessonProposalCard.parse(contentJson) } ?: return
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun act(action: String) {
        if (busy) return
        busy = true
        scope.launch {
            proposalAction(token, card.proposalId, action)
            busy = false
            onChanged()   // 상태 변경은 서버가 message-updated 로 브로드캐스트하지만 컨텍스트도 재조회.
        }
    }

    Column(
        Modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MuyeonColors.surface)
            .border(1.dp, MuyeonColors.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.EventAvailable, null, tint = MuyeonColors.primary, modifier = Modifier.size(14.dp))
            Text(
                "레슨 약속 제안",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.primary,
            )
            Spacer(Modifier.weight(1f))
            Chip(proposalStatusLabel(card.status), proposalStatusColor(card.status))
        }

        Text(
            proposalDateTime(card.startAt) + " · ${card.durationMin ?: 60}분",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
        card.place?.takeIf { it.isNotEmpty() }?.let { place ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = MuyeonColors.secondary, modifier = Modifier.size(11.dp))
                Text(place, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textSub)
            }
        }
        card.memo?.takeIf { it.isNotEmpty() }?.let {
            Text(it, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textSub)
        }

        // 대기중일 때만 액션 — 고객: 수락/거절 / 강사(제안자): 제안 취소.
        if (card.status == "PROPOSED") {
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isTeacherSide) {
                    ProposalButton("제안 취소", filled = false, enabled = !busy, modifier = Modifier.weight(1f)) { act("cancel") }
                } else {
                    ProposalButton("거절", filled = false, enabled = !busy, modifier = Modifier.weight(1f)) { act("decline") }
                    ProposalButton("수락", filled = true, enabled = !busy, modifier = Modifier.weight(1f)) { act("accept") }
                }
            }
        }
    }
}

@Composable
private fun ProposalButton(
    text: String,
    filled: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp,
        color = if (filled) Color.White else MuyeonColors.primary, textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) MuyeonColors.primary else MuyeonColors.primary.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 9.dp),
    )
}

private fun proposalStatusLabel(s: String) = when (s) {
    "PROPOSED" -> "대기중"
    "ACCEPTED" -> "수락됨"
    "DECLINED" -> "거절됨"
    "CANCELED" -> "취소됨"
    "EXPIRED" -> "만료"
    else -> s
}

private fun proposalStatusColor(s: String) = when (s) {
    "ACCEPTED" -> MuyeonColors.green
    "PROPOSED" -> MuyeonColors.primary
    else -> MuyeonColors.secondary
}

/** iOS LessonProposalFormat.dateTime — "8월 12일(화) 오후 3:00". */
private fun proposalDateTime(iso: String): String {
    val t = QuoteUi.parseDate(iso) ?: return ""
    return SimpleDateFormat("M월 d일(E) a h:mm", Locale.KOREA).format(Date(t))
}
