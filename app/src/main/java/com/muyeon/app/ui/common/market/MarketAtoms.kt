package com.muyeon.app.ui.common.market

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.R
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.castingstage.components.StageBottomButton
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.ImageViewerActivity
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.ui.common.MuyeonList
import com.muyeon.app.ui.lesson.LessonPlace
import com.muyeon.app.ui.lesson.LocationPreviewCard
import com.muyeon.app.ui.lesson.openLessonNaverMap
import com.muyeon.app.ui.quote.QuoteUi
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.json.JSONArray


/** 하트(찜) — 켜지면 주황 채움. */
@Composable
fun MarketHeart(on: Boolean, size: Dp = 22.dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size + 14.dp).clip(CircleShape).clickable(onClick = onClick)
            .semantics { contentDescription = if (on) "찜 해제" else "찜하기" },
        Alignment.Center,
    ) {
        Icon(
            if (on) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null,
            tint = if (on) MuyeonColors.primary else MuyeonColors.hint, modifier = Modifier.size(size),
        )
    }
}

/** 작은 상태 배지(예약중·거래완료·결제 대기 등). */
@Composable
fun MarketBadge(text: String, fg: Color = Color.White, bg: Color = MuyeonColors.primary) {
    Text(
        text, style = ts(FontWeight.SemiBold, 12), color = fg,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * 글자가 바뀌는 상태 배지(판매중↔예약중↔거래완료, 결제 대기↔예약 확정 …) — 폭을 고정해
 *  상태가 바뀌어도 배지·옆 글자가 밀리지 않는다(글자는 가운데).
 */
@Composable
fun MarketStatusBadge(text: String, width: Dp, fg: Color = Color.White, bg: Color = MuyeonColors.primary) {
    Text(
        text, style = ts(FontWeight.SemiBold, 12), color = fg, textAlign = TextAlign.Center, maxLines = 1,
        modifier = Modifier.width(width).clip(RoundedCornerShape(6.dp)).background(bg).padding(vertical = 3.dp),
    )
}

/** 목록 위 필터 알약 — 값이 걸려 있으면 검정 테두리·굵게. */
@Composable
fun MarketFilterPill(text: String, active: Boolean, width: Dp = 112.dp, onClick: () -> Unit) {
    // 폭 고정 — "지역"→"서울", "정렬"→"가격 낮은순"처럼 값이 바뀌어도 옆 알약이 밀리지 않게.
    Row(
        Modifier.width(width).height(34.dp).clip(CircleShape)
            .border(1.dp, if (active) MuyeonColors.textHead else MuyeonList.ink200, CircleShape)
            .background(Color.White).clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
    ) {
        Text(
            text, style = ts(if (active) FontWeight.SemiBold else FontWeight.Medium, 14), color = MuyeonColors.textHead, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
        )
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = MuyeonColors.textSub, modifier = Modifier.size(16.dp))
    }
}


/** 섹션 제목(굵게 18). */
@Composable
fun MarketSectionTitle(text: String) {
    Text(text, style = ts(FontWeight.Bold, 18), color = MuyeonColors.textHead)
}
