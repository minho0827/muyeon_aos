package com.muyeon.app.ui.quotefeed.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.ui.format.timeAgo
import com.muyeon.app.ui.quotefeed.QuoteFeedLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.domain.model.quotefeed.QuoteFeedItem

/**
 * '레슨을 찾는 회원들' 카드 — ★ 2026-11-09 장르 사진 대신 글자로 그린 '레슨 요청서'(사용자 시안 승인).
 *   150 × 200(예전 3:4 사진 칸 크기) · 위 주황 띠(제목 + 제안 보냄·내 요청) · 장르·비회원 칩 · 점선 ·
 *   지역/요일/시간/형태 4줄(없으면 '협의') · 아래 티켓 절취선 + 'N분 전 · 제안 기다리는 중'.
 *   카드 아래 글자 줄은 요청서와 겹쳐 뺐다. 요청자 이름·연락처는 서버가 주지 않는다(웹 공개 정책 그대로).
 */
@Composable
fun QuoteFeedCard(item: QuoteFeedItem, sent: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(3.dp, shape, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, SheetBorder, shape)
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.fillMaxWidth()) {
            SheetHeader(item, sent)
            Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val genre = QuoteFeedLabels.genre(item.categoryId) ?: stringResource(R.string.quote_feed_lesson)
                    // 장르 칩은 앱 장르 색(CategoryColors) — 레슨 카드와 같은 색.
                    val gc = com.muyeon.app.ui.theme.CategoryColors.genre(genre)
                    Chip(genre, gc.fg, gc.bg)
                    if (item.guestRequest) Chip(stringResource(R.string.quote_sheet_guest), AppColors.TextSub, AppColors.SurfaceMuted)
                }
                DashedLine()
                val any = stringResource(R.string.quote_sheet_any)
                SheetRow(stringResource(R.string.quote_sheet_region), item.region ?: any)
                SheetRow(stringResource(R.string.quote_sheet_days), QuoteFeedLabels.daysText(item) ?: any)
                SheetRow(stringResource(R.string.quote_sheet_time), QuoteFeedLabels.timesText(item) ?: any)
                SheetRow(stringResource(R.string.quote_sheet_format), QuoteFeedLabels.formatsText(item) ?: any)
            }
        }
        SheetFooter(item, Modifier.align(Alignment.BottomStart))
    }
}

private val SheetBorder = Color(0xFFECECEF)
private val SheetDash = Color(0xFFDCDCE0)
private val PrimaryInk = Color(0xFFC85A12)
private val BandBg = Color(0xFFFFF1E7)

@Composable
private fun SheetHeader(item: QuoteFeedItem, sent: Boolean) {
    Row(
        Modifier.fillMaxWidth().height(30.dp).background(BandBg).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.quote_sheet_title), style = TextStyle(fontFamily = com.muyeon.app.ui.theme.AppFonts.Pretendard, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp), color = PrimaryInk)
        Spacer(Modifier.weight(1f))
        // 상태 칩은 띠 오른쪽 — 아래 장르 칩과 겹치지 않게(요청 번호는 빼기로 함, 사용자 결정).
        val state = when {
            item.isMine -> stringResource(R.string.quote_feed_mine)
            sent -> stringResource(R.string.quote_feed_sent)
            else -> null
        }
        if (state != null) {
            Text(
                state, style = TextStyle(fontFamily = com.muyeon.app.ui.theme.AppFonts.Pretendard, fontSize = 9.5.sp, fontWeight = FontWeight.Bold), color = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(AppColors.TextHead).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun Chip(text: String, fg: Color, bg: Color) {
    Text(
        text, style = TextStyle(fontFamily = com.muyeon.app.ui.theme.AppFonts.Pretendard, fontSize = 10.sp, fontWeight = FontWeight.Bold), color = fg, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun SheetRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = TextStyle(fontFamily = com.muyeon.app.ui.theme.AppFonts.Pretendard, fontSize = 10.sp), color = AppColors.TextHint, modifier = Modifier.width(30.dp))
        Text(value, style = TextStyle(fontFamily = com.muyeon.app.ui.theme.AppFonts.Pretendard, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold), color = AppColors.TextHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DashedLine(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(1.dp)) {
        drawLine(SheetDash, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
    }
}

/** 티켓 절취선 — 위 점선 + 양옆 반원 홈 + 'N분 전 · 제안 기다리는 중'. */
@Composable
private fun SheetFooter(item: QuoteFeedItem, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(30.dp)) {
        DashedLine(Modifier.align(Alignment.TopStart))
        Box(Modifier.size(14.dp).offset(x = (-7).dp, y = (-7).dp).clip(CircleShape).background(AppColors.Surface).border(1.dp, SheetBorder, CircleShape))
        Box(Modifier.align(Alignment.TopEnd).size(14.dp).offset(x = 7.dp, y = (-7).dp).clip(CircleShape).background(AppColors.Surface).border(1.dp, SheetBorder, CircleShape))
        Row(Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.createdAt?.let { timeAgo(it) }.orEmpty(), style = TextStyle(fontFamily = com.muyeon.app.ui.theme.AppFonts.Pretendard, fontSize = 10.sp), color = AppColors.TextHint)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.quote_sheet_waiting), style = TextStyle(fontFamily = com.muyeon.app.ui.theme.AppFonts.Pretendard, fontSize = 10.sp, fontWeight = FontWeight.Bold), color = AppColors.Primary)
        }
    }
}
