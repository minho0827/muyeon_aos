package com.muyeon.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.resume.ResumeOptions
import org.json.JSONObject
import java.util.Locale

/**
 * 채팅 카드 말풍선 — iOS `ChatBubbleComponents`(QUOTE_CARD) / `SurveyCardBubble` /
 *  `SurveyUpdateBubble` / `LessonCardBubble` 이식.
 *
 * ⚠️ 서버는 메시지 타입을 9종 보내는데(TEXT/IMAGE/VIDEO/SYSTEM/QUOTE_REQUEST/QUOTE_CARD/
 *   SURVEY_CARD/SURVEY_UPDATE/LESSON_CARD/LESSON_PROPOSAL) AOS 는 4종만 그리고 있었다.
 *   나머지는 `else -> Text(content)` 로 떨어져 **JSON 원문이 그대로 노출**됐다.
 */

/** 안전 파싱 — content 가 JSON 이 아니면 null(원문 노출 대신 카드 자체를 감춘다). */
private fun parse(json: String): JSONObject? =
    runCatching { JSONObject(json) }.getOrNull()

private fun won(v: Int) = String.format(Locale.KOREA, "%,d", v)

// MARK: - QUOTE_CARD (서비스 견적)

/**
 * 서비스 견적 카드. 강사 발신(우측) / 회원 수신(좌측).
 *  제공자 본인이 보낸 카드에는 자기 프로필 버튼을 노출하지 않는다 —
 *  같은 카드를 양쪽이 공유하므로 수신자에게만 프로필 이동을 준다(iOS showProviderProfile).
 */
@Composable
fun QuoteCardBubble(json: String, showProviderProfile: Boolean, onOpenProvider: (Int, Boolean) -> Unit) {
    val d = parse(json) ?: return
    val teacherId = d.optInt("id").takeIf { it > 0 }
    val attachment = d.optJSONObject("attachment")
    val isAcademy = attachment?.optString("type") == "ACADEMY" || d.optString("profileType") == "ACADEMY"
    val sub = listOf(
        ResumeOptions.careerLabel(d.optString("career")),
        d.optString("region"),
    ).filter { it.isNotEmpty() }.joinToString(" · ")

    Column(
        Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(16.dp))
            .background(MuyeonColors.surface)
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            QuoteAvatar(d.optString("image").ifEmpty { null }, d.optString("name").ifEmpty { "강사" }, 44.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "서비스 견적",
                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                    color = MuyeonColors.textSub,
                )
                Text(
                    d.optString("name").ifEmpty { "강사" },
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    lineHeight = 19.sp, color = MuyeonColors.textHead,
                )
            }
        }
        HorizontalDivider(color = MuyeonColors.border)
        if (sub.isNotEmpty()) {
            Text(
                sub,
                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                color = MuyeonColors.textSub,
            )
        }
        CardKv("서비스", "${d.optString("service")} 레슨")
        // 예약금은 결제 방식이 DEPOSIT 일 때만(iOS 와 같은 조건).
        val deposit = d.optInt("depositAmount")
        if (d.optString("paymentMode") == "DEPOSIT" && deposit > 0) {
            CardKv("예약금", "${won(deposit)}원", valueColor = MuyeonColors.primary)
        }
        CardKv(
            "예상금액",
            QuoteUi.priceText(
                d.optInt("priceAmount").takeIf { it > 0 },
                d.optString("priceUnit").ifEmpty { null },
                d.optString("price").ifEmpty { null },
            ),
            valueSize = 16,
        )
        attachment?.let { att ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF2F2F7)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "${if (isAcademy) "학원 정보" else "이력서"} · ${att.optString("title")}",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textHead,
                )
                att.optString("intro").takeIf { it.isNotEmpty() }?.let {
                    Text(
                        it,
                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                        color = MuyeonColors.textSub,
                    )
                }
                val tags = jsonStrings(att, "genres") + jsonStrings(att, "fields")
                if (isAcademy && tags.isNotEmpty()) {
                    Text(
                        tags.joinToString(" · "),
                        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                        color = MuyeonColors.textSub,
                    )
                }
            }
        }
        if (teacherId != null && showProviderProfile) {
            Text(
                if (isAcademy) "학원 정보 보기" else "강사 프로필 보기",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                lineHeight = 18.sp, color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(MuyeonColors.primary)
                    .clickable { onOpenProvider(teacherId, isAcademy) }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

private fun jsonStrings(o: JSONObject, key: String): List<String> =
    o.optJSONArray(key)?.let { arr ->
        (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotEmpty() }
    } ?: emptyList()

@Composable
private fun CardKv(label: String, value: String, valueColor: Color? = null, valueSize: Int = 14) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
            color = MuyeonColors.textSub, modifier = Modifier.weight(1f),
        )
        Text(
            value,
            fontFamily = customFontFamily,
            fontWeight = if (valueSize >= 16) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = valueSize.sp, lineHeight = (valueSize + 3).sp,
            color = valueColor ?: MuyeonColors.textHead,
        )
    }
}

// MARK: - SURVEY_CARD (레슨 전 설문)

/**
 * 설문 카드. 강사 발신(우측) / 회원 열람(좌측). 탭 → 응답/열람.
 *  카드 구분용 상단 라벨은 "N차 · M월 D일 발송" — 제목이 다 같아 구분이 안 되던 문제(iOS 와 동일).
 */
