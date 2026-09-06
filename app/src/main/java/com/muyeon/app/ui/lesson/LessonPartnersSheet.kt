package com.muyeon.app.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * 수강생(레슨 인연) 선택 시트 — iOS `LessonPartnersSheet.swift` 1:1.
 *  캘린더에서 바로 다음 약속을 잡을 때 상대를 고른다.
 *  아바타 48 + 이름 + 최근 레슨, 구분선은 76dp 들여쓴다(iOS 와 같은 값).
 *
 * ⚠️ 종전 AOS 는 이 시트가 없어 `LessonApi.partners()` 를 부르는 곳이 하나도 없었다.
 *   레슨 약속잡기는 채팅방에서 상대가 이미 정해진 경우에만 가능했다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPartnersSheet(
    api: LessonApi,
    onSelect: (LessonPartnerSummary) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var partners by remember { mutableStateOf<List<LessonPartnerSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        partners = api.partners().getOrDefault(emptyList())
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "수강생 선택",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    lineHeight = 20.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                )
                if (partners.isNotEmpty()) {
                    Text(
                        "${partners.size}",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        lineHeight = 19.sp, color = MuyeonColors.textSub,
                    )
                }
            }
            HorizontalDivider(color = MuyeonColors.border)

            when {
                loading -> Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    CircularProgressIndicator(color = MuyeonColors.primary)
                }
                partners.isEmpty() -> Column(
                    Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.People, null, tint = MuyeonColors.chevron,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        "아직 레슨을 진행한 수강생이 없어요",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        lineHeight = 17.sp, color = MuyeonColors.textHead,
                    )
                    Text(
                        "레슨을 진행하면 여기서 바로 다음 약속을 잡을 수 있어요.",
                        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp,
                        color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                    )
                }
                else -> LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    items(partners.size, key = { i -> partners[i].userId }) { i ->
                        val p = partners[i]
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { onSelect(p); onDismiss() }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            QuoteAvatar(p.image, p.displayName, 48.dp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    p.displayName,
                                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                                )
                                Text(
                                    partnerSubtitle(p),
                                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                                    color = MuyeonColors.textSub,
                                )
                            }
                            Text("›", fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.chevron)
                        }
                        // iOS Divider().padding(.leading, 76)
                        HorizontalDivider(color = MuyeonColors.border, modifier = Modifier.padding(start = 76.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private val PARTNER_ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}
private val PARTNER_MD = SimpleDateFormat("M월 d일", Locale.KOREA).apply {
    timeZone = TimeZone.getTimeZone("Asia/Seoul")
}

/** "발레 · 최근 레슨 7월 17일" — iOS subtitle(_:). 둘 다 없으면 "레슨 이력". */
private fun partnerSubtitle(p: LessonPartnerSummary): String {
    val parts = buildList {
        p.lastService?.takeIf { it.isNotEmpty() }?.let { add(it) }
        p.lastLessonAt?.let { iso ->
            runCatching { PARTNER_ISO.parse(iso.take(19)) }.getOrNull()
                ?.let { add("최근 레슨 ${PARTNER_MD.format(it)}") }
        }
    }
    return parts.ifEmpty { listOf("레슨 이력") }.joinToString(" · ")
}
