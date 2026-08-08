package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

/**
 * 받은견적 화면 공용 UI 헬퍼(라벨/시간/이미지) + 소형 컴포넌트(아바타·배지·빈상태)
 *  — iOS `QuoteReceivedUI.swift` + `MyQuotesListView.QuoteEmptyState` 1:1.
 *  라벨은 웹 constants/quoteCategories.js 와 1:1 동기화.
 */
object QuoteUi {

    /** 카테고리 8종 (웹 QUOTE_CATEGORIES 와 동일). 문진용 QuoteCategory.all(7종)과 달리 'hobby' 포함. */
    val categoryLabels: Map<String, String> = mapOf(
        "ballet" to "발레", "barre" to "바레", "korean" to "한국무용", "modern" to "현대무용",
        "practical" to "실용무용", "balletfit" to "발레핏", "musical" to "뮤지컬", "hobby" to "취미무용",
    )

    fun categoryLabel(id: String?): String {
        if (id == null) return "레슨"
        return categoryLabels[id] ?: id
    }

    /** 헤더 제목용 — 카테고리 + '레슨'. 라벨이 이미 '레슨'이면 중복 방지('레슨 레슨' → '레슨'). */
    fun categoryTitle(id: String?): String {
        val label = categoryLabel(id)
        return if (label == "레슨") "레슨" else "$label 레슨"
    }

    fun statusLabel(s: String?): String = when (s) {
        "OPEN" -> "견적 받는 중"
        "MATCHED" -> "매칭 완료"
        "CLOSED" -> "완료"
        "CANCELED" -> "취소됨"
        "EXPIRED" -> "견적 마감"
        else -> s ?: ""
    }

    fun statusColor(s: String?): Color = when (s) {
        "MATCHED" -> MuyeonColors.green          // 채택/매칭 완료 = 초록(진행중과 구분)
        "OPEN" -> MuyeonColors.orange            // 견적 받는 중 = 주황(대기 상태 시인성)
        "CANCELED", "EXPIRED" -> MuyeonColors.textSub
        else -> MuyeonColors.primary
    }

    /** 이미지 URL — 절대(http)면 그대로, 상대경로면 baseURL 접두(iOS 실사고 대응). */
    fun imageUrl(path: String?): String? {
        if (path.isNullOrEmpty()) return null
        return if (path.startsWith("http")) path else BuildConfig.API_BASE_URL + path
    }

    /** ISO8601 → epoch millis (fractional 유무 모두 허용). 실패 시 null. */
    fun parseDate(iso: String?): Long? {
        if (iso.isNullOrEmpty()) return null
        for (pattern in ISO_PATTERNS) {
            runCatching {
                val f = SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                return f.parse(iso)?.time
            }
        }
        return null
    }

    /** ISO8601 → 상대시간(간단). iOS QuoteUI.relativeTime 과 동일 임계값. */
    fun relativeTime(iso: String?): String {
        val time = parseDate(iso) ?: return ""
        val diff = (System.currentTimeMillis() - time) / 1000.0
        if (diff < 60) return "방금"
        if (diff < 3600) return "${(diff / 60).toInt()}분 전"
        if (diff < 86400) return "${(diff / 3600).toInt()}시간 전"
        if (diff < 86400 * 7) return "${(diff / 86400).toInt()}일 전"
        return SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(time))
    }

    /** 구조화 금액 → 표시 문자열. 예: (60000, PER_SESSION) → "회당 60,000원". */
    fun priceText(amount: Int?, unit: String?, fallback: String?): String {
        if (amount != null) {
            val won = String.format(Locale.KOREA, "%,d", amount)
            val prefix = when (unit) {
                "PER_MONTH" -> "월 "
                "TOTAL" -> ""
                else -> "회당 "
            }
            return "$prefix${won}원"
        }
        if (!fallback.isNullOrEmpty()) return fallback
        return "협의 가능"
    }

    /** 견적 만료(요청 생성 + N일, 기본 14일)까지 남은 일수. 양수=남음, 0=오늘, 음수=지남. */
    fun daysUntilExpiry(createdAtISO: String?, days: Int = 14): Int? {
        val created = parseDate(createdAtISO) ?: return null
        val expiry = created + days * 86_400_000L
        return ceil((expiry - System.currentTimeMillis()) / 86_400_000.0).toInt()
    }

    private val ISO_PATTERNS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )
}

/** 강사 아바타(원형, Coil 캐시 + 이니셜 폴백) — iOS QuoteAvatar. */
@Composable
fun QuoteAvatar(image: String?, name: String, size: Dp = 52.dp, modifier: Modifier = Modifier) {
    val url = QuoteUi.imageUrl(image)
    Box(
        modifier.size(size).clip(CircleShape).background(MuyeonColors.placeholder),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                name.take(1),
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.38f).sp, color = MuyeonColors.secondary,
            )
        }
    }
}

/**
 * 응답자 겹침 아바타 — 2명 이상이면 원형 프로필을 겹쳐서 표시(최대 4). iOS QuoteAvatarStack.
 *  1명 이하면 단일 QuoteAvatar 와 동일. 행 레이아웃 폭은 단일 아바타와 같게 유지한다.
 */
@Composable
fun QuoteAvatarStack(images: List<String?>, fallbackName: String, size: Dp = 52.dp) {
    val shown = images.take(4)
    val n = maxOf(shown.size, 1)
    if (n <= 1) {
        QuoteAvatar(shown.firstOrNull(), fallbackName, size)
        return
    }
    val sub = size * 0.68f          // 개별 아바타 지름
    val step = sub * 0.60f          // 겹침 간격(작을수록 많이 겹침)
    val width = sub + step * (n - 1)
    Box(Modifier.width(size), contentAlignment = Alignment.Center) {
        Box(Modifier.width(width).height(sub)) {
            shown.forEachIndexed { idx, img ->
                Box(
                    Modifier
                        .offset(x = step * idx)
                        .zIndex((n - idx).toFloat())   // 왼쪽(앞) 아바타가 위로
                        .border(2.dp, MuyeonColors.surface, CircleShape),
                ) {
                    QuoteAvatar(img, fallbackName, sub)
                }
            }
        }
    }
}

/** 소형 배지(칩) — 평점/전공/상태. iOS QuotePill. */
@Composable
fun QuotePill(text: String, color: Color = MuyeonColors.primary, filled: Boolean = false) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp,
        lineHeight = 13.sp,
        color = if (filled) Color.White else color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color else color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** 빈 상태 — iOS QuoteEmptyState. */
@Composable
fun QuoteEmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, null, tint = Color(0xFFB3B3B3), modifier = Modifier.size(40.dp))
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
            lineHeight = 20.sp, color = MuyeonColors.textHead,
        )
        Text(
            message,
            fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
        )
    }
}