@Composable
fun SurveyCardBubble(
    json: String,
    done: Boolean,
    seq: Int,
    sentAt: String?,
    revision: Int,
    onOpen: (Int) -> Unit,
) {
    val d = parse(json) ?: return
    val dispatchId = d.optInt("dispatchId").takeIf { it > 0 }
    val header = buildList {
        if (seq > 0) add("${seq}차")
        dateLabel(sentAt)?.let { add("$it 발송") }
    }.ifEmpty { listOf("레슨 전 설문") }.joinToString(" · ")

    Column(
        Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(16.dp))
            .background(MuyeonColors.surface)
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(16.dp))
            .clickable(enabled = dispatchId != null) { dispatchId?.let(onOpen) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(9.dp))
                    .background((if (done) MuyeonColors.green else MuyeonColors.primary).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (done) Icons.Filled.CheckCircle else Icons.Outlined.Assignment, null,
                    tint = if (done) MuyeonColors.green else MuyeonColors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        header,
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                        lineHeight = 15.sp, color = MuyeonColors.primary,
                    )
                    if (done) {
                        Text(
                            "✓ 응답 완료",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                            lineHeight = 13.sp, color = MuyeonColors.green,
                            modifier = Modifier.clip(RoundedCornerShape(50))
                                .background(MuyeonColors.green.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    d.optString("title").ifEmpty { "레슨 전 설문" },
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // 응답을 여러 번 고쳤으면 회차를 알려준다(강사가 최신본인지 알 수 있게).
        if (done && revision > 1) {
            Text(
                "${revision}번째 응답본이에요",
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                color = MuyeonColors.textSub,
            )
        }
        Text(
            if (done) "응답 보기 ›" else "설문 열기 ›",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.primary,
        )
    }
}

/** "2026-09-06T…" → "9월 6일". 파싱 실패면 null(라벨에서 빠진다). */
private fun dateLabel(iso: String?): String? {
    val s = iso?.take(10)?.split("-") ?: return null
    if (s.size != 3) return null
    val m = s[1].toIntOrNull() ?: return null
    val d = s[2].toIntOrNull() ?: return null
    return "${m}월 ${d}일"
}

// MARK: - SURVEY_UPDATE (응답/수정 알림)

/** 회원 응답·수정 알림 — 탭하면 해당 설문 카드로 이동. */
@Composable
fun SurveyUpdateBubble(json: String, onOpen: (Int) -> Unit) {
    val d = parse(json) ?: return
    val dispatchId = d.optInt("dispatchId").takeIf { it > 0 }
    val isEdit = d.optString("action").ifEmpty { "RESPOND" } == "EDIT"
    val label = if (isEdit) {
        "회원이 레슨 전 설문을 수정했어요 (${d.optInt("revision").takeIf { it > 0 } ?: 2}번째)"
    } else {
        "회원이 레슨 전 설문에 응답했어요"
    }
    val accent = if (isEdit) MuyeonColors.orange else MuyeonColors.green

    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF2F2F7))
            .clickable(enabled = dispatchId != null) { dispatchId?.let(onOpen) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isEdit) Icons.Filled.Edit else Icons.Filled.CheckCircle, null,
            tint = accent, modifier = Modifier.size(13.dp),
        )
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
            lineHeight = 15.sp, color = MuyeonColors.textHead, maxLines = 1,
        )
        Text(
            "보기 ›",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            lineHeight = 15.sp, color = accent,
        )
    }
}

// MARK: - LESSON_CARD (레슨 일정)

/** 레슨 일정 카드 — 양쪽 공통(가운데). 탭 → 해당 일정 상세. */
@Composable
fun LessonCardBubble(json: String, onOpen: (Int) -> Unit) {
    val d = parse(json) ?: return
    val lessonId = d.optInt("lessonId").takeIf { it > 0 }
    val verb = d.optString("verb").ifEmpty { "등록" }
    val by = d.optString("by").ifEmpty { null }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MuyeonColors.surface)
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(16.dp))
            .clickable(enabled = lessonId != null) { lessonId?.let(onOpen) }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(MuyeonColors.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.CalendarMonth, null, tint = MuyeonColors.primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                // 양방향 방: "jay님이 등록한 레슨 일정" 처럼 이름을 넣어 방향을 분명히 한다.
                by?.let { "${it}님이 ${verb}한 레슨 일정" } ?: "레슨 일정 $verb",
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                color = MuyeonColors.textSub,
            )
            Text(
                d.optString("when").ifEmpty { "레슨 일정" },
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead,
            )
            d.optString("place").takeIf { it.isNotEmpty() }?.let {
                Text(
                    it,
                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                    color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            "일정 보기 ›",
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.primary,
        )
    }
}

// MARK: - SYSTEM / QUOTE_REQUEST (가운데 안내)

/**
 * 시스템·견적요청 안내 — 가운데 회색 알약.
 *  QUOTE_REQUEST 는 JSON content 대신 요약 문구로 바꾼다(iOS quoteRequestText).
 */
@Composable
fun SystemNoticeBubble(type: String, content: String) {
    val text = if (type == "QUOTE_REQUEST") quoteRequestText(content) else content
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text,
            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp,
            color = MuyeonColors.textSub,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF2F2F7))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/**
 * QUOTE_REQUEST content(JSON {quoteId,category,region}) → 안내 문구. 파싱 실패 시 기본 문구.
 *  category 는 코드(ballet)/라벨(발레) 혼재 가능 → 카테고리 표에서 정규화한다.
 */
internal fun quoteRequestText(content: String): String {
    val cat = parse(content)?.optString("category")?.takeIf { it.isNotEmpty() }
        ?: return "📩 견적을 요청했어요"
    return "📩 ${QuoteUi.categoryTitle(cat)} 견적을 요청했어요"
}
