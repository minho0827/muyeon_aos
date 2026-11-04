package com.muyeon.app.ui.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * ★ 2026-11-04 확인 기관 로고(백엔드 3a2da22) — 강사 공개 프로필 **상세**에서만 쓴다(작은 카드엔 없음).
 *  badges.logos[]: 관리자가 확인한 항목 중 기관을 고른 것. 옛 서버·비회원은 빈 배열.
 */
data class VerifiedLogo(
    val id: Int,
    val kind: String,
    /** EDUCATION | CAREER | CERT — 상세의 어느 섹션 맨 위에 붙을지. */
    val section: String,
    val org: String,
    val title: String,
    val logoUrl: String,
    val verifiedAt: String?,
) {
    /** "{org} · {title} · {verifiedAt} 확인" (확인일이 없으면 날짜 빼고). */
    val confirmText: String get() = listOfNotNull(org, title.ifBlank { null }, verifiedAt?.let { "$it 확인" }).joinToString(" · ")

    companion object {
        fun listFrom(arr: JSONArray?): List<VerifiedLogo> = (0 until (arr?.length() ?: 0)).mapNotNull { i ->
            val o = arr?.optJSONObject(i) ?: return@mapNotNull null
            val url = o.optString("logoUrl").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            VerifiedLogo(
                o.optInt("id"), o.optString("kind"), o.optString("section").ifEmpty { "CAREER" },
                o.optString("org"), o.optString("title"), url,
                o.optString("verifiedAt").takeIf { it.isNotBlank() && it != "null" },
            )
        }
    }
}

private const val ROW_MAX = 4
private fun logoUrl(path: String) = com.muyeon.app.ui.quote.QuoteUi.imageUrl(path)

/**
 * 이름 아래 "✔ 무용연이 확인한 경력" + 가로 로고 칩(최대 4개, 넘치면 "+N ›").
 *  logos 가 비어 있으면 아무것도 그리지 않는다(문구·자리 모두 없음 — 부르는 쪽도 비면 부르지 않는다).
 *  칩: 흰 둥근 칩 높이 36 · 안쪽 4 · 옅은 테두리, 로고 칸은 28×84 고정(Fit, 잘리지 않음 — 그림이 와도 칩 크기 그대로).
 *  로고를 못 불러오면 그 칩만 숨긴다.
 */
@Composable
fun VerifiedLogoRow(logos: List<VerifiedLogo>, modifier: Modifier = Modifier) {
    if (logos.isEmpty()) return
    var failed by remember(logos) { mutableStateOf<Set<Int>>(emptySet()) }
    var shown by remember { mutableStateOf<VerifiedLogo?>(null) }
    var sheet by remember { mutableStateOf(false) }
    val visible = logos.filter { it.id !in failed }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("✔ 무용연이 확인한 경력", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MuyeonColors.primary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            visible.take(ROW_MAX).forEach { l ->
                LogoChip(l, onClick = { shown = l }, onError = { failed = failed + l.id })
            }
            (visible.size - ROW_MAX).takeIf { it > 0 }?.let { more -> MoreChip(more) { sheet = true } }
        }
    }
    shown?.let { l ->
        MuyeonDialog(title = l.confirmText, actions = listOf(MuyeonDialogAction("확인")), onDismiss = { shown = null })
    }
    if (sheet) VerifiedLogoSheet(visible, onDismiss = { sheet = false })
}

@Composable
private fun LogoChip(l: VerifiedLogo, onClick: () -> Unit, onError: () -> Unit) {
    Box(
        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Color.White)
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(18.dp)).clickable(onClick = onClick)
            .padding(4.dp).semantics { contentDescription = l.org },
        Alignment.Center,
    ) {
        AsyncImage(
            logoUrl(l.logoUrl), l.org, contentScale = ContentScale.Fit,
            onError = { onError() },
            modifier = Modifier.height(28.dp).width(84.dp),
        )
    }
}

@Composable
private fun MoreChip(more: Int, onClick: () -> Unit) {
    Box(
        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Color.White)
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp),
        Alignment.Center,
    ) { Text("+$more ›", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MuyeonColors.textHead, style = TabularNums) }
}

/** 전체 확인 기관 목록 시트 — 40dp 로고(흰 둥근 사각) + 기관명 · 내용 · 확인일. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifiedLogoSheet(logos: List<VerifiedLogo>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("무용연이 확인한 경력", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MuyeonColors.textHead)
            logos.forEach { l -> LogoSheetRow(l) }
        }
    }
}

@Composable
private fun LogoSheetRow(l: VerifiedLogo) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp)).padding(4.dp),
            Alignment.Center,
        ) { AsyncImage(logoUrl(l.logoUrl), l.org, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(l.org, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
            l.title.takeIf { it.isNotBlank() }?.let { Text(it, fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.body, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            l.verifiedAt?.let { Text("$it 확인", fontFamily = customFontFamily, fontSize = 12.sp, color = MuyeonColors.textSub, style = TabularNums) }
        }
    }
}

/**
 * 섹션(학력·경력·자격증) 맨 위 확인 줄 — 그 섹션의 확인 항목마다 "20×20 로고 + {org} · {title}".
 *  부르는 쪽이 섹션 항목이 있을 때만 부른다. 로고를 못 불러오면 그 줄만 숨긴다.
 */
@Composable
fun SectionVerifiedLine(logos: List<VerifiedLogo>) {
    var failed by remember(logos) { mutableStateOf<Set<Int>>(emptySet()) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        logos.filter { it.id !in failed }.forEach { l ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AsyncImage(
                    logoUrl(l.logoUrl), l.org, contentScale = ContentScale.Fit,
                    onError = { failed = failed + l.id },
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    listOf(l.org, l.title).filter { it.isNotBlank() }.joinToString(" · "),
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MuyeonColors.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 섹션별 확인 항목(EDUCATION·CAREER·CERT) — 없으면 null(섹션은 지금과 같다). */
fun List<VerifiedLogo>.forSection(section: String): List<VerifiedLogo>? = filter { it.section == section }.takeIf { it.isNotEmpty() }
